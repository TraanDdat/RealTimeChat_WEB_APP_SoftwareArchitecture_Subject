package com.example.zaloclone.socket

import com.example.zaloclone.ApiConfig
import io.socket.client.IO
import io.socket.client.Socket

interface ISocketClient {
    fun create(token: String): Socket
    fun getSocket(): Socket?
    fun disconnect()
}

class SocketClient ( private val baseUrl: String ) : ISocketClient {
    private var socket: Socket? = null

    override fun create(token: String): Socket {
        disconnect()
        val option = IO.Options.builder()
            .setPath("/socket.io")
            .setTransports(arrayOf("websocket","polling"))
            .setReconnection(true)
            .setExtraHeaders(mapOf("Authorization" to listOf("Bearer $token")))
            .build()
        socket = IO.socket(baseUrl, option)
        return socket!!
    }

    override fun getSocket(): Socket? = socket

    override fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }
}

val socketClient: ISocketClient = SocketClient(ApiConfig.BASE_URL)