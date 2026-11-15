package com.example.zaloclone.socket

import com.example.zaloclone.ui.model.PresenceStatus
import com.example.zaloclone.ui.model.UserPresence
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.time.Duration.Companion.seconds

data class SocketCallbacks(
    val onOnlineUsers: ((List<Int>) -> Unit)? = null,
    val onPresenceChanged: ((UserPresence) -> Unit)? = null
)

interface ISocketService {
    fun listenEvents(callbacks: SocketCallbacks)
    fun removeListeners()
    fun startHeartbeat(scope: CoroutineScope)
    fun stopHeartbeat()
    fun updatePresenceStatus(status: PresenceStatus)
}

class SocketService(
    private val socketClient: ISocketClient
) : ISocketService {
    private var socket: Socket? = null
    private var heartbeatJob: Job? = null

    override fun listenEvents(callbacks: SocketCallbacks) {
        socket = socketClient.getSocket()
        socket?.off("onlineUsers")
        socket?.off("presenceChanged")

        socket?.on("onlineUsers") { args ->
            val data = args.firstOrNull()
            var userIdList = emptyList<Int>()
            if (data is List<*>) {
                userIdList = data.mapNotNull { (it as? Number)?.toInt() }
            }
            if (data is JSONArray) {
                userIdList = (0 until data.length()).map { data.getInt(it) }
            }
            callbacks.onOnlineUsers?.invoke(userIdList)
        }

        socket?.on("presenceChanged") { args ->
            val data = args.firstOrNull()
            if (data is JSONObject) {
                val userId = data.optInt("userId", -1)
                val statusStr = data.optString("status", "OFFLINE").uppercase()
                val lastSeen = if (data.has("lastSeen") && !data.isNull("lastSeen")) data.getString("lastSeen") else null
                val status = try {
                    PresenceStatus.valueOf(statusStr)
                } catch (_: Exception) {
                    PresenceStatus.OFFLINE
                }
                if (userId != -1) {
                    callbacks.onPresenceChanged?.invoke(
                        UserPresence(userId = userId, status = status, lastSeen = lastSeen)
                    )
                }
            } else if (data is String) {
                try {
                    val jsonObj = JSONObject(data)
                    val userId = jsonObj.optInt("userId", -1)
                    val statusStr = jsonObj.optString("status", "OFFLINE").uppercase()
                    val lastSeen = if (jsonObj.has("lastSeen") && !jsonObj.isNull("lastSeen")) jsonObj.getString("lastSeen") else null
                    val status = try {
                        PresenceStatus.valueOf(statusStr)
                    } catch (_: Exception) {
                        PresenceStatus.OFFLINE
                    }
                    if (userId != -1) {
                        callbacks.onPresenceChanged?.invoke(
                            UserPresence(userId = userId, status = status, lastSeen = lastSeen)
                        )
                    }
                } catch (_: Exception) {
                    // Ignore parsing error
                }
            }
        }
    }

    override fun removeListeners() {
        stopHeartbeat()
        socket?.off("onlineUsers")
        socket?.off("presenceChanged")
        socket = null
    }

    override fun startHeartbeat(scope: CoroutineScope) {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(25.seconds) // Heartbeat every 25 seconds
                val currentSocket = socketClient.getSocket()
                if (currentSocket != null && currentSocket.connected()) {
                    currentSocket.emit("heartbeat")
                }
            }
        }
    }

    override fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    override fun updatePresenceStatus(status: PresenceStatus) {
        val currentSocket = socketClient.getSocket()
        if (currentSocket != null && currentSocket.connected()) {
            currentSocket.emit("update_status", status.name)
        }
    }
}

val socketService: ISocketService = SocketService(socketClient)
