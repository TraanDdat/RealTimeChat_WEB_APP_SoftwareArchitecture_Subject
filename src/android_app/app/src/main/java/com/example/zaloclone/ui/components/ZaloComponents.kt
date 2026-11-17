package com.example.zaloclone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.border
import androidx.compose.ui.unit.Dp
import com.example.zaloclone.ui.model.*
import com.example.zaloclone.ui.theme.ButtonGray
import com.example.zaloclone.ui.theme.TextGray
import com.example.zaloclone.ui.theme.ZaloCyan
import com.example.zaloclone.ui.viewModel.ChatState
import com.example.zaloclone.ui.viewModel.FriendState
import com.example.zaloclone.ui.viewModel.FriendViewModel
import com.example.zaloclone.ui.viewModel.MessageNotification

@Composable
fun PresenceBadge(
    status: PresenceStatus,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp
) {
    val color = when (status) {
        PresenceStatus.ONLINE -> Color(0xFF4CAF50)
        PresenceStatus.AWAY -> Color(0xFFFFC107)
        PresenceStatus.BUSY -> Color(0xFFF44336)
        PresenceStatus.OFFLINE -> Color(0xFF9E9E9E)
    }

    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape)
            .border(2.dp, Color.White, CircleShape)
    )
}

@Composable
fun UserAvatar(
    name: String,
    avatarBgColor: Color,
    modifier: Modifier = Modifier,
    avatarUri: String? = null,
    fontSize: TextUnit = 18.sp,
    presenceStatus: PresenceStatus? = null
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(avatarBgColor),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUri.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = name.ifBlank { " " }.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    color = Color.White
                )
            }
        }
        if (presenceStatus != null) {
            PresenceBadge(
                status = presenceStatus,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = ZaloCyan),
        shape = RoundedCornerShape(50),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = ButtonGray),
        shape = RoundedCornerShape(50),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text(text = text, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZaloTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextGray) },
        visualTransformation = visualTransformation,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ZaloCyan,
            unfocusedBorderColor = ZaloCyan.copy(alpha = 0.5f)
        ),
        singleLine = true
    )
}

@Composable
fun NotificationDialog(
    chatState: ChatState,
    friendState: FriendState,
    friendViewModel: FriendViewModel,
    onDismiss: () -> Unit,
    onAcceptFriend: (Int) -> Unit,
    onRejectFriend: (Int) -> Unit,
    onNotificationMessageClick: (MessageNotification) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            friendViewModel.getRequest()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Notifications", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { selectedTab = 0 }) {
                        Text(
                            text = "Friend Requests (${friendState.friendRequests.size})",
                            color = if (selectedTab == 0) Color(0xFF00BCD4) else Color.Gray,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    TextButton(onClick = { selectedTab = 1 }) {
                        Text(
                            text = "Message (${chatState.unreadNotifications.size})",
                            color = if (selectedTab == 1) Color(0xFF00BCD4) else Color.Gray,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (selectedTab == 0) {
                        if (friendState.friendRequests.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No friend requests", color = Color.Gray)
                                }
                            }
                        } else {
                            items(friendState.friendRequests, key = { it.id }) { req ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F7)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                modifier = Modifier.size(40.dp),
                                                shape = CircleShape,
                                                color = Color(0xFF00BCD4)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = req.username.firstOrNull()?.uppercase() ?: "?",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = req.username, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Text(text = req.email, fontSize = 12.sp, color = Color.Gray)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        //Text(text = req.message, fontSize = 13.sp, color = Color.DarkGray)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            OutlinedButton(
                                                onClick = { onRejectFriend(req.id) },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text("Reject", fontSize = 13.sp)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = { onAcceptFriend(req.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text("Accept", fontSize = 13.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    } else {
                        if (chatState.unreadNotifications.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No unseen message", color = Color.Gray)
                                }
                            }
                        } else {
                            items(chatState.unreadNotifications, key = { it.id }) { notif ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNotificationMessageClick(notif) },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(40.dp),
                                            shape = CircleShape,
                                            color = Color(0xFF00ACC1)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = notif.senderName.firstOrNull()?.uppercase() ?: "?",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = notif.senderName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text(text = notif.text, fontSize = 13.sp, color = Color.DarkGray, maxLines = 1)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
