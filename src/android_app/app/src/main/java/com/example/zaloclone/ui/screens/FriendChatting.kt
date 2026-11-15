package com.example.zaloclone.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Notifications
import com.example.zaloclone.ui.components.NotificationDialog
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zaloclone.ui.components.UserAvatar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.zaloclone.ui.viewModel.ChatViewModel
import com.example.zaloclone.api.model.ChatUser

@Composable
fun FriendsScreen(
    chatViewModel: ChatViewModel,
    onFriendSelect: (ChatUser) -> Unit = {},
    onAddFriend: (String) -> Unit = {},
    onNavigateToChats: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val chatState by chatViewModel.state.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var friendInput by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    val cyanHeaderColor = Color(0xFF00BCD4)
    val searchBarBg = Color(0xFF26C6DA)
    val navActiveColor = Color(0xFF00BCD4)

    // Fetch contacts from backend
    LaunchedEffect(Unit) {
        chatViewModel.getAllContacts()
        chatViewModel.getMyChatPartners()
    }

    // Filter contacts
    val filteredContacts = chatState.allContacts.filter { user ->
        user.username.contains(searchQuery, ignoreCase = true) ||
        user.email.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cyanHeaderColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Friends",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Notification & Add Friend button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            IconButton(
                                onClick = { showNotificationDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            val totalUnread = chatState.friendRequests.size + chatState.unreadNotifications.size
                            if (totalUnread > 0) {
                                Badge(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-4).dp, y = 4.dp),
                                    containerColor = Color.Red,
                                    contentColor = Color.White
                                ) {
                                    Text(text = totalUnread.toString(), fontSize = 10.sp)
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                friendInput = ""
                                showAddDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Friend",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        TextButton(onClick = { isEditing = !isEditing }) {
                            Text(
                                text = if (isEditing) "Done" else "Edit",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search friends",
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = searchBarBg,
                        unfocusedContainerColor = searchBarBg,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }
                )
            }
        },

        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToChats,
                    icon = {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Chats"
                        )
                    },
                    label = { Text("Chats") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Contacts"
                        )
                    },
                    label = { Text("Contacts") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = navActiveColor,
                        selectedTextColor = navActiveColor,
                        indicatorColor = Color.Transparent
                    )
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToSettings,
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color.Gray
                    )
                )
            }
        }
    ) { paddingValues ->

        when {
            chatState.isUsersLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            chatState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chatState.errorMessage
                            ?: "Unable to load contacts",
                        color = Color.Red
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (filteredContacts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No contacts found",
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    items(
                        items = filteredContacts,
                        key = { it.id }
                    ) { user ->
                        ContactItemRow(
                            user = user,
                            isEditing = isEditing,
                            onDelete = {
                                chatViewModel.deleteFriend(user.id)
                            },
                            onClick = {
                                onFriendSelect(user)
                            }
                        )

                        HorizontalDivider(
                            color = Color(0xFFF2F2F7),
                            thickness = 1.dp,
                            modifier = Modifier.padding(start = 76.dp)
                        )
                    }
                }
            }
        }
    }

    // Add Friend Dialog
    var searchedUser by remember { mutableStateOf<ChatUser?>(null) }
    var searchAttempted by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                searchedUser = null
                searchAttempted = false
                friendInput = ""
            },
            title = {
                Text("Add friend by Gmail")
            },
            text = {
                Column {
                    Text(
                        text = "Enter the Gmail:",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = friendInput,
                        onValueChange = { 
                            friendInput = it 
                            searchedUser = null
                            searchAttempted = false
                        },
                        label = {
                            Text("Gmail")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (searchAttempted) {
                        if (searchedUser != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(40.dp),
                                        shape = CircleShape,
                                        color = Color(0xFF00BCD4)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = searchedUser?.username?.firstOrNull()?.uppercase() ?: "?",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = searchedUser?.username ?: "",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = searchedUser?.email ?: "",
                                            fontSize = 13.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "User not found",
                                color = Color.Red,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (searchedUser == null) {
                    Button(
                        enabled = friendInput.isNotBlank(),
                        onClick = {
                            val emailTrimmed = friendInput.trim()
                            val found = chatState.allContacts.find { 
                                it.email.equals(emailTrimmed, ignoreCase = true) 
                            }
                            searchAttempted = true
                            if (found != null) {
                                searchedUser = found
                            } else {
                                searchedUser = null
                                Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Search")
                    }
                } else {
                    Button(
                        onClick = {
                            val isAlreadyFriend = chatState.chats.any { 
                                it.id == searchedUser?.id || it.email.equals(searchedUser?.email, ignoreCase = true) 
                            }
                            if (isAlreadyFriend) {
                                Toast.makeText(context, "Existing friend", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Sent friend request to ${searchedUser?.username}", Toast.LENGTH_SHORT).show()
                                onAddFriend(searchedUser?.email ?: "")
                                showAddDialog = false
                                searchedUser = null
                                searchAttempted = false
                                friendInput = ""
                            }
                        }
                    ) {
                        Text("Add Friend")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        searchedUser = null
                        searchAttempted = false
                        friendInput = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNotificationDialog) {
        NotificationDialog(
            chatState = chatState,
            onDismiss = { showNotificationDialog = false },
            onAcceptFriend = { req ->
                chatViewModel.acceptFriendRequest(req)
                Toast.makeText(context, "Accept friend request from ${req.name}. Successfully add friend!", Toast.LENGTH_SHORT).show()
            },
            onRejectFriend = { req ->
                chatViewModel.rejectFriendRequest(req)
                Toast.makeText(context, "Reject friend request from ${req.name}.", Toast.LENGTH_SHORT).show()
            },
            onNotificationMessageClick = { notif ->
                chatViewModel.clearNotificationsForUser(notif.senderId)
                showNotificationDialog = false
                val user = ChatUser(
                    id = notif.senderId,
                    username = notif.senderName,
                    email = "",
                    profilePic = null
                )
                onFriendSelect(user)
            }
        )
    }
}

@Composable
fun ContactItemRow(
    user: ChatUser,
    isEditing: Boolean = false,
    onDelete: () -> Unit = {},
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = Color(0xFFB2EBF2)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.username.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00838F)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = user.username,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = user.email,
                fontSize = 13.sp,
                color = Color.Gray
            )
        }

        if (isEditing) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Friend",
                    tint = Color.Red
                )
            }
        }
    }
}
