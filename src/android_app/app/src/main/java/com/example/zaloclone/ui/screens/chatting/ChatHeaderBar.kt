package com.example.zaloclone.ui.screens.chatting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zaloclone.ui.components.PresenceBadge
import com.example.zaloclone.ui.model.PresenceStatus

@Composable
fun ChatTopBar(
    name: String,
    status: String,
    isGroup: Boolean = false,
    memberCount: Int = 0,
    presenceStatus: PresenceStatus? = null,
    onBackClick: () -> Unit,
    onInfoClick: () -> Unit = {}
) {
    Surface(shadowElevation = 2.dp, color = Color.White) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Gray
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = isGroup) { onInfoClick() }
            ) {
                Text(text = name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isGroup && presenceStatus != null) {
                        PresenceBadge(status = presenceStatus, size = 10.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isGroup) "$memberCount thành viên" else status,
                        fontSize = 12.sp,
                        color = if (isGroup) Color(0xFF5B61F4) else Color.Gray,
                        fontWeight = if (isGroup) FontWeight.Medium else FontWeight.Normal
                    )
                    if (isGroup) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(Xem)",
                            fontSize = 11.sp,
                            color = Color(0xFF5B61F4)
                        )
                    }
                }
            }
            Row {
                IconButton(onClick = { onInfoClick() }) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Group Info",
                        tint = if (isGroup) Color(0xFF5B61F4) else Color.Gray
                    )
                }
            }
        }
    }
}
