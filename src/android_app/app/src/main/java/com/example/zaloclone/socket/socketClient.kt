package com.example.zaloclone.socket

import com.example.zaloclone.ApiConfig
import io.socket.client.IO
import io.socket.client.Socket

class SocketClient ( private val baseUrl: String ) {
    private var socket: Socket? = null

    fun create(token: String): Socket {
        if(socket == null) {
            val option = IO.Options.builder()
                .setPath("/socket.io")
                .setTransports(arrayOf("websocket","polling"))
                .setReconnection(true)
                .setExtraHeaders(mapOf("Authorization" to listOf("Bearer $token")))
                .build()
            socket = IO.socket(baseUrl, option)
        }
        return socket!!
    }

    fun getSocket(): Socket? { return socket}

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }
}

val socketClient = SocketClient(ApiConfig.BASE_URL)