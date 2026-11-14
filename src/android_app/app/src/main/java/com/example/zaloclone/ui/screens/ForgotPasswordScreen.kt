package com.example.zaloclone.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zaloclone.ui.components.PrimaryButton
import com.example.zaloclone.ui.components.SecondaryButton
import com.example.zaloclone.ui.components.ZaloTextField
import com.example.zaloclone.ui.theme.TextGray
import com.example.zaloclone.ui.theme.ZaloCyan

@Composable
fun ForgotPasswordScreen(
    onNavigateToWelcome: () -> Unit,
    onPasswordResetSuccess: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Quên mật khẩu",
            fontSize = 32.sp,
            color = ZaloCyan,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 10.dp)
        )

        Text(
            text = "Nhập Gmail đăng ký để nhận mã OTP xác thực và tạo mật khẩu mới.",
            fontSize = 14.sp,
            color = TextGray,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 20.dp)
        )

        ZaloTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = "Gmail"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { otpCode = it },
                    placeholder = { Text("Mã OTP", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ZaloCyan,
                        unfocusedBorderColor = ZaloCyan.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )
            }
            Button(
                onClick = {
                    if (email.isBlank()) {
                        Toast.makeText(context, "Vui lòng nhập Gmail trước", Toast.LENGTH_SHORT).show()
                    } else {
                        isOtpSent = true
                        Toast.makeText(context, "Đã gửi mã OTP đến $email (Mã mẫu: 123456)", Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ZaloCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Text(text = if (isOtpSent) "Gửi lại" else "Gửi OTP", color = Color.White, fontSize = 14.sp)
            }
        }

        ZaloTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            placeholder = "Mật khẩu mới",
            visualTransformation = PasswordVisualTransformation()
        )

        ZaloTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            placeholder = "Xác nhận mật khẩu mới",
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(20.dp))

        PrimaryButton(
            text = "Xác nhận đổi mật khẩu",
            onClick = {
                when {
                    email.isBlank() -> {
                        Toast.makeText(context, "Vui lòng nhập Gmail", Toast.LENGTH_SHORT).show()
                    }
                    otpCode.isBlank() -> {
                        Toast.makeText(context, "Vui lòng nhập mã OTP", Toast.LENGTH_SHORT).show()
                    }
                    newPassword.isBlank() -> {
                        Toast.makeText(context, "Vui lòng nhập mật khẩu mới", Toast.LENGTH_SHORT).show()
                    }
                    newPassword != confirmPassword -> {
                        Toast.makeText(context, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(context, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                        onPasswordResetSuccess()
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SecondaryButton(
            text = "Quay lại",
            onClick = onNavigateToWelcome
        )
    }
}
