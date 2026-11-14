package com.example.zaloclone.ui.screens

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
    val chatState by chatViewModel.state.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var friendInput by remember { mutableStateOf("") }

    val cyanHeaderColor = Color(0xFF00BCD4)
    val searchBarBg = Color(0xFF26C6DA)
    val navActiveColor = Color(0xFF00BCD4)

    // Fetch contacts from backend
    LaunchedEffect(Unit) {
        chatViewModel.getAllContacts()
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

                    // Add Friend button
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
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Tìm kiếm theo tên hoặc Gmail",
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
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
            },
            title = {
                Text("Thêm bạn bè")
            },
            text = {
                Column {
                    Text(
                        text = "Nhập địa chỉ Gmail của người dùng:",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = friendInput,
                        onValueChange = { friendInput = it },
                        label = {
                            Text("Gmail")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = friendInput.isNotBlank(),
                    onClick = {
                        onAddFriend(friendInput.trim())
                        showAddDialog = false
                        friendInput = ""
                    }
                ) {
                    Text("Thêm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                    }
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun ContactItemRow(
    user: ChatUser,
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
    }
}
