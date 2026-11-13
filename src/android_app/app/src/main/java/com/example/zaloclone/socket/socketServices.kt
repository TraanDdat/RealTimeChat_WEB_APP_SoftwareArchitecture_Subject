package com.example.zaloclone.socket

import io.socket.client.Socket
import org.json.JSONArray

data class SocketCallbacks (
    val onOnlineUsers: ((List<Int>) -> Unit)? = null
)

class SocketService (
    private val socketClient: SocketClient
) {
    private var socket: Socket? = null

    fun listenEvents (
        callbacks: SocketCallbacks
    ) {
        socket = socketClient.getSocket()
        socket?.off("onlineUsers") // off current user
        socket?.on("onlineUsers") { args -> // kotliln received ArrayOf, then only get first element is OK
            val data = args.firstOrNull()
            var userIdList = emptyList<Int>()
            if (data is List<*>) {
                userIdList  = data.mapNotNull { (it as? Number)?.toInt() }
            }
            if (data is JSONArray) {
                userIdList = (0 until data.length()).map { data.getInt(it) }
            }
            callbacks.onOnlineUsers?.invoke(userIdList)
        }
    }

    fun removeListeners () {
        socket?.off("onlineUsers")
        socket = null
    }
}

val socketService = SocketService(socketClient)