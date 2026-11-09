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
import com.example.zaloclone.ui.screens.ChatListScreen
import com.example.zaloclone.ui.screens.ChattingScreen
import com.example.zaloclone.ui.screens.FriendsScreen
import com.example.zaloclone.ui.screens.LoginScreen
import com.example.zaloclone.ui.screens.RegisterScreen
import com.example.zaloclone.ui.screens.SettingsScreen
import com.example.zaloclone.ui.screens.WelcomeScreen
import com.example.zaloclone.ui.theme.ZaloCyan

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
    var selectedChatId by remember { mutableStateOf("1") }
    var selectedChatName by remember { mutableStateOf("Anny Peter") }

    when (currentScreen) {
        "welcome" -> WelcomeScreen(
            onNavigateToLogin = { currentScreen = "login" },
            onNavigateToRegister = { currentScreen = "register" }
        )
        "login" -> LoginScreen(
            onLoginClick = {
                // Khi nhấn Đăng Nhập -> Chuyển sang ChatListScreen
                currentScreen = "chat_list"
            }
        )
        "register" -> RegisterScreen(
            onRegisterClick = {
                // Khi nhấn Đăng Ký -> Chuyển sang ChatListScreen
                currentScreen = "chat_list"
            }
        )
        "chat_list" -> ChatListScreen(
            onNavigateToFriends = { currentScreen = "friends" },
            onNavigateToSettings = { currentScreen = "settings" },
            onChatItemClick = { chatId, chatName ->
                selectedChatId = chatId
                selectedChatName = chatName
                currentScreen = "chatting"
            }
        )
        "friends" -> FriendsScreen(
            onNavigateToChats = { currentScreen = "chat_list" },
            onNavigateToSettings = { currentScreen = "settings" },
            onFriendSelect = { friend ->
                selectedChatId = friend.id
                selectedChatName = friend.name
                currentScreen = "chatting"
            }
        )
        "chatting" -> ChattingScreen(
            chatId = selectedChatId,
            chatName = selectedChatName,
            onNavigateBack = { currentScreen = "chat_list" } // Nút Back trở về danh sách
        )
        "settings" -> SettingsScreen(
            onNavigateToChats = { currentScreen = "chat_list" },
            onNavigateToFriends = { currentScreen = "friends" },
            onLogout = { currentScreen = "welcome" }
        )
    }
}
