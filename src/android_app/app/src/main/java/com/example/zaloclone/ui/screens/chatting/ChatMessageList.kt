package com.example.zaloclone.ui.screens.chatting

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.zaloclone.ui.model.Message
import com.example.zaloclone.ui.model.MessageType

@Composable
fun MessageBubble(
    message: Message,
    senderName: String,
    onImageClick: (String) -> Unit = {},
    onReactionSelect: (Message, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var showReactionMenu by remember { mutableStateOf(false) }

    val bubbleColor = if (message.isMe) Color(0xFFF2F2F7) else Color.White
    val borderColor = if (message.isMe) Color.Transparent else Color(0xFFE5E5EA)
    val alignment = if (message.isMe) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (message.isMe) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            horizontalAlignment = if (message.isMe) Alignment.End else Alignment.Start
        ) {
            if (showReactionMenu) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, Color(0xFFE5E5EA)),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val reactions = listOf("❤️", "👍", "😂", "😮", "😢", "😡")
                        reactions.forEach { reaction ->
                            Text(
                                text = reaction,
                                fontSize = 20.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        onReactionSelect(message, reaction)
                                        showReactionMenu = false
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = if (message.isMe) Arrangement.End else Arrangement.Start
            ) {
                if (!message.isMe) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2C4B1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(senderName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Box {
                    Surface(
                        color = if (message.type == MessageType.EMOTION) Color.Transparent else bubbleColor,
                        shape = shape,
                        border = if (!message.isMe && message.type != MessageType.EMOTION) BorderStroke(1.dp, borderColor) else null,
                        modifier = Modifier.clickable {
                            showReactionMenu = !showReactionMenu
                        }
                    ) {
                        when (message.type) {
                            MessageType.TEXT -> {
                                Text(
                                    text = message.text,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    color = Color.Black,
                                    fontSize = 15.sp
                                )
                            }
                            MessageType.AUDIO -> {
                                AudioMessageContent(duration = message.duration ?: "0:00")
                            }
                            MessageType.IMAGE -> {
                                ImageMessageContent(
                                    imageUrl = message.mediaUrl,
                                    onImageClick = { url -> onImageClick(url) }
                                )
                            }
                            MessageType.VIDEO -> {
                                VideoMessageContent(
                                    videoUrl = message.mediaUrl,
                                    onPlayClick = { url ->
                                        url?.let {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(Uri.parse(it), "video/*")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                )
                            }
                            MessageType.DOCUMENT -> {
                                DocumentMessageContent(
                                    fileName = message.fileName ?: "Document",
                                    fileSize = message.fileSize ?: "File",
                                    fileUrl = message.mediaUrl,
                                    onOpenClick = { url ->
                                        url?.let {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(Uri.parse(it), "*/*")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                )
                            }
                            MessageType.EMOTION -> {
                                EmotionMessageContent(emotion = message.emotion ?: "😊")
                            }
                        }
                    }

                    message.reaction?.let { reactionText ->
                        Surface(
                            color = Color.White,
                            shape = CircleShape,
                            shadowElevation = 2.dp,
                            border = BorderStroke(1.dp, Color(0xFFE5E5EA)),
                            modifier = Modifier
                                .align(if (message.isMe) Alignment.BottomStart else Alignment.BottomEnd)
                                .offset(y = 8.dp, x = if (message.isMe) (-6).dp else 6.dp)
                        ) {
                            Text(
                                text = reactionText,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageMessageContent(imageUrl: String?, onImageClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .sizeIn(maxWidth = 220.dp, maxHeight = 240.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { imageUrl?.let { onImageClick(it) } }
    ) {
        if (!imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Image message",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Image, contentDescription = "Image", tint = Color.White)
            }
        }
    }
}

@Composable
fun VideoMessageContent(videoUrl: String?, onPlayClick: (String?) -> Unit) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onPlayClick(videoUrl) },
        contentAlignment = Alignment.Center
    ) {
        if (!videoUrl.isNullOrEmpty()) {
            AsyncImage(
                model = videoUrl,
                contentDescription = "Video thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play Video",
                tint = Color(0xFF5B61F4),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun DocumentMessageContent(
    fileName: String,
    fileSize: String,
    fileUrl: String?,
    onOpenClick: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .width(240.dp)
            .clickable { onOpenClick(fileUrl) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE3F2FD)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.InsertDriveFile,
                contentDescription = "Document",
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (fileSize.isNotBlank()) fileSize else "Tài liệu",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "Open Document",
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun EmotionMessageContent(emotion: String) {
    Box(
        modifier = Modifier.padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (emotion.contains(" ")) {
            Surface(
                color = Color(0xFFFFF3E0),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFFE0B2))
            ) {
                Text(
                    text = emotion,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        } else {
            Text(
                text = emotion,
                fontSize = 44.sp
            )
        }
    }
}

@Composable
fun AudioMessageContent(duration: String) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .width(180.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8E8F5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color(0xFF5B61F4),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE5E5EA))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(4.dp)
                        .background(Color(0xFF5B61F4))
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = duration, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun TimeDivider(time: String) {
    val timeText = time.toString().substring(11, 16)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = timeText, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
    }
}