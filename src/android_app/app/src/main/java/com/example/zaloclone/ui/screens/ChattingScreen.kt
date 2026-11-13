package com.example.zaloclone.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zaloclone.ChatState
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChattingScreen(
    state: ChatState,
    currentUserId: Long,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSend: (String, Uri?, () -> Unit) -> Unit
) {
    val partner = state.selectedUser ?: return
    var text by rememberSaveable(partner.id) { mutableStateOf("") }
    var image by rememberSaveable(partner.id) { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) image = uri.toString()
    }
    LaunchedEffect(partner.id, state.messages.lastOrNull()?.id) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(partner.username, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (!state.connected) "Reconnecting" else if (partner.id in state.onlineUsers) "Online" else "Offline",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !state.loadingMessages) { Icon(Icons.Default.Refresh, "Refresh history") }
                }
            )
        },
        bottomBar = {
            Column {
                if (image != null) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = image, contentDescription = "Selected image", modifier = Modifier.size(72.dp), contentScale = ContentScale.Fit)
                        IconButton(onClick = { image = null }, enabled = !state.sending) { Icon(Icons.Default.Close, "Remove image") }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { imagePicker.launch("image/*") }, enabled = !state.sending) { Icon(Icons.Default.Image, "Attach image") }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { if (it.length <= 2000) text = it },
                        placeholder = { Text("Message") },
                        enabled = !state.sending,
                        maxLines = 4,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            onSend(text, image?.let(Uri::parse)) {
                                text = ""
                                image = null
                            }
                        },
                        enabled = !state.sending && (text.isNotBlank() || image != null)
                    ) {
                        if (state.sending) CircularProgressIndicator(Modifier.size(24.dp))
                        else Icon(Icons.AutoMirrored.Filled.Send, "Send")
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.loadingMessages) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.messages.isEmpty() && !state.loadingMessages) Text("No messages yet", Modifier.padding(24.dp))
            LazyColumn(state = listState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.messages, key = { it.id }) { message ->
                    val isSender = message.senderId == currentUserId
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = if (isSender) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (isSender) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSender) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(0.82f).widthIn(max = 420.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                message.image?.let {
                                    AsyncImage(
                                        model = it, contentDescription = "Shared image",
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 240.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                message.text?.takeIf { it.isNotBlank() }?.let { Text(it) }
                                Text(
                                    runCatching {
                                        OffsetDateTime.parse(message.createdAt).atZoneSameInstant(ZoneId.systemDefault())
                                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                                    }.getOrDefault(""),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}