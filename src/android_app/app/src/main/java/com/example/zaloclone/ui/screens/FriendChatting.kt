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
import androidx.compose.material.icons.filled.Call
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Data model bạn bè
data class FriendItem(
    val id: String,
    val name: String,
    val status: String,
    val isOnline: Boolean = false,
    val avatarBgColor: Color = Color(0xFFD1E3FF)
)

@Composable
fun FriendsScreen(
    onFriendSelect: (FriendItem) -> Unit = {},
    onNavigateToChats: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    val sampleFriends = listOf(
        FriendItem("1", "Anny Peter", "Online", isOnline = true, Color(0xFFE2C4B1)),
        FriendItem("2", "Arnold Peter", "Last seen at 6:20 PM", isOnline = false, Color(0xFFE0C1C6)),
        FriendItem("3", "Dann Jazmyn", "Last seen at 6:20 PM", isOnline = false, Color(0xFFE5E5EA)),
        FriendItem("4", "Duane Brodie", "Online", isOnline = true, Color(0xFFD7BDE2)),
        FriendItem("5", "Damion Gage", "Last seen at 6:20 PM", isOnline = false, Color(0xFFBDC3C7)),
        FriendItem("6", "Demarcus Jazmyn", "Online", isOnline = true, Color(0xFFD0D3D4)),
        FriendItem("7", "Emmalee Alivia", "Last seen at 6:20 PM", isOnline = false, Color(0xFFE5E5EA)),
        FriendItem("8", "Esha Chikere", "Last seen at 6:20 PM", isOnline = false, Color(0xFFE0C1C6)),
        FriendItem("9", "Mariam Rowan", "Last seen at 6:20 PM", isOnline = false, Color(0xFFD1E3FF)),
        FriendItem("10", "Piety Candi", "Online", isOnline = true, Color(0xFFBDC3C7))
    )

    val cyanHeaderColor = Color(0xFF38C7FF)
    val searchBarBg = Color(0xFF2CBCEE)
    val navActiveColor = Color(0xFF5B61F4)

    Scaffold(
        topBar = {
            // Header Xanh Cyan chứa Title + Nút thêm + Thanh Search
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
                    IconButton(onClick = { /* Thêm bạn mới */ }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Friend",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Thanh Tìm kiếm (Search Box)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(searchBarBg)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "Search" else searchQuery,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 16.sp
                        )
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
                    selected = false,
                    onClick = { onNavigateToChats() },
                    icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Chats") },
                    label = { Text("Chats") },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray)
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Default.Call, contentDescription = "Calls") },
                    label = { Text("Calls") },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray)
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Friends") },
                    label = { Text("Friends") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = navActiveColor,
                        selectedTextColor = navActiveColor,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
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
            items(sampleFriends) { friend ->
                FriendItemRow(friend = friend, onClick = { onFriendSelect(friend) })
                HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp, modifier = Modifier.padding(start = 76.dp))
            }
        }
    }
}

@Composable
fun FriendItemRow(
    friend: FriendItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar tròn
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(friend.avatarBgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = friend.name.take(1),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Tên & Trạng thái Online / Offline
        Column {
            Text(
                text = friend.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = friend.status,
                fontSize = 13.sp,
                color = if (friend.isOnline) Color(0xFF34C759) else Color.Gray
            )
        }
    }
}