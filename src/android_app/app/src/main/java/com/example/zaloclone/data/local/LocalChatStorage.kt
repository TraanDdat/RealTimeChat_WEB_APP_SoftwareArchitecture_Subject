package com.example.zaloclone.data.local

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.zaloclone.ui.model.ChatItem
import com.example.zaloclone.ui.model.FriendItem
import com.example.zaloclone.ui.model.Message
import com.example.zaloclone.api.model.*

// JSON file storage has been removed in favor of Postgres (users, conversations, conversationParticipants) and MongoDB (chatting).
// This file is kept as a stub for backward compatibility if needed.

fun saveMessagesToJson(context: Context, chatId: String, messages: List<Message>) {}
fun loadMessagesFromJson(context: Context, chatId: String): List<Message> = emptyList()
fun getOrInitChatHistory(context: Context, chatId: String, chatName: String): List<Message> = emptyList()
fun saveChatListToJson(context: Context, chatList: List<ChatItem>) {}
fun loadChatListFromJson(context: Context): List<ChatItem> = emptyList()
fun syncChatListWithFriends(context: Context) {}
fun syncChatListWithHistory(context: Context) {}
fun saveFriendListToJson(context: Context, friendList: List<FriendItem>) {}
fun loadFriendListFromJson(context: Context): List<FriendItem> = emptyList()
fun deleteChat(context: Context, chatId: String) {}
fun addFriendAndChat(context: Context, id: String, name: String, email: String = "", avatarBgColor: Color, avatarUri: String? = null) {}
fun updateGroupName(context: Context, groupId: String, newName: String) {}
fun updateFriendNickname(context: Context, friendId: String, newNickname: String) {}
fun updateGroupMembers(context: Context, groupId: String, newMembers: List<String>) {}
fun saveUserProfileToJson(context: Context, profile: UserProfile) {}
fun loadUserProfileFromJson(context: Context): UserProfile = UserProfile()
