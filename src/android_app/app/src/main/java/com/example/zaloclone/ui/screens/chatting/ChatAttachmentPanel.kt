package com.example.zaloclone.ui.screens.chatting

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

fun getFileNameAndSizeFromUri(context: Context, uri: Uri): Pair<String, String> {
    var name = "Document"
    var sizeStr = ""
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) {
                    val foundName = cursor.getString(nameIndex)
                    if (!foundName.isNullOrEmpty()) name = foundName
                }
                if (sizeIndex != -1) {
                    val bytes = cursor.getLong(sizeIndex)
                    sizeStr = when {
                        bytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
                        bytes >= 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
                        else -> "$bytes B"
                    }
                }
            }
        }
    } catch (e: Exception) {
        name = uri.lastPathSegment ?: "Document"
    }
    return Pair(name, sizeStr)
}

@Composable
fun AttachmentOptionPanel(
    onSelectImage: () -> Unit,
    onSelectVideo: () -> Unit,
    onSelectDocument: () -> Unit,
    onSelectEmotion: () -> Unit
) {
    Surface(
        color = Color(0xFFF9F9FB),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AttachmentOptionItem(
                icon = Icons.Default.Image,
                label = "Hình ảnh",
                bgColor = Color(0xFF4CAF50),
                onClick = onSelectImage
            )
            AttachmentOptionItem(
                icon = Icons.Default.Videocam,
                label = "Video",
                bgColor = Color(0xFFF44336),
                onClick = onSelectVideo
            )
            AttachmentOptionItem(
                icon = Icons.Default.InsertDriveFile,
                label = "Tài liệu",
                bgColor = Color(0xFF2196F3),
                onClick = onSelectDocument
            )
            AttachmentOptionItem(
                icon = Icons.Default.EmojiEmotions,
                label = "Cảm xúc",
                bgColor = Color(0xFFFF9800),
                onClick = onSelectEmotion
            )
        }
    }
}

@Composable
fun AttachmentOptionItem(
    icon: ImageVector,
    label: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
    }
}

@Composable
fun EmotionPickerSheet(
    onDismiss: () -> Unit,
    onEmotionSelected: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val emojis = listOf(
        "😊", "😂", "❤️", "👍", "🔥", "🎉", "🥳", "😮",
        "😢", "😡", "👏", "💯", "💩", "🙏", "✨", "😍",
        "🤔", "😎", "🤣", "💖", "🥰", "🙌", "💥", "⭐",
        "🤩", "😴", "🤝", "😇", "🥳", "💪", "🚀", "🎯"
    )

    val stickers = listOf(
        "🐱 Mèo Vui Vẻ", "🐻 Gấu Thương Thương", "🐰 Thỏ Cười Khì", "🐶 Chó Tung Hoa",
        "🦊 Cáo Thả Tim", "🐼 Gấu Trúc Say Hi", "🦁 Sư Tử Cool", "🐸 Ếch Nhảy Múa",
        "🐣 Gà Con Xinh", "🐯 Hổ Dễ Thương", "🦄 Kỳ Lân May Mắn", "🐙 Bạch Tuộc Yêu"
    )

    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F2F7)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.weight(1f),
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Biểu cảm", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Nhãn dán", fontWeight = FontWeight.Bold) }
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            if (selectedTab == 0) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    items(emojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { onEmotionSelected(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 22.sp)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    items(stickers) { sticker ->
                        Surface(
                            modifier = Modifier
                                .padding(4.dp)
                                .clickable { onEmotionSelected(sticker) },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF2F2F7),
                            border = BorderStroke(1.dp, Color(0xFFE5E5EA))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sticker,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}