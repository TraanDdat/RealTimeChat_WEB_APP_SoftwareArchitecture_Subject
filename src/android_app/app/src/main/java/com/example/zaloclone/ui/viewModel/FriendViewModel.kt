package com.example.zaloclone.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zaloclone.api.model.*
import com.example.zaloclone.api.repository.IFriendRepository
import com.example.zaloclone.socket.ISocketClient

import io.socket.emitter.Emitter
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.example.zaloclone.session.SessionManager

data class FriendState(
    val friendRequests: List<FriendRequestResponse> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class FriendViewModel(
    private val repository: IFriendRepository,
    private val socket: ISocketClient,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(FriendState())
    val state = _state.asStateFlow()

    fun _getCurrentEmail(): String? = sessionManager.currentEmail

    fun sendRequest(target_email: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val currentEmail = _getCurrentEmail()?: return@launch
                val data_request = AddFriendRequest(
                    current_email = currentEmail,
                    target_email = target_email
                )
                repository.sendRequest(data_request)
                getRequest()
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = e.localizedMessage?: "Unable to send friend request")
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun getRequest() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val currentEmail = _getCurrentEmail()?: return@launch
                val response = repository.getRequests()
                _state.value = _state.value.copy(
                    friendRequests = response.data,
                    errorMessage = null)
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
                repository.acceptRequest(friendship_id)
                _state.value = _state.value.copy(
                    friendRequests = _state.value.friendRequests.filter {it.id != friendship_id},
                    errorMessage = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = e.localizedMessage?: "Unable to accept friend request")
            }
        }
    }

    fun rejectFriendRequest(friendship_id: Int) {
        viewModelScope.launch {
            try {
                repository.rejectRequest(friendship_id)
                _state.value = _state.value.copy(
                    friendRequests = _state.value.friendRequests.filter {it.id != friendship_id},
                    errorMessage = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = e.localizedMessage?: "Unable to reject friend request")
            }
        }
    }

    fun deleteFriendship(otherUserId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteFriendshipRequest(otherUserId)
                _state.value = _state.value.copy(
                    errorMessage = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = e.localizedMessage ?: "Unable to delete friendship"
                )
            }
        }
    }
}