package com.example.tmp

import android.content.SharedPreferences
import com.example.zaloclone.ui.api.remote.ApiServices
import com.example.zaloclone.ui.api.remote.ChatMessage
import com.example.zaloclone.ui.api.remote.LoginRequest
import com.example.zaloclone.ui.api.remote.RegisterRequest
import com.example.zaloclone.ui.api.remote.SendMessageRequest
import com.example.zaloclone.ui.api.remote.SessionCookieJar
import com.example.zaloclone.ui.api.remote.mergeMessages
import com.google.gson.GsonBuilder
import io.socket.client.IO
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class ExampleUnitTest {
    private val gson = GsonBuilder().serializeNulls().create()
    private val messageJson = """{"_id":"message-1","senderId":1,"receiverId":2,"text":"hello","image":null,"createdAt":"2026-11-13T10:00:00Z"}"""

    private fun preferences(): SharedPreferences {
        val stored = AtomicReference<String?>(null)
        val preferences = mock(SharedPreferences::class.java)
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(preferences.getString("jwt", null)).thenAnswer { stored.get() }
        `when`(preferences.edit()).thenReturn(editor)
        `when`(editor.putString(eq("jwt"), anyString())).thenAnswer {
            stored.set(it.getArgument(1))
            editor
        }
        `when`(editor.remove("jwt")).thenAnswer {
            stored.set(null)
            editor
        }
        return preferences
    }

    private fun api(server: MockWebServer, client: OkHttpClient = OkHttpClient()): ApiServices =
        Retrofit.Builder().baseUrl(server.url("/")).client(client)
            .addConverterFactory(GsonConverterFactory.create(gson)).build().create(ApiServices::class.java)

    @Test
    fun loginAndSessionUseWebContract() = runBlocking {
        MockWebServer().use { server ->
            val storage = preferences()
            val jar = SessionCookieJar(storage, server.url("/"))
            val service = api(server, OkHttpClient.Builder().cookieJar(jar).build())
            server.enqueue(MockResponse().addHeader("Set-Cookie", "jwt=test-session; Path=/; HttpOnly; Max-Age=3600")
                .setBody("""{"data":{"email":"alice@example.com","username":"Alice","role":"User","access_token":""},"error_code":null}"""))
            server.enqueue(MockResponse().setBody("""{"_id":1,"username":"Alice","email":"alice@example.com","profilePic":null}"""))
            service.login(LoginRequest("alice@example.com", "password"))
            val user = service.checkAuth()
            val login = server.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("/api/auth/login", login.path)
            assertEquals("POST", login.method)
            val body = JSONObject(login.body.readUtf8())
            assertEquals(setOf("email", "password", "role"), body.keys().asSequence().toSet())
            assertEquals("User", body.getString("role"))
            val check = server.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("/api/auth/check", check.path)
            assertEquals("jwt=test-session", check.getHeader("Cookie"))
            assertEquals(1L, user.id)
            assertTrue(SessionCookieJar(storage, server.url("/"))
                .loadForRequest(server.url("/api/messages/contacts")).isNotEmpty())
        }
    }

    @Test
    fun registerAndHistoryMatchWebEndpoints() = runBlocking {
        MockWebServer().use { server ->
            val service = api(server)
            server.enqueue(MockResponse().setBody("""{"data":{"email":"alice@example.com","username":"Alice","role":"User"},"error_code":null}"""))
            server.enqueue(MockResponse().setBody("[]"))
            server.enqueue(MockResponse().setBody("[]"))
            server.enqueue(MockResponse().setBody("[$messageJson]"))
            service.register(RegisterRequest("alice@example.com", "Alice", "password"))
            service.contacts()
            service.chats()
            val history = service.messages(2)
            val register = server.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("/api/auth/register", register.path)
            assertEquals(setOf("email", "username", "password", "role"), JSONObject(register.body.readUtf8()).keys().asSequence().toSet())
            assertEquals("/api/messages/contacts", server.takeRequest(2, TimeUnit.SECONDS)!!.path)
            assertEquals("/api/messages/chats", server.takeRequest(2, TimeUnit.SECONDS)!!.path)
            assertEquals("/api/messages/2", server.takeRequest(2, TimeUnit.SECONDS)!!.path)
            assertEquals("message-1", history.single().id)
        }
    }

    @Test
    fun sendDoesNotSubmitClientIdentityOrLocalMetadata() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(201).setBody(messageJson))
            val message = api(server).send(2, SendMessageRequest("hello"))
            val request = server.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("POST", request.method)
            assertEquals("/api/messages/send/2", request.path)
            val body = JSONObject(request.body.readUtf8())
            assertEquals(setOf("text", "image"), body.keys().asSequence().toSet())
            assertTrue(body.isNull("image"))
            assertEquals(1L, message.senderId)
        }
    }

    @Test
    fun expiredSecureAndForeignCookiesAreNotSent() {
        val origin = "http://10.0.2.2:8000/".toHttpUrl()
        val jar = SessionCookieJar(preferences(), origin)
        jar.saveFromResponse(origin, listOf(Cookie.parse(origin, "jwt=test; Path=/; Max-Age=3600")!!))
        assertTrue(jar.loadForRequest("http://example.com/".toHttpUrl()).isEmpty())
        assertEquals("test", jar.loadForRequest(origin).single().value)
        jar.saveFromResponse(origin, listOf(Cookie.parse(origin, "jwt=; Path=/; Max-Age=0")!!))
        assertTrue(jar.loadForRequest(origin).isEmpty())
        jar.saveFromResponse(origin, listOf(Cookie.parse(origin, "jwt=test; Path=/; Secure; Max-Age=3600")!!))
        assertTrue(jar.loadForRequest(origin).isEmpty())
        assertEquals("test", jar.loadForRequest("https://10.0.2.2:8000/".toHttpUrl()).single().value)
        jar.clear()
        assertTrue(jar.loadForRequest(origin).isEmpty())
    }

    @Test
    fun liveMessageSurvivesHistoryResponseAndIsDeduplicated() {
        val first = gson.fromJson(messageJson, ChatMessage::class.java)
        val reply = first.copy(id = "message-2", senderId = 2, receiverId = 1, createdAt = "2026-11-13T10:00:00.500Z")
        val merged = mergeMessages(listOf(first), listOf(reply, first))
        assertEquals(listOf("message-1", "message-2"), merged.map { it.id })
        assertTrue(first.belongsTo(1, 2))
        assertTrue(reply.belongsTo(1, 2))
        assertFalse(reply.belongsTo(1, 3))
    }

    @Test
    fun socketHandshakeUsesSameSessionCookieAndReceivesNewMessage() {
        MockWebServer().use { server ->
            val jar = SessionCookieJar(preferences(), server.url("/"))
            jar.saveFromResponse(server.url("/"), listOf(Cookie.parse(server.url("/"), "jwt=socket-session; Path=/; Max-Age=3600")!!))
            val http = OkHttpClient.Builder().cookieJar(jar).build()
            val received = AtomicReference<ChatMessage>()
            val latch = CountDownLatch(1)
            server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send("0{\"sid\":\"engine-test\",\"upgrades\":[],\"pingInterval\":25000,\"pingTimeout\":20000}")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (text == "40") {
                        webSocket.send("40{\"sid\":\"socket-test\"}")
                        webSocket.send("42[\"newMessage\",$messageJson]")
                    }
                }
            }))
            val socket = IO.socket(server.url("/").toString(), IO.Options.builder()
                .setPath("/socket.io").setTransports(arrayOf("websocket"))
                .setCallFactory(http).setWebSocketFactory(http).setReconnection(false).setForceNew(true).build())
            try {
                socket.on("newMessage") { arguments ->
                    received.set(gson.fromJson(arguments[0].toString(), ChatMessage::class.java))
                    latch.countDown()
                }
                socket.connect()
                assertTrue("Socket.IO did not receive newMessage", latch.await(10, TimeUnit.SECONDS))
                val request = server.takeRequest(2, TimeUnit.SECONDS)!!
                assertEquals("/socket.io/", request.requestUrl!!.encodedPath)
                assertEquals("4", request.requestUrl!!.queryParameter("EIO"))
                assertEquals("jwt=socket-session", request.getHeader("Cookie"))
                assertEquals("message-1", received.get().id)
            } finally {
                socket.off()
                socket.disconnect()
                http.dispatcher.executorService.shutdown()
                http.connectionPool.evictAll()
            }
        }
    }
}