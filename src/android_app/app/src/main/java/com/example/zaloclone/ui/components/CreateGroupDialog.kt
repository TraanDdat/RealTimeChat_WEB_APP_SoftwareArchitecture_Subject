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
import com.example.zaloclone.ui.model.ChatItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.zaloclone.ui.viewModel.*
import com.example.zaloclone.api.model.*
import android.widget.Toast

@Composable
fun CreateGroupDialog(
    groupViewModel: GroupViewModel,
    chatViewModel: ChatViewModel,
    context: Context,
    onDismiss: () -> Unit,
    onGroupCreated: (String, String) -> Unit
) {
    var groupNameInput by remember { mutableStateOf("") }
    val chatState by chatViewModel.state.collectAsState()
    val allContacts = chatState.allContacts
    val friendList: List<ChatUser> = allContacts
    val selectedFriends = remember { mutableStateMapOf<Int, Boolean>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create new group", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                OutlinedTextField(
                    value = groupNameInput,
                    onValueChange = { groupNameInput = it },
                    label = { Text("Group name (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Select (${selectedFriends.count { it.value }} selected):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(friendList) { friend ->
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
                                name = friend.username,
                                //avatarBgColor = friend.safeAvatarBgColor,
                                //avatarUri = friend.avatarUri,
                                modifier = Modifier.size(36.dp),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = friend.username,
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
                    val selectedMembers = friendList.filter {
                        selectedFriends[it.id] == true
                    }
                    if (selectedMembers.isNotEmpty()) {
                        val memberIds = selectedMembers.map { it.id }
                        groupViewModel.createGroup(
                            name = groupNameInput.trim(),
                            memberIds = memberIds
                        ) { success: Boolean ->
                            if (success) {
                                groupViewModel.getGroups()
                                onDismiss()
                                Toast.makeText(
                                    context,
                                    "Group created successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Failed to create group",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                },
                enabled = selectedFriends.any { it.value }
            ) {
                Text("Create new group")
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