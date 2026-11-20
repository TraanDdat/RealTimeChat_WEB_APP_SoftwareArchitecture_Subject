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
import kotlinx.coroutines.launch
import com.example.zaloclone.ui.screens.ChatListScreen
import com.example.zaloclone.ui.screens.ChattingScreen
import com.example.zaloclone.ui.screens.ForgotPasswordScreen
import com.example.zaloclone.ui.screens.FriendsScreen
import com.example.zaloclone.ui.screens.LoginScreen
import com.example.zaloclone.ui.screens.RegisterScreen
import com.example.zaloclone.ui.screens.SettingsScreen
import com.example.zaloclone.ui.screens.WelcomeScreen
import com.example.zaloclone.ui.theme.ZaloCyan
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.zaloclone.api.model.LoginRequest
import com.example.zaloclone.api.remote.handleRegister
import com.example.zaloclone.api.model.RegisterConfirmRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zaloclone.ui.viewModel.*
import com.example.zaloclone.di.AppContainer
import com.example.zaloclone.api.model.LoginResponse
import com.example.zaloclone.api.model.LoginData
import com.example.zaloclone.socket.socketClient

private val LightColorScheme = lightColorScheme(
    primary = ZaloCyan,
    background = Color.White,
    surface = Color.White
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as ZaloApplication).container
        setContent {
            MaterialTheme(colorScheme = LightColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppNavigation(appContainer)
                }
            }
        }
    }
}

@Composable
fun MainAppNavigation(appContainer: AppContainer) {
    var currentScreen by remember { mutableStateOf("welcome") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedChatId by remember { mutableStateOf("1") }
    var selectedChatName by remember { mutableStateOf("Anny Peter") }
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(
            repository = appContainer.authRepository,
            tokenManager = appContainer.tokenManager,
            socketClient = appContainer.socketClientInstance,
            socketService = appContainer.socketServiceInstance
        )
    )
    val authState by authViewModel.state.collectAsState()
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory (
            repository = appContainer.chatRepository,
            socketClient = appContainer.socketClientInstance,
            sessionManager = appContainer.sessionManager
        )
    )

    val friendViewModel: FriendViewModel = viewModel(
        factory = FriendViewModelFactory (
            repository = appContainer.friendRepository,
            socketClient = appContainer.socketClientInstance,
            sessionManager = appContainer.sessionManager
        )
    )

    val groupViewModel: GroupViewModel = viewModel(
        factory = GroupViewModelFactory (
            repository = appContainer.groupRepository
        )
    )

    LaunchedEffect(authState.isSocketReady) {
        if (authState.isSocketReady) {
            chatViewModel.startGlobalSocketListener()
        }
    }

    LaunchedEffect(authState.userId, authState.email) {
        val userId = authState.userId
        val email = authState.email
        if (userId != null && email != null) {
            appContainer.sessionManager.setCurrentUser(userId, email)
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.AuthCheck()
    }

    LaunchedEffect(authState.authUser) {
        if (authState.authUser != null) {
            currentScreen = "chat_list"
        }
    }

    LaunchedEffect(authState.errorMessage) {
        authState.errorMessage?.let { message ->
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_SHORT
            ).show()

            authViewModel.clearMessages()
        }
    }

    when (currentScreen) {    
        "welcome" -> WelcomeScreen(
            onNavigateToLogin = { currentScreen = "login" },
            onNavigateToRegister = { currentScreen = "signup" },
            onNavigateToForgotPassword = { currentScreen = "forgot_password" }
        )

        "login" -> LoginScreen(
            onLoginClick = { email, password ->
                authViewModel.login(
                    LoginRequest( email = email, password = password )
                )
            },
            onNavigateToForgotPassword = { currentScreen = "forgot_password" },
            onNavigateToWelcome = { currentScreen = "welcome" }
        )
        "forgot_password" -> ForgotPasswordScreen(
            onNavigateToWelcome = { currentScreen = "welcome" },
            onPasswordResetSuccess = { currentScreen = "login" }
        )
        "signup" -> RegisterScreen(
            onRegisterClick = { email, username, password, confirmPassword ->
                scope.launch {
                    val result = handleRegister(
                        RegisterConfirmRequest(
                            email = email,
                            username = username,
                            password = password,
                            confirmPassword = confirmPassword
                        )
                    )
                    result.onSuccess { currentScreen = "login" }
                }
            },
            onNavigateToWelcome = { currentScreen = "welcome" }
        )
        "chat_list" -> ChatListScreen(
            chatViewModel = chatViewModel,
            friendViewModel = friendViewModel,
            groupViewModel = groupViewModel,
            authViewModel = authViewModel,
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
            chatViewModel = chatViewModel,
            friendViewModel = friendViewModel,
            groupViewModel = groupViewModel,
            onGroupSelect = { group ->
                selectedChatId = "group_${group.id}"
                selectedChatName = group.name
                currentScreen = "chatting"
            },
            onFriendSelect = { friend ->
                selectedChatId = friend.id.toString()
                selectedChatName = friend.username
                chatViewModel.setSelectedUser(friend)
                currentScreen = "chatting"
        })
        "chatting" -> ChattingScreen(
            chatId = selectedChatId,
            chatName = selectedChatName,
            chatViewModel = chatViewModel,
            groupViewModel = groupViewModel,
            onNavigateBack = { 
                chatViewModel.clearSelectedChat()
                currentScreen = "chat_list" }
        )
        "settings" -> SettingsScreen(
            authViewModel = authViewModel,
            onNavigateToChats = { currentScreen = "chat_list" },
            onNavigateToFriends = { currentScreen = "friends" },
            onLogout = { 
                authViewModel.logout()
                currentScreen = "welcome" }
        )
    }
}