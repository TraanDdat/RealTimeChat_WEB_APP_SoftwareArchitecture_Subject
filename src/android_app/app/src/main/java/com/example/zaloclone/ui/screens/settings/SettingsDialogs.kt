package com.example.zaloclone.ui.screens.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zaloclone.data.local.saveUserProfileToJson
import com.example.zaloclone.ui.model.UserProfile

@Composable
fun ChangeNameDialog(
    context: Context,
    userProfile: UserProfile,
    onProfileUpdated: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var newNameInput by remember { mutableStateOf(userProfile.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
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
                        onProfileUpdated(updated)
                        saveUserProfileToJson(context, updated)
                        onDismiss()
                        Toast.makeText(context, "Đổi tên thành công!", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = newNameInput.isNotBlank()
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun ChangeAvatarDialog(
    context: Context,
    userProfile: UserProfile,
    imagePickerLauncher: ManagedActivityResultLauncher<String, Uri?>,
    onProfileUpdated: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val presetColors = listOf(
        0xFF38C7FF, // Cyan
        0xFF5B61F4, // Indigo
        0xFF34C759, // Green
        0xFFFF9500, // Orange
        0xFFE0C1C6, // Pink
        0xFF78909C  // Grey
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đổi avatar", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Chọn phương thức đổi avatar:", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onDismiss()
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
                                        avatarUri = null
                                    )
                                    onProfileUpdated(updated)
                                    saveUserProfileToJson(context, updated)
                                    onDismiss()
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
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    context: Context,
    userProfile: UserProfile,
    onProfileUpdated: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                            onProfileUpdated(updated)
                            saveUserProfileToJson(context, updated)
                            onDismiss()
                            Toast.makeText(context, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ) {
                Text("Cập nhật")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun LogoutConfirmDialog(
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đăng xuất", fontWeight = FontWeight.Bold) },
        text = { Text("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản không?") },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onLogout()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
            ) {
                Text("Đăng xuất", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}