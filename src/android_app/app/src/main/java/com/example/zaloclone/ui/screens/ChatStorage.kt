package com.example.zaloclone.ui.screens

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.zaloclone.api.model.*
import com.example.zaloclone.ui.model.MessageType
import com.example.zaloclone.ui.components.CreateGroupDialog as ComponentCreateGroupDialog
import com.example.zaloclone.ui.viewModel.*

// Typealiases & Model Re-exports for backward compatibility
typealias MessageType = MessageType
typealias Message = com.example.zaloclone.ui.model.Message
typealias ChatItem = com.example.zaloclone.ui.model.ChatItem
typealias FriendItem = com.example.zaloclone.ui.model.FriendItem

// Local storage delegation helpers
fun saveMessagesToJson(context: Context, chatId: String, messages: List<Message>) =
    com.example.zaloclone.data.local.saveMessagesToJson(context, chatId, messages)

fun loadMessagesFromJson(context: Context, chatId: String): List<Message> =
    com.example.zaloclone.data.local.loadMessagesFromJson(context, chatId)

fun getOrInitChatHistory(context: Context, chatId: String, chatName: String): List<Message> =
    com.example.zaloclone.data.local.getOrInitChatHistory(context, chatId, chatName)

fun saveChatListToJson(context: Context, chatList: List<ChatItem>) =
    com.example.zaloclone.data.local.saveChatListToJson(context, chatList)

fun loadChatListFromJson(context: Context): List<ChatItem> =
    com.example.zaloclone.data.local.loadChatListFromJson(context)

fun syncChatListWithFriends(context: Context) =
    com.example.zaloclone.data.local.syncChatListWithFriends(context)

fun syncChatListWithHistory(context: Context) =
    com.example.zaloclone.data.local.syncChatListWithHistory(context)

fun saveFriendListToJson(context: Context, friendList: List<FriendItem>) =
    com.example.zaloclone.data.local.saveFriendListToJson(context, friendList)

fun loadFriendListFromJson(context: Context): List<FriendItem> =
    com.example.zaloclone.data.local.loadFriendListFromJson(context)

fun deleteChat(context: Context, chatId: String) =
    com.example.zaloclone.data.local.deleteChat(context, chatId)

fun deleteFriend(context: Context, friendId: String) =
    com.example.zaloclone.data.local.deleteFriend(context, friendId)

fun addFriendAndChat(
    context: Context,
    id: String,
    name: String,
    email: String = "",
    avatarBgColor: Color,
    avatarUri: String? = null
) = com.example.zaloclone.data.local.addFriendAndChat(context, id, name, email, avatarBgColor, avatarUri)

@Composable
fun CreateGroupDialog(
    chatViewModel: ChatViewModel,
    groupViewModel: GroupViewModel,
    context: Context,
    onDismiss: () -> Unit,
    onGroupCreated: (String, String) -> Unit
) = ComponentCreateGroupDialog(
    context = context,
    onDismiss = onDismiss,
    onGroupCreated = onGroupCreated,
    chatViewModel = chatViewModel,
    groupViewModel = groupViewModel)

fun updateGroupName(context: Context, groupId: String, newName: String) =
    com.example.zaloclone.data.local.updateGroupName(context, groupId, newName)

fun updateFriendNickname(context: Context, friendId: String, newNickname: String) =
    com.example.zaloclone.data.local.updateFriendNickname(context, friendId, newNickname)

fun updateGroupMembers(context: Context, groupId: String, newMembers: List<String>) =
    com.example.zaloclone.data.local.updateGroupMembers(context, groupId, newMembers)

fun saveUserProfileToJson(context: Context, profile: UserProfile) =
    com.example.zaloclone.data.local.saveUserProfileToJson(context, profile)

fun loadUserProfileFromJson(context: Context): UserProfile =
    com.example.zaloclone.data.local.loadUserProfileFromJson(context)