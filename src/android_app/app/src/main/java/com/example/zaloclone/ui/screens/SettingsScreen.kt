package com.example.zaloclone.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.zaloclone.data.local.loadUserProfileFromJson
import com.example.zaloclone.data.local.saveUserProfileToJson
import com.example.zaloclone.ui.screens.settings.*

@Composable
fun SettingsScreen(
    onNavigateToChats: () -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    var userProfile by remember { mutableStateOf(loadUserProfileFromJson(context)) }

    // Dialog State
    var showChangeNameDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showChangeAvatarDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // Image Picker Launcher for Avatar
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val updated = userProfile.copy(avatarUri = it.toString())
            userProfile = updated
            saveUserProfileToJson(context, updated)
            Toast.makeText(context, "Cập nhật avatar thành công!", Toast.LENGTH_SHORT).show()
        }
    }

    val cyanHeaderColor = Color(0xFF38C7FF)
    val navActiveColor = Color(0xFF5B61F4)
    val avatarBgColor = Color(userProfile.avatarBgColorHex)

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(cyanHeaderColor)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigateToChats() },
                    icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Chats") },
                    label = { Text("Chats") },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray)
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigateToFriends() },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Friends") },
                    label = { Text("Contacts") },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray)
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = navActiveColor,
                        selectedTextColor = navActiveColor,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F6F8))
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // User Profile Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(avatarBgColor)
                            .clickable { showChangeAvatarDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!userProfile.avatarUri.isNullOrBlank()) {
                            AsyncImage(
                                model = userProfile.avatarUri,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = userProfile.name.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(Color(0xFF5B61F4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Gmail: ${userProfile.email}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    IconButton(onClick = { showChangeNameDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            tint = Color(0xFF5B61F4)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Account Settings",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    SettingsMenuItem(
                        icon = Icons.Default.Person,
                        iconBgColor = Color(0xFF4A90E2),
                        title = "Change nickname",
                        subtitle = userProfile.name,
                        onClick = { showChangeNameDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFF0F0F0))

                    SettingsMenuItem(
                        icon = Icons.Default.CameraAlt,
                        iconBgColor = Color(0xFF9013FE),
                        title = "Change avatar",
                        subtitle = "",
                        onClick = { showChangeAvatarDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFF0F0F0))

                    SettingsMenuItem(
                        icon = Icons.Default.Lock,
                        iconBgColor = Color(0xFFF5A623),
                        title = "Change password",
                        subtitle = "",
                        onClick = { showChangePasswordDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "System",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                SettingsMenuItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    iconBgColor = Color(0xFFFF3B30),
                    title = "Log out",
                    titleColor = Color(0xFFFF3B30),
                    subtitle = "",
                    onClick = { showLogoutConfirmDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showChangeNameDialog) {
        ChangeNameDialog(
            context = context,
            userProfile = userProfile,
            onProfileUpdated = { userProfile = it },
            onDismiss = { showChangeNameDialog = false }
        )
    }

    if (showChangeAvatarDialog) {
        ChangeAvatarDialog(
            context = context,
            userProfile = userProfile,
            imagePickerLauncher = imagePickerLauncher,
            onProfileUpdated = { userProfile = it },
            onDismiss = { showChangeAvatarDialog = false }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            context = context,
            userProfile = userProfile,
            onProfileUpdated = { userProfile = it },
            onDismiss = { showChangePasswordDialog = false }
        )
    }

    if (showLogoutConfirmDialog) {
        LogoutConfirmDialog(
            onLogout = onLogout,
            onDismiss = { showLogoutConfirmDialog = false }
        )
    }
}

@Composable
fun SettingsMenuItem(
    icon: ImageVector,
    iconBgColor: Color,
    title: String,
    titleColor: Color = Color.Black,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = titleColor
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.LightGray
        )
    }
}