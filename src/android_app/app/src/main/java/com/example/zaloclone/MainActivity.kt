package com.example.zaloclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.zaloclone.ui.screens.ChatListScreen
import com.example.zaloclone.ui.screens.FriendsScreen
import com.example.zaloclone.ui.screens.LoginScreen
import com.example.zaloclone.ui.screens.RegisterScreen
import com.example.zaloclone.ui.screens.WelcomeScreen
import com.example.zaloclone.ui.theme.ZaloCyan
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.zaloclone.ui.data.remote.loginUser
import com.example.zaloclone.ui.data.remote.LoginRequest
import com.example.zaloclone.ui.data.remote.handleRegister
import com.example.zaloclone.ui.data.remote.RegisterConfirmRequest

private val LightColorScheme = lightColorScheme(
    primary = ZaloCyan,
    background = Color.White,
    surface = Color.White
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = LightColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppNavigation()
                }
            }
        }
    }
}

@Composable
fun MainAppNavigation() {
    // Mặc định xuất phát từ màn hình "welcome"
    var currentScreen by remember { mutableStateOf("welcome") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    when (currentScreen) {
        "welcome" -> WelcomeScreen(
            onNavigateToLogin = { currentScreen = "login" },
            onNavigateToRegister = { currentScreen = "register" }
        )

        "login" -> LoginScreen(
            onLoginClick = { email, password, role ->
                scope.launch {
                    val result = loginUser(
                        LoginRequest(
                            email = email,
                            password = password,
                            role = role
                        )
                    )
                    result.onSuccess { response ->
                        val token = response.access_token
                        val email = response.email
                        currentScreen = "chat_list"
                        }.onFailure { error -> println("Login failed") 
                    }
                }
            }
        )

        "register" -> RegisterScreen(
            onRegisterClick = { email, password, confirmPassword, role ->
                scope.launch {
                    val result = handleRegister(
                        RegisterConfirmRequest(
                            email = email,
                            password = password,
                            confirmPassword = confirmPassword,
                            role = role
                        )
                    )
                    result.onSuccess { response ->
                        println("Register success")
                        currentScreen = "login"
                        Toast.makeText(context, "Register successful", Toast.LENGTH_SHORT).show()
                    }.onFailure { error -> 
                                    println("Register failed")
                                    Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        "chat_list" -> ChatListScreen(
            onNavigateToFriends = { currentScreen = "friends" },
            onChatItemClick = { chatId ->
                // Mở khung chat chi tiết với bạn bè
            }
        )

        "friends" -> FriendsScreen(
            onNavigateToChats = { currentScreen = "chat_list" },
            onFriendSelect = { friend ->
                // Chọn bạn bè xong -> Mở khung chat
            }
        )
    }
}