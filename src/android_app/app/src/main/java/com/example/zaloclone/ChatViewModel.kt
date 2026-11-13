package com.example.zaloclone

import android.app.Application
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zaloclone.ui.api.remote.ChatMessage
import com.example.zaloclone.ui.api.remote.ChatUser
import com.example.zaloclone.ui.api.remote.LoginRequest
import com.example.zaloclone.ui.api.remote.RegisterRequest
import com.example.zaloclone.ui.api.remote.RetrofitClient
import com.example.zaloclone.ui.api.remote.SendMessageRequest
import com.example.zaloclone.ui.api.remote.mergeMessages
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException

data class ChatState(
    val user: ChatUser? = null,
    val checkingSession: Boolean = true,
    val authenticating: Boolean = false,
    val contacts: List<ChatUser> = emptyList(),
    val chats: List<ChatUser> = emptyList(),
    val selectedUser: ChatUser? = null,
    val messages: List<ChatMessage> = emptyList(),
    val loadingUsers: Boolean = false,
    val loadingMessages: Boolean = false,
    val sending: Boolean = false,
    val connected: Boolean = false,
    val onlineUsers: Set<Long> = emptySet(),
    val error: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val client = RetrofitClient(application)
    private val api = client.api
    private val mutableState = MutableStateFlow(ChatState())
    val state = mutableState.asStateFlow()
    private var socket: Socket? = null
    private var authJob: Job? = null
    private var usersJob: Job? = null
    private var historyJob: Job? = null
    private var sendJob: Job? = null
    private var sessionVersion = 0

    init {
        restoreSession()
    }

    fun dismissError() {
        mutableState.update { it.copy(error = null) }
    }

    fun restoreSession() {
        if (authJob?.isActive == true) return
        authJob = viewModelScope.launch {
            mutableState.update { it.copy(checkingSession = true, error = null) }
            try {
                openSession(api.checkAuth())
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (error is HttpException && error.code() == 401) clearSession()
                else reportError(error)
            } finally {
                mutableState.update { it.copy(checkingSession = false) }
            }
        }
    }

    fun authenticate(email: String, password: String, username: String? = null) {
        if (authJob?.isActive == true) return
        if (email.isBlank() || password.isBlank() || username?.isBlank() == true) {
            mutableState.update { it.copy(error = "Please complete all fields.") }
            return
        }
        authJob = viewModelScope.launch {
            mutableState.update { it.copy(authenticating = true, error = null) }
            try {
                if (username != null) api.register(RegisterRequest(email.trim(), username.trim(), password))
                api.login(LoginRequest(email.trim(), password))
                openSession(api.checkAuth())
            } catch (error: Exception) {
                reportError(error)
            } finally {
                mutableState.update { it.copy(authenticating = false) }
            }
        }
    }

    private fun openSession(user: ChatUser) {
        require(user.id > 0) { "Invalid user response." }
        mutableState.update { it.copy(user = user, checkingSession = false, error = null) }
        startRealtime()
        refreshUsers()
    }

    fun refreshUsers() {
        if (state.value.user == null) return
        usersJob?.cancel()
        val version = sessionVersion
        usersJob = viewModelScope.launch {
            mutableState.update { it.copy(loadingUsers = true) }
            try {
                val contacts = api.contacts()
                val chats = api.chats()
                if (version == sessionVersion) {
                    mutableState.update { it.copy(contacts = contacts, chats = chats) }
                }
            } catch (error: Exception) {
                reportError(error)
            } finally {
                if (version == sessionVersion) mutableState.update { it.copy(loadingUsers = false) }
            }
        }
    }

    fun selectUser(user: ChatUser?) {
        historyJob?.cancel()
        mutableState.update { it.copy(selectedUser = user, messages = emptyList(), loadingMessages = false, error = null) }
        if (user != null) refreshHistory()
    }

    fun refreshHistory() {
        val partnerId = state.value.selectedUser?.id ?: return
        val userId = state.value.user?.id ?: return
        val version = sessionVersion
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            mutableState.update { it.copy(loadingMessages = true) }
            try {
                val history = api.messages(partnerId).filter { it.belongsTo(userId, partnerId) }
                if (version == sessionVersion && state.value.selectedUser?.id == partnerId) {
                    mutableState.update { it.copy(messages = mergeMessages(history, it.messages)) }
                }
            } catch (error: Exception) {
                reportError(error)
            } finally {
                if (version == sessionVersion && state.value.selectedUser?.id == partnerId) {
                    mutableState.update { it.copy(loadingMessages = false) }
                }
            }
        }
    }

    fun sendMessage(text: String, imageUri: Uri?, onSent: () -> Unit) {
        val partnerId = state.value.selectedUser?.id ?: return
        val userId = state.value.user?.id ?: return
        if (state.value.sending || (text.isBlank() && imageUri == null)) return
        if (text.length > 2000) {
            mutableState.update { it.copy(error = "Messages must be at most 2000 characters.") }
            return
        }
        val version = sessionVersion
        sendJob = viewModelScope.launch {
            mutableState.update { it.copy(sending = true, error = null) }
            try {
                val image = imageUri?.let { encodeImage(it) }
                val message = api.send(partnerId, SendMessageRequest(text.trim(), image))
                require(message.belongsTo(userId, partnerId)) { "Invalid message response." }
                if (version == sessionVersion && state.value.selectedUser?.id == partnerId) {
                    mutableState.update { it.copy(messages = mergeMessages(it.messages, listOf(message))) }
                    onSent()
                }
                refreshUsers()
            } catch (error: Exception) {
                reportError(error)
            } finally {
                if (version == sessionVersion) mutableState.update { it.copy(sending = false) }
            }
        }
    }

    private suspend fun encodeImage(uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = getApplication<Application>().contentResolver
        val mime = resolver.getType(uri) ?: throw IllegalArgumentException("Unknown image type.")
        require(mime.startsWith("image/")) { "Please select an image." }
        val bytes = resolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= 5_000_000) { "Images must be at most 5 MB." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: throw IllegalArgumentException("Unable to read image.")
        "data:$mime;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
    }

    fun startRealtime() {
        val userId = state.value.user?.id ?: return
        if (socket != null) return
        val version = sessionVersion
        val options = IO.Options.builder()
            .setPath("/socket.io")
            .setCallFactory(client.httpClient)
            .setWebSocketFactory(client.httpClient)
            .setReconnection(true)
            .setForceNew(true)
            .build()
        val connection = IO.socket(ApiConfig.BASE_URL, options)
        socket = connection
        connection.on(Socket.EVENT_CONNECT) {
            viewModelScope.launch {
                if (version != sessionVersion || socket !== connection) return@launch
                mutableState.update { it.copy(connected = true) }
                refreshUsers()
                refreshHistory()
            }
        }
        connection.on(Socket.EVENT_DISCONNECT) {
            viewModelScope.launch {
                if (socket === connection) mutableState.update { it.copy(connected = false, onlineUsers = emptySet()) }
            }
        }
        connection.on(Socket.EVENT_CONNECT_ERROR) {
            viewModelScope.launch {
                if (socket !== connection) return@launch
                mutableState.update { it.copy(connected = false, onlineUsers = emptySet()) }
                try {
                    api.checkAuth()
                } catch (error: Exception) {
                    if (version == sessionVersion && socket === connection) reportError(error)
                }
            }
        }
        connection.on("getOnlineUsers") { arguments ->
            val online = runCatching {
                client.gson.fromJson(arguments.firstOrNull().toString(), Array<Long>::class.java).toSet()
            }.getOrNull() ?: return@on
            viewModelScope.launch {
                if (version == sessionVersion && socket === connection) {
                    mutableState.update { it.copy(onlineUsers = online) }
                }
            }
        }
        connection.on("newMessage") { arguments ->
            val message = runCatching {
                client.gson.fromJson(arguments.firstOrNull().toString(), ChatMessage::class.java)
            }.getOrNull() ?: return@on
            viewModelScope.launch {
                if (version != sessionVersion || socket !== connection) return@launch
                val partnerId = state.value.selectedUser?.id
                if (partnerId != null && message.belongsTo(userId, partnerId)) {
                    mutableState.update { it.copy(messages = mergeMessages(it.messages, listOf(message))) }
                }
                if (message.receiverId == userId || message.senderId == userId) refreshUsers()
            }
        }
        connection.connect()
    }

    fun stopRealtime() {
        socket?.off()
        socket?.disconnect()
        socket = null
        mutableState.update { it.copy(connected = false, onlineUsers = emptySet()) }
    }

    fun logout() {
        authJob?.cancel()
        clearSession()
    }

    private fun clearSession() {
        sessionVersion++
        usersJob?.cancel()
        historyJob?.cancel()
        sendJob?.cancel()
        stopRealtime()
        client.cookieJar.clear()
        mutableState.value = ChatState(checkingSession = false)
    }

    private fun reportError(error: Exception) {
        if (error is CancellationException) throw error
        if (error is HttpException && error.code() == 401 && state.value.user != null) {
            clearSession()
            mutableState.update { it.copy(error = "Your session expired. Please sign in again.") }
            return
        }
        val message = if (error is HttpException) {
            runCatching {
                JSONObject(error.response()?.errorBody()?.string().orEmpty()).optString("message")
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Request failed (${error.code()})."
        } else error.message ?: "Unable to connect. Please try again."
        mutableState.update { it.copy(error = message) }
    }

    override fun onCleared() {
        stopRealtime()
        super.onCleared()
    }
}