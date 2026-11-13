package com.example.zaloclone.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

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
                    // Avatar với nút đổi avatar
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

                        // Icon camera nhỏ phía dưới góc avatar
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
                            text = "SĐT: ${userProfile.phone}",
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
                text = "TÀI KHOẢN & BẢO MẬT",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            // Settings Menu Items Card
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
                        title = "Đổi tên hiển thị",
                        subtitle = userProfile.name,
                        onClick = { showChangeNameDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFF0F0F0))

                    SettingsMenuItem(
                        icon = Icons.Default.CameraAlt,
                        iconBgColor = Color(0xFF9013FE),
                        title = "Đổi avatar",
                        subtitle = "Cập nhật ảnh đại diện",
                        onClick = { showChangeAvatarDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFF0F0F0))

                    SettingsMenuItem(
                        icon = Icons.Default.Lock,
                        iconBgColor = Color(0xFFF5A623),
                        title = "Đổi mật khẩu",
                        subtitle = "Bảo mật tài khoản",
                        onClick = { showChangePasswordDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "HỆ THỐNG",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            // Logout Card
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
                    title = "Đăng xuất",
                    titleColor = Color(0xFFFF3B30),
                    subtitle = "Thoát tài khoản khỏi ứng dụng",
                    onClick = { showLogoutConfirmDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 1. DIALOG ĐỔI TÊN
    if (showChangeNameDialog) {
        var newNameInput by remember { mutableStateOf(userProfile.name) }
        AlertDialog(
            onDismissRequest = { showChangeNameDialog = false },
            title = { Text("Đổi tên hiển thị", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Nhập tên mới của bạn:", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newNameInput,
                        onValueChange = { newNameInput = it },
                        label = { Text("Tên hiển thị") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNameInput.isNotBlank()) {
                            val updated = userProfile.copy(name = newNameInput.trim())
                            userProfile = updated
                            saveUserProfileToJson(context, updated)
                            showChangeNameDialog = false
                            Toast.makeText(context, "Đổi tên thành công!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = newNameInput.isNotBlank()
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeNameDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // 2. DIALOG ĐỔI AVATAR
    if (showChangeAvatarDialog) {
        val presetColors = listOf(
            0xFF38C7FF, // Cyan
            0xFF5B61F4, // Indigo
            0xFF34C759, // Green
            0xFFFF9500, // Orange
            0xFFE0C1C6, // Pink
            0xFF78909C  // Grey
        )

        AlertDialog(
            onDismissRequest = { showChangeAvatarDialog = false },
            title = { Text("Đổi avatar", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Chọn phương thức đổi avatar:", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Option 1: Pick from gallery
                    Button(
                        onClick = {
                            showChangeAvatarDialog = false
                            imagePickerLauncher.launch("image/*")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B61F4)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chọn ảnh từ thư viện", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Hoặc chọn màu nền avatar:", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 2: Choose preset color
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetColors) { colorHex ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorHex))
                                    .clickable {
                                        val updated = userProfile.copy(
                                            avatarBgColorHex = colorHex,
                                            avatarUri = null // Reset custom image uri to use color
                                        )
                                        userProfile = updated
                                        saveUserProfileToJson(context, updated)
                                        showChangeAvatarDialog = false
                                        Toast.makeText(context, "Cập nhật avatar thành công!", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (userProfile.avatarUri.isNullOrEmpty() && userProfile.avatarBgColorHex == colorHex) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showChangeAvatarDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    // 3. DIALOG ĐỔI MẬT KHẨU
    if (showChangePasswordDialog) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Đổi mật khẩu", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it; errorMessage = null },
                        label = { Text("Mật khẩu hiện tại") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; errorMessage = null },
                        label = { Text("Mật khẩu mới") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMessage = null },
                        label = { Text("Xác nhận mật khẩu mới") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            oldPassword != userProfile.password -> {
                                errorMessage = "Mật khẩu hiện tại không đúng!"
                            }
                            newPassword.isBlank() -> {
                                errorMessage = "Mật khẩu mới không được để trống!"
                            }
                            newPassword.length < 4 -> {
                                errorMessage = "Mật khẩu mới phải có ít nhất 4 ký tự!"
                            }
                            newPassword != confirmPassword -> {
                                errorMessage = "Xác nhận mật khẩu mới không khớp!"
                            }
                            else -> {
                                val updated = userProfile.copy(password = newPassword)
                                userProfile = updated
                                saveUserProfileToJson(context, updated)
                                showChangePasswordDialog = false
                                Toast.makeText(context, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Cập nhật")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // 4. DIALOG XÁC NHẬN ĐĂNG XUẤT
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Đăng xuất", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản không?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                ) {
                    Text("Đăng xuất", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
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
