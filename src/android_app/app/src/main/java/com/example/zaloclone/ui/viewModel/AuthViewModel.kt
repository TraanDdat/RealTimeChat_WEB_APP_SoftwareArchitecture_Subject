package com.example.zaloclone.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zaloclone.api.datastore.ITokenManager
import com.example.zaloclone.api.model.*
import com.example.zaloclone.api.repository.IAuthRepository
import com.example.zaloclone.socket.ISocketClient
import com.example.zaloclone.socket.ISocketService
import com.example.zaloclone.socket.SocketCallbacks
import com.example.zaloclone.ui.model.PresenceStatus
import com.example.zaloclone.ui.model.UserPresence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.socket.client.Socket
import kotlinx.coroutines.flow.update

data class AuthState(
    val authUser: AuthUser? = null,
    val userId: Int? = null,
    val email: String? = null,
    val isCheckingAuth: Boolean = true,
    val isSigningUp: Boolean = false,
    val isLoggingIn: Boolean = false,
    val getOnlineUsers: List<Int> = emptyList(),
    val userPresences: Map<Int, UserPresence> = emptyMap(),
    val myPresenceStatus: PresenceStatus = PresenceStatus.OFFLINE,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSocketReady: Boolean = false,
    val isLoading: Boolean = false
)

class AuthViewModel(
    private val repository: IAuthRepository,
    private val tokenManager: ITokenManager,
    private val socketClient: ISocketClient,
    private val socketService: ISocketService
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun AuthCheck() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCheckingAuth = true)
            try {
                val token = tokenManager.getToken()
                if (token.isNullOrBlank()) {
                    disconnectSocket()
                    _state.value = AuthState(
                        isCheckingAuth = false
                    )
                    return@launch
                }
                val user = repository.AuthCheck()
                val authUser = user.copy(
                    access_token = token
                )
                _state.value = _state.value.copy(
                    authUser = authUser,
                    userId = user.id,
                    email = user.email,
                    isCheckingAuth = false
                )
                connectSocket(token)
            } catch (e: Exception) {
                tokenManager.clearToken()
                disconnectSocket()
                _state.value = _state.value.copy(
                    authUser = null,
                    userId = null,
                    email = null,
                    myPresenceStatus = PresenceStatus.OFFLINE,
                    isCheckingAuth = false,
                    errorMessage = e.localizedMessage ?: "Error when login"
                )
            }
        }
    }

    fun signup(request: RegisterRequest) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isSigningUp = true,
                errorMessage = null,
                successMessage = null
            )
            try {
                repository.signup(request)
                _state.value = _state.value.copy(
                    successMessage = "Account created successfully!"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = e.localizedMessage ?: "Unable to create account"
                )
            } finally {
                _state.value = _state.value.copy(isSigningUp = false)
            }
        }
    }

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoggingIn = true,
                errorMessage = null,
                successMessage = null
            )
            try {
                val response = repository.login(request)
                val token = response.data.access_token
                    ?: throw Exception("Access token is missing")
                if (token.isBlank()) {
                    throw Exception("Access token is empty")
                }
                tokenManager.saveToken(token)
                val user = repository.AuthCheck()
                val authUser = user.copy(
                    access_token = token
                )
                _state.value = _state.value.copy(
                    authUser = authUser,
                    userId = authUser.id,
                    email = authUser.email,
                    myPresenceStatus = PresenceStatus.ONLINE,
                    successMessage = "Logged in successfully"
                )
                Log.d("token","${response}")
                Log.d("authUser","${_state.value.authUser}")
                connectSocket(token)
            } catch (e: Exception) {
                tokenManager.clearToken()
                disconnectSocket()
                _state.value = _state.value.copy(
                    authUser = null,
                    errorMessage = e.localizedMessage ?: "Unable to log in"
                )
            } finally {
                _state.value = _state.value.copy(
                    isLoggingIn = false
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                repository.logout()
            } catch (_: Exception) {
            } finally {
                tokenManager.clearToken()
                disconnectSocket()
                _state.value = AuthState(
                    isCheckingAuth = false,
                    isLoggingIn = false,
                    isSigningUp = false,
                    myPresenceStatus = PresenceStatus.OFFLINE,
                    successMessage = "Logged out successfully"
                )
            }
        }
    }

    fun updateProfile(request: UpdateProfileRequest) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            try {
                val user = repository.updateProfile(request)
                val currentUser = _state.value.authUser
                _state.value = _state.value.copy(
                    authUser = currentUser?.copy(
                        username = user.username ?: currentUser.username,
                        profilePic = user.profilePic ?: currentUser.profilePic,
                        access_token = currentUser.access_token
                    ),
                    isLoading = false,
                    successMessage = "Profile updated successfully"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Unable to update profile"
                )
            }
        }
    }

    fun updateMyPresenceStatus(status: PresenceStatus) {
        _state.update {it.copy(myPresenceStatus = status)}
        socketService.updatePresenceStatus(status)
    }
    
    fun connectSocket(token: String) {
        _state.update { it.copy(isSocketReady = false) }
        val socket = socketClient.create(token)
        socketService.listenEvents(
            SocketCallbacks(
                onOnlineUsers = { userIds ->
                    _state.update { current ->
                        val updatedPresences = current.userPresences.toMutableMap()
                        updatedPresences.keys.toList().forEach { id ->
                            val old = updatedPresences[id] ?: return@forEach
                            updatedPresences[id] = old.copy(
                                status = if (id in userIds) {
                                    PresenceStatus.ONLINE
                                } else {
                                    PresenceStatus.OFFLINE
                                }
                            )
                        }
                        userIds.forEach { id ->
                            val old = updatedPresences[id]
                            updatedPresences[id] = old?.copy(
                                status = PresenceStatus.ONLINE
                            ) ?: UserPresence(
                                userId = id,
                                status = PresenceStatus.ONLINE,
                                lastSeen = null
                            )
                        }
                        current.copy(
                            getOnlineUsers = userIds,
                            userPresences = updatedPresences
                        )
                    }
                },
                onPresenceChanged = { presence ->
                    _state.update { current ->
                        current.copy(
                            userPresences = current.userPresences +
                                (presence.userId to presence),
                            myPresenceStatus = if (presence.userId == current.userId) {
                                presence.status
                            } else {
                                current.myPresenceStatus
                            }
                        )
                    }
                }
            )
        )
        _state.value = _state.value.copy(isSocketReady = true)
        if (!socket.connected()) {
            socket.connect()
        }
        socketService.startHeartbeat(viewModelScope)
    }

    private fun disconnectSocket() {
        socketService.stopHeartbeat()
        socketService.removeListeners()
        socketClient.disconnect()

        _state.value = _state.value.copy(
            isSocketReady = false,
            getOnlineUsers = emptyList(),
            userPresences = emptyMap()
        )
    }

    fun clearMessages() {
        _state.value = _state.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    override fun onCleared() {
        disconnectSocket()
        super.onCleared()
    }
}
