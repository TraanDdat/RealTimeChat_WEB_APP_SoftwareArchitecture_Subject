package com.example.zaloclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.zaloclone.api.model.*
import com.example.zaloclone.api.remote.handleRegister
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zaloclone.ui.viewModel.*
import com.example.zaloclone.di.AppContainer

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
    val friendState by friendViewModel.state.collectAsState()
    val groupViewModel: GroupViewModel = viewModel(
        factory = GroupViewModelFactory (
            repository = appContainer.groupRepository
        )
    )

    fun clearUserSession() {
        chatViewModel.clearSession()
        friendViewModel.clearSession()
        groupViewModel.clearSession()
    }

    LaunchedEffect(authState.isSocketReady) {
        if (authState.isSocketReady) {
            chatViewModel.startGlobalSocketListener()
            friendViewModel.startGlobalSocketListener()
            friendViewModel.getRequest()
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

    LaunchedEffect(friendState.friendActionMessage) {
        friendState.friendActionMessage?.let { message ->
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_SHORT
            ).show()

            friendViewModel.clearFriendActionMessage()
        }
    }

    LaunchedEffect(authState.forceLogoutMessage) {
        authState.forceLogoutMessage?.let { message ->
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(authState.authUser, authState.isCheckingAuth) {
        if (authState.isCheckingAuth) return@LaunchedEffect

        if (authState.authUser != null) {
            currentScreen = "chat_list"
        } else {
            clearUserSession()
            currentScreen = "welcome"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                }
            )
        }
        if (friendState.showFriendRequestPopup) {
            FriendRequestBanner(
                friendState = friendState,
                onAccept = { id ->
                    friendViewModel.acceptFriendRequest(id)
                    Toast.makeText(context, "Accepted friend request!", Toast.LENGTH_SHORT).show()
                    friendViewModel.dismissFriendRequestPopup()
                },
                onReject = { id ->
                    friendViewModel.rejectFriendRequest(id)
                    Toast.makeText(context, "Rejected friend request!", Toast.LENGTH_SHORT).show()
                    friendViewModel.dismissFriendRequestPopup()
                },
                onDismiss = {
                    friendViewModel.dismissFriendRequestPopup()
                }
            )
        }
    }
}

@Composable
fun FriendRequestBanner(
    friendState: FriendState,
    onAccept: (Int) -> Unit,
    onReject: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val req = friendState.latestFriendRequest
    val senderName = req?.username?.ifBlank { req?.email } ?: "Someone"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .zIndex(1f),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFE0F7FA),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 6.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Friend Request",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF00838F)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$senderName has sent a request to you",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            req?.id?.let { onReject(it) }
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Reject", fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            req?.id?.let { onAccept(it) }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Accept", fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }
    }
}