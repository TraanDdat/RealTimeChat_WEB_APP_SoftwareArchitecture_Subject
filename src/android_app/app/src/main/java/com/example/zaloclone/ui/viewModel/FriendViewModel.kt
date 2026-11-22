package com.example.zaloclone.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zaloclone.api.model.*
import com.example.zaloclone.api.repository.IFriendRepository
import com.example.zaloclone.socket.ISocketClient

import io.socket.emitter.Emitter
import com.google.gson.Gson
import android.util.Log
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.example.zaloclone.session.SessionManager
import kotlinx.coroutines.flow.update

data class FriendState(
    val friendRequests: List<FriendRequestResponse> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showFriendRequestPopup: Boolean = false,
    val latestFriendRequest: FriendRequestEvent? = null,
    val friendActionMessage: String? = null
)

class FriendViewModel(
    private val repository: IFriendRepository,
    private val socketClient: ISocketClient,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(FriendState())
    private var friendRequestListener: Emitter.Listener? = null
    private var friendAcceptedListener: Emitter.Listener? = null
    private var friendRejectedListener: Emitter.Listener? = null 
    val state = _state.asStateFlow()
    fun startGlobalSocketListener() {
        val socket = socketClient.getSocket() ?: run {
            Log.e("FRIEND_SOCKET", "Socket is null in startGlobalSocketListener")
            return
        }
        Log.d("FRIEND_SOCKET", "Registering friendRequest listener on socket: ${socket.id()}")

        friendRequestListener?.let {
            socket.off("friendRequest", it)
        }
        friendAcceptedListener?.let {
            socket.off("acceptFriendRequest", it)
        }
        friendRejectedListener?.let {
            socket.off("rejectFriendRequest", it)
        }

        val requestListener = Emitter.Listener { args ->
            Log.d("FRIEND_SOCKET", "friendRequest RECEIVED")
            viewModelScope.launch {
                try {
                    if (args.isEmpty()) return@launch
                    val payload = args[0]
                    val jsonStr = when (payload) {
                        is String -> payload
                        is JSONObject -> payload.toString()
                        else -> Gson().toJson(payload)
                    }
                    Log.d("FRIEND_SOCKET", "Raw payload: $jsonStr")
                    val parsedRequest = Gson().fromJson(
                        jsonStr,
                        FriendRequestEvent::class.java
                    )
                    Log.d("FRIEND_SOCKET", "Username: ${parsedRequest.username}")
                    Log.d("FRIEND_SOCKET", "Email: ${parsedRequest.email}")
                    _state.update {
                        it.copy(
                            showFriendRequestPopup = true,
                            latestFriendRequest = parsedRequest,
                            errorMessage = null
                        )
                    }
                    try {
                        val response = repository.getRequests()
                        _state.update {
                            it.copy(friendRequests = response.data)
                        }
                    } catch (e: Exception) {
                        Log.e("FRIEND_SOCKET", "Refresh failed", e)
                    }
                } catch (e: Exception) {
                    Log.e("FRIEND_SOCKET", "Event handling failed", e)
                }
            }
        }

        val acceptedListener = Emitter.Listener { args ->
            Log.d("FRIEND_SOCKET", "acceptFriendRequest RECEIVED")
            viewModelScope.launch {
                try {
                    if (args.isEmpty()) return@launch
                    val payload = args[0]
                    val jsonStr = when (payload) {
                        is String -> payload
                        is JSONObject -> payload.toString()
                        else -> Gson().toJson(payload)
                    }
                    Log.d("FRIEND_SOCKET", "Accept payload: $jsonStr")
                    val event = Gson().fromJson(
                        jsonStr,
                        FriendActionEvent::class.java
                    )
                    _state.update {
                        it.copy(
                            friendActionMessage =
                                "${event.username ?: "Someone"} accepted your friend request.",
                            errorMessage = null
                        )
                    }
                    try {
                        val response = repository.getRequests()
                        _state.update {
                            it.copy(friendRequests = response.data)
                        }
                    } catch (e: Exception) {
                        Log.e("FRIEND_SOCKET", "Refresh failed", e)
                    }
                } catch (e: Exception) {
                    Log.e("FRIEND_SOCKET", "Accept event handling failed", e)
                }
            }
        }

        val rejectedListener = Emitter.Listener { args ->
            Log.d("FRIEND_SOCKET", "rejectFriendRequest RECEIVED")
            viewModelScope.launch {
                try {
                    if (args.isEmpty()) return@launch
                    val payload = args[0]
                    val jsonStr = when (payload) {
                        is String -> payload
                        is JSONObject -> payload.toString()
                        else -> Gson().toJson(payload)
                    }
                    Log.d("FRIEND_SOCKET", "Reject payload: $jsonStr")
                    val event = Gson().fromJson(
                        jsonStr,
                        FriendActionEvent::class.java
                    )
                    _state.update {
                        it.copy(
                            friendActionMessage =
                                "${event.username ?: "Someone"} rejected your friend request.",
                            errorMessage = null
                        )
                    }
                    try {
                        val response = repository.getRequests()
                        _state.update {
                            it.copy(friendRequests = response.data)
                        }
                    } catch (e: Exception) {
                        Log.e("FRIEND_SOCKET", "Refresh failed", e)
                    }
                } catch (e: Exception) {
                    Log.e("FRIEND_SOCKET", "Reject event handling failed", e)
                }
            }
        }

        friendRequestListener = requestListener
        friendAcceptedListener = acceptedListener
        friendRejectedListener = rejectedListener
        socket.on("friendRequest", requestListener)
        socket.on("acceptFriendRequest", acceptedListener)
        socket.on("rejectFriendRequest", rejectedListener)
        Log.d("FRIEND_SOCKET", "Successfully registered socket listeners for friend requests.")
    }

    fun sendRequest(targetEmail: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val currentEmail = getCurrentEmail()?: return@launch
                val dataRequest = AddFriendRequest(
                    current_email = currentEmail,
                    target_email = targetEmail
                )
                repository.sendRequest(dataRequest)
                getRequest()
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = e.localizedMessage?: "Unable to send friend request")
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun getCurrentEmail(): String? = sessionManager.currentEmail

    fun getRequest() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val response = repository.getRequests()
                _state.value = _state.value.copy(
                    friendRequests = response.data,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = e.localizedMessage
                        ?: "Unable to load friend requests"
                )
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun acceptFriendRequest(friendship_id: Int) {
        viewModelScope.launch {
            try {
                val req = _state.value.friendRequests.find { it.id == friendship_id }
                repository.acceptRequest(friendship_id)
                val name = req?.username ?: req?.email ?: "user"
                _state.value = _state.value.copy(
                    friendRequests = _state.value.friendRequests.filter { it.id != friendship_id },
                    friendActionMessage = "Friend request from $name has been accepted successfully!",
                    errorMessage = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = e.localizedMessage ?: "Unable to accept friend request")
            }
        }
    }

    fun rejectFriendRequest(friendship_id: Int) {
        viewModelScope.launch {
            try {
                val req = _state.value.friendRequests.find { it.id == friendship_id }
                repository.rejectRequest(friendship_id)
                val name = req?.username ?: req?.email ?: "user"
                _state.value = _state.value.copy(
                    friendRequests = _state.value.friendRequests.filter { it.id != friendship_id },
                    friendActionMessage = "Friend request from $name has been rejected.",
                    errorMessage = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = e.localizedMessage ?: "Unable to reject friend request")
            }
        }
    }

    fun dismissActionPopup() {
        _state.value = _state.value.copy(
            friendActionMessage = null
        )
    }

    fun deleteFriendship(
        otherUserId: Int,
        onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val response = repository.deleteFriendshipRequest(otherUserId)
                onResult(response.isSuccessful)
                _state.value = _state.value.copy(
                    errorMessage = null
                )
            } catch (e: Exception) {
                onResult(false)
                _state.value = _state.value.copy(
                    errorMessage = e.localizedMessage ?: "Unable to delete friendship"
                )
            }
        }
    }

    fun dismissFriendRequestPopup() {
        _state.value = _state.value.copy(showFriendRequestPopup = false)
    }

    fun clearFriendActionMessage() {
        _state.update {
            it.copy(friendActionMessage = null)
        }
    }

    fun clearSession() {
        _state.value = FriendState()
    }
}