package com.example.zaloclone.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zaloclone.data.local.loadChatListFromJson
import com.example.zaloclone.data.local.loadFriendListFromJson
import com.example.zaloclone.data.local.saveChatListToJson
import com.example.zaloclone.ui.model.ChatItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CreateGroupDialog(
    context: Context,
    onDismiss: () -> Unit,
    onGroupCreated: (String, String) -> Unit
) {
    var groupNameInput by remember { mutableStateOf("") }
    val friendList = remember { mutableStateOf(loadFriendListFromJson(context)) }
    val selectedFriends = remember { mutableStateMapOf<String, Boolean>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo nhóm chat mới", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                OutlinedTextField(
                    value = groupNameInput,
                    onValueChange = { groupNameInput = it },
                    label = { Text("Tên nhóm (để trống sẽ lấy tên thành viên)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Chọn thành viên (${selectedFriends.count { it.value }} đã chọn):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(friendList.value) { friend ->
                        val isSelected = selectedFriends[friend.id] == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedFriends[friend.id] = !isSelected
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedFriends[friend.id] = checked
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            UserAvatar(
                                name = friend.name,
                                avatarBgColor = friend.safeAvatarBgColor,
                                avatarUri = friend.avatarUri,
                                modifier = Modifier.size(36.dp),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = friend.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selectedMemberNames = friendList.value.filter { selectedFriends[it.id] == true }.map { it.name }
                    if (selectedMemberNames.isNotEmpty()) {
                        val groupId = "group_${System.currentTimeMillis()}"
                        val groupName = if (groupNameInput.isBlank()) {
                            selectedMemberNames.joinToString(", ")
                        } else {
                            groupNameInput.trim()
                        }
                        val allMembers = listOf("Bạn (Tôi)") + selectedMemberNames
                        val membersDescription = "Thành viên: ${selectedMemberNames.joinToString(", ")}"

                        val colors = listOf(Color(0xFF38C7FF), Color(0xFF5B61F4), Color(0xFF34C759), Color(0xFFFF9500), Color(0xFFAF52DE))
                        val randomColor = colors[groupId.hashCode().let { if (it < 0) -it else it } % colors.size]

                        val newChatItem = ChatItem(
                            id = groupId,
                            name = groupName,
                            lastMessage = membersDescription,
                            time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                            unreadCount = 0,
                            avatarBgColor = randomColor,
                            isGroup = true,
                            members = allMembers
                        )

                        val currentChats = loadChatListFromJson(context).toMutableList()
                        currentChats.add(0, newChatItem)
                        saveChatListToJson(context, currentChats)

                        onGroupCreated(groupId, groupName)
                    }
                },
                enabled = selectedFriends.any { it.value }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}