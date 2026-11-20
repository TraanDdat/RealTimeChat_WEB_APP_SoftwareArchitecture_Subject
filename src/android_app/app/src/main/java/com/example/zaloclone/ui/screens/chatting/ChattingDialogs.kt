package com.example.zaloclone.ui.screens.chatting

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.zaloclone.data.local.updateFriendNickname
import com.example.zaloclone.ui.components.UserAvatar
import com.example.zaloclone.api.model.*
import com.example.zaloclone.ui.viewModel.*

@Composable
fun FullscreenImageViewerDialog(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            AsyncImage(
                model = imageUrl,
                contentDescription = "Full Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun GroupInfoDialog(
    groupName: String,
    members: List<String>,
    onDismiss: () -> Unit,
    onAddMemberClick: () -> Unit,
    onChangeGroupNameClick: () -> Unit
) {
    val colors = listOf(
        Color(0xFF38C7FF), Color(0xFF5B61F4), Color(0xFF34C759),
        Color(0xFFFF9500), Color(0xFFAF52DE), Color(0xFFE2C4B1)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5B61F4)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = groupName.take(1),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = groupName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onChangeGroupNameClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Group Name",
                            tint = Color(0xFF5B61F4),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = "${members.size} thành viên",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
            ) {
                HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Danh sách thành viên (${members.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    TextButton(
                        onClick = onAddMemberClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+ Thêm",
                            color = Color(0xFF5B61F4),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(members.size) { index ->
                        val memberName = members[index]
                        val memberColor = colors[memberName.hashCode().let { if (it < 0) -it else it } % colors.size]
                        val isLeader = index == 0 || memberName.contains("Bạn")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(memberColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = memberName.take(1),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = memberName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            if (isLeader) {
                                Surface(
                                    color = Color(0xFFE3F2FD),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Trưởng nhóm",
                                        color = Color(0xFF0288D1),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Thành viên",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        if (index < members.size - 1) {
                            HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 0.5.dp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B61F4))
            ) {
                Text("Đóng")
            }
        }
    )
}

@Composable
fun AddMemberDialog(
    allContacts: List<ChatUser>,
    currentMembers: List<Int>,
    onDismiss: () -> Unit,
    onMembersAdded: (List<Int>) -> Unit
) {
    val availableFriends = remember(allContacts, currentMembers) {
        allContacts.filter { it.id !in currentMembers }
    }
    val selected = remember { mutableStateMapOf<Int, Boolean>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm thành viên vào nhóm", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                if (availableFriends.isEmpty()) {
                    Text(
                        text = "Tất cả bạn bè đã có trong nhóm.",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(availableFriends) { friend ->
                            val isChecked = selected[friend.id] == true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selected[friend.id] = !isChecked }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { selected[friend.id] = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                UserAvatar(
                                    name = friend.username,
                                    // = friend.safeAvatarBgColor,
                                    //avatarUri = friend.avatarUri,
                                    modifier = Modifier.size(36.dp),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = friend.username,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newMemberIds = availableFriends.filter { selected[it.id] == true }.map { it.id }
                    if (newMemberIds.isNotEmpty()) {
                        onMembersAdded(newMemberIds)
                    }
                },
                enabled = selected.any { it.value }
            ) {
                Text("Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun ChangeGroupNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đổi tên nhóm", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Tên nhóm mới") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        onSave(nameInput.trim())
                    }
                },
                enabled = nameInput.isNotBlank()
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun IndividualChatSettingsDialog(
    context: Context,
    friendId: Int,
    friendName: String,
    onDismiss: () -> Unit,
    chatViewModel: ChatViewModel,
    onNicknameUpdated: (String) -> Unit
) {
    val chatState by chatViewModel.state.collectAsState()
    val allContacts = chatState.allContacts
    val friendList = allContacts
    val friend = friendList.find { it.id == friendId }
    var nicknameInput by remember { mutableStateOf(friend?.username ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cài đặt cá nhân & Biệt danh", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Tên gốc: $friendName", fontSize = 14.sp, color = Color.Gray)
                if (friend?.email?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Gmail: ${friend.email}", fontSize = 14.sp, color = Color.Gray)
                } /*else if (friend?.phone?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Gmail: ${friend.phone}", fontSize = 14.sp, color = Color.Gray)
                }*/
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = nicknameInput,
                    onValueChange = { nicknameInput = it },
                    label = { Text("Biệt danh") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    //updateFriendNickname(context, friendId, nicknameInput.trim())
                    onNicknameUpdated(nicknameInput.trim().ifBlank { friendName })
                }
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}