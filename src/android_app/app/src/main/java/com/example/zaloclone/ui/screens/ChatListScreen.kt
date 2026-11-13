package com.example.zaloclone.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.zaloclone.ChatState
import com.example.zaloclone.ui.api.remote.ChatUser
import com.example.zaloclone.ui.components.UserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(state: ChatState, onSelect: (ChatUser) -> Unit, onRefresh: () -> Unit, onLogout: () -> Unit) {
    var showContacts by rememberSaveable { mutableStateOf(false) }
    val users = if (showContacts) state.contacts else state.chats
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("CloneZalo", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${state.user?.username.orEmpty()} - ${if (state.connected) "Connected" else "Reconnecting"}",
                            style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !state.loadingUsers) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = if (showContacts) 1 else 0) {
                Tab(selected = !showContacts, onClick = { showContacts = false }, text = { Text("Chats") })
                Tab(selected = showContacts, onClick = { showContacts = true }, text = { Text("Contacts") })
            }
            if (state.loadingUsers) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (users.isEmpty() && !state.loadingUsers) {
                Text(if (showContacts) "No contacts" else "No conversations", Modifier.padding(24.dp))
            }
            LazyColumn(Modifier.weight(1f)) {
                items(users, key = { it.id }) { user ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(user) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        UserAvatar(
                            name = user.username, avatarBgColor = MaterialTheme.colorScheme.primary,
                            avatarUri = user.profilePic, modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(user.username, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (state.connected && user.id in state.onlineUsers) "Online" else user.email,
                                style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}