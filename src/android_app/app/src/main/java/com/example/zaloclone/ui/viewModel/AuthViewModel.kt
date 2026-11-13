package com.example.zaloclone.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zaloclone.api.datastore.TokenManager
import com.example.zaloclone.api.model.*
import com.example.zaloclone.api.repository.AuthRepository
import com.example.zaloclone.socket.SocketClient
import com.example.zaloclone.socket.SocketService
import com.example.zaloclone.socket.SocketCallbacks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val authUser: AuthUser? = null,
    val isCheckingAuth: Boolean = true,
    val isSigningUp: Boolean = false,
    val isLoggingIn: Boolean = false,
    val onlineUsers: List<Int> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository,
    private val tokenManager: TokenManager,
    private val socketClient: SocketClient,
    private val socketService: SocketService
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun AuthCheck() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCheckingAuth = true)
            try {
                val token = tokenManager.getToken()
                if (token.isNullOrBlank()) {
                    _state.value = _state.value.copy(
                        authUser = null,
                        isCheckingAuth = false
                    )
                    return@launch
                }
                val user = repository.AuthCheck()
                _state.value = _state.value.copy(
                    authUser = user,
                    isCheckingAuth = false
                )
                connectSocket(token)
            } catch (e: Exception) {
                tokenManager.clearToken()
                disconnectSocket()
                _state.value = _state.value.copy(
                    authUser = null,
                    isCheckingAuth = false,
                    errorMessage = e.localizedMessage
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
                _state.value = _state.value.copy(
                    authUser = user,
                    successMessage = "Logged in successfully"
                )
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
            } catch (e: Exception) {
            } finally {
                tokenManager.clearToken()
                disconnectSocket()
                _state.value = _state.value.copy(
                    authUser = null,
                    onlineUsers = emptyList(),
                    successMessage = "Logged out successfully"
                )
            }
        }
    }

    fun updateProfile(request: UpdateProfileRequest) {
        viewModelScope.launch {
            try {
                val user = repository.updateProfile(request)
                _state.value = _state.value.copy(
                    authUser = user,
                    successMessage = "Profile updated successfully"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = e.localizedMessage ?: "Unable to update profile"
                )
            }
        }
    }

    private fun connectSocket(token: String) {
        val socket = socketClient.create(token)
        socketService.listenEvents(
            SocketCallbacks(
                onOnlineUsers = { userIds ->
                    _state.value = _state.value.copy(
                        onlineUsers = userIds
                    )
                }
            )
        )

        if (!socket.connected()) {
            socket.connect()
        }
    }

    private fun disconnectSocket() {
        socketService.removeListeners()
        socketClient.disconnect()

        _state.value = _state.value.copy(
            onlineUsers = emptyList()
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