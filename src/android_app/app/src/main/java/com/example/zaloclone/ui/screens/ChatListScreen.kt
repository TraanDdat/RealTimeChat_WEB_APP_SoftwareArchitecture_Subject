package com.example.zaloclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zaloclone.ui.components.UserAvatar

@Composable
fun ChatListScreen(
    onChatItemClick: (String, String) -> Unit = { _, _ -> },
    onNavigateToFriends: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onGroupCreated: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val chatList = remember { mutableStateListOf<ChatItem>() }
    var isEditing by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    // Tải dữ liệu từ JSON file khi khởi tạo màn hình
    LaunchedEffect(Unit) {
        syncChatListWithHistory(context)
        val savedList = loadChatListFromJson(context)
        // Chỉ lấy những chat item có lastMessage không bị rỗng
        val filteredList = savedList.filter { it.lastMessage.isNotBlank() }

        chatList.clear()
        chatList.addAll(filteredList)
    }

    val cyanHeaderColor = Color(0xFF38C7FF)
    val navActiveColor = Color(0xFF5B61F4)

    Scaffold(
        topBar = {
            // Thanh Tiêu đề Xanh Cyan
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(cyanHeaderColor)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chats",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showCreateGroupDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Create Group",
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
            }
        },
        bottomBar = {
            // Bottom Navigation Bar
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Chats") },
                    label = { Text("Chats") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = navActiveColor,
                        selectedTextColor = navActiveColor,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigateToFriends() },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Friends") },
                    label = { Text("Contacts") },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray)
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigateToSettings() },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(chatList) { chat ->
                ChatItemRow(
                    chat = chat,
                    isEditing = isEditing,
                    onClick = { onChatItemClick(chat.id, chat.name) },
                    onDelete = {
                        deleteChat(context, chat.id)
                        chatList.remove(chat)
                    }
                )
                HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp, modifier = Modifier.padding(start = 76.dp))
            }
        }
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            context = context,
            onDismiss = { showCreateGroupDialog = false },
            onGroupCreated = { chatId, chatName ->
                showCreateGroupDialog = false
                onGroupCreated(chatId, chatName)
                onChatItemClick(chatId, chatName)
            }
        )
    }
}

@Composable
fun ChatItemRow(
    chat: ChatItem,
    isEditing: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar tròn dạng UserAvatar
        UserAvatar(
            name = chat.name,
            avatarBgColor = chat.safeAvatarBgColor,
            avatarUri = chat.avatarUri,
            modifier = Modifier.size(52.dp),
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Nội dung tin nhắn
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = chat.name,
                fontWeight = if (chat.unreadCount > 0) FontWeight.ExtraBold else FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = chat.lastMessage,
                fontSize = 14.sp,
                fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                color = if (chat.unreadCount > 0) Color.Black else Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Thời gian & Nút xóa hoặc badge số tin chưa đọc
        if (isEditing) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Chat",
                    tint = Color.Red
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = chat.time,
                    fontSize = 12.sp,
                    color = if (chat.unreadCount > 0) Color(0xFF5B61F4) else Color.Gray,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (chat.unreadCount > 0) {
                    val displayCount = if (chat.unreadCount > 5) "5+" else chat.unreadCount.toString()
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                            .padding(horizontal = 4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF5B61F4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayCount,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
