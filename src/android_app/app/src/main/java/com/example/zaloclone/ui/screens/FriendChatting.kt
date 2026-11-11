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

@Composable
fun FriendsScreen(
    onFriendSelect: (FriendItem) -> Unit = {},
    onNavigateToChats: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onGroupCreated: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val friendList = remember { mutableStateListOf<FriendItem>() }
    val requestList = remember { mutableStateListOf<FriendRequestItem>() }
    var isEditing by remember { mutableStateOf(false) }

    // Dialog states
    var showAddDialog by remember { mutableStateOf(false) }
    var showRequestsDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var phoneInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }

    // Load friends and requests from JSON database on start
    LaunchedEffect(Unit) {
        syncChatListWithFriends(context)
        val loaded = loadFriendListFromJson(context)
        friendList.clear()
        friendList.addAll(loaded)

        val loadedRequests = loadFriendRequestsFromJson(context)
        requestList.clear()
        requestList.addAll(loadedRequests)
    }

    val cyanHeaderColor = Color(0xFF38C7FF)
    val searchBarBg = Color(0xFF2CBCEE)
    val navActiveColor = Color(0xFF5B61F4)

    val filteredFriends = if (searchQuery.isBlank()) {
        friendList
    } else {
        friendList.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
        }
    }

    Scaffold(
        topBar = {
            // Header Xanh Cyan chứa Title + Nút Edit + Nút thêm + Thanh Search
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
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Friend",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Thanh Tìm kiếm (Search Box)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or phone", color = Color.White.copy(alpha = 0.8f)) },
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
                        .height(50.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                )
            }
        },
        bottomBar = {
            // Bottom Navigation Bar
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigateToChats() },
                    icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Chats") },
                    label = { Text("Chats") },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray)
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Friends") },
                    label = { Text("Contacts") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = navActiveColor,
                        selectedTextColor = navActiveColor,
                        indicatorColor = Color.Transparent
                    )
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
            if (requestList.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRequestsDialog = true },
                        color = Color(0xFFFFF3CD)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF856404)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Có ${requestList.size} lời mời kết bạn mới",
                                    color = Color(0xFF856404),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Text(
                                text = "Xem >",
                                color = Color(0xFF856404),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                }
            }

            items(filteredFriends) { friend ->
                FriendItemRow(
                    friend = friend,
                    isEditing = isEditing,
                    onClick = { onFriendSelect(friend) },
                    onDelete = {
                        deleteFriend(context, friend.id)
                        friendList.remove(friend)
                    }
                )
                HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp, modifier = Modifier.padding(start = 76.dp))
            }
        }
    }

    // Dialog thêm bạn bằng số điện thoại
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Thêm bạn mới") },
            text = {
                Column {
                    Text("Nhập số điện thoại để thêm bạn:", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Số điện thoại") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Tên gợi nhớ (Tùy chọn)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (phoneInput.isNotBlank()) {
                            val newId = System.currentTimeMillis().toString()
                            val newName = if (nameInput.isNotBlank()) nameInput else "Bạn ($phoneInput)"
                            val colors = listOf(Color(0xFFE2C4B1), Color(0xFFD1E3FF), Color(0xFFE0C1C6), Color(0xFFD7BDE2), Color(0xFFBDC3C7))
                            val randomColor = colors[friendList.size % colors.size]

                            addFriendAndChat(context, newId, newName, phoneInput, randomColor)
                            friendList.clear()
                            friendList.addAll(loadFriendListFromJson(context))

                            phoneInput = ""
                            nameInput = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Thêm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog danh sách lời mời kết bạn
    if (showRequestsDialog) {
        AlertDialog(
            onDismissRequest = { showRequestsDialog = false },
            title = { Text("Lời mời kết bạn (${requestList.size})") },
            text = {
                if (requestList.isEmpty()) {
                    Text("Không có lời mời kết bạn nào.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        items(requestList) { req ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    name = req.name,
                                    avatarBgColor = req.avatarBgColor,
                                    avatarUri = req.avatarUri,
                                    modifier = Modifier.size(40.dp),
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = req.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = req.message, fontSize = 12.sp, color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    TextButton(
                                        onClick = {
                                            // Accept
                                            addFriendAndChat(context, req.id, req.name, req.phone, req.avatarBgColor)
                                            friendList.clear()
                                            friendList.addAll(loadFriendListFromJson(context))

                                            requestList.remove(req)
                                            saveFriendRequestsFromJson(context, requestList)
                                            if (requestList.isEmpty()) {
                                                showRequestsDialog = false
                                            }
                                        },
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Chấp nhận", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    TextButton(
                                        onClick = {
                                            // Reject
                                            requestList.remove(req)
                                            saveFriendRequestsFromJson(context, requestList)
                                            if (requestList.isEmpty()) {
                                                showRequestsDialog = false
                                            }
                                        },
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Từ chối", color = Color.Red, fontSize = 13.sp)
                                    }
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRequestsDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    // Dialog Tạo nhóm chat
    if (showCreateGroupDialog) {
        CreateGroupDialog(
            context = context,
            onDismiss = { showCreateGroupDialog = false },
            onGroupCreated = { chatId, chatName ->
                showCreateGroupDialog = false
                onGroupCreated(chatId, chatName)
            }
        )
    }
}

@Composable
fun FriendItemRow(
    friend: FriendItem,
    isEditing: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val displayName = if (friend.nickname.isNotBlank()) friend.nickname else friend.name
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar tròn dạng UserAvatar
        UserAvatar(
            name = displayName,
            avatarBgColor = friend.safeAvatarBgColor,
            avatarUri = friend.avatarUri,
            modifier = Modifier.size(48.dp),
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Tên, số điện thoại & Trạng thái Online / Offline
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = displayName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (friend.nickname.isNotBlank()) {
                Text(
                    text = "Tên gốc: ${friend.name}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            if (friend.phone.isNotBlank()) {
                Text(
                    text = "SĐT: ${friend.phone}",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = friend.status,
                fontSize = 13.sp,
                color = if (friend.isOnline) Color(0xFF34C759) else Color.Gray
            )
        }

        // Nút xóa khi ở chế độ Edit
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
