package com.example.zaloclone.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
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
import com.example.zaloclone.ui.theme.ZaloCyan
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
<<<<<<< HEAD
fun RegisterScreen(
    onRegisterClick: () -> Unit,
    onNavigateToWelcome: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
=======
fun RegisterScreen(onRegisterClick: (String, String, String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
>>>>>>> feature/lan-with-android
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Register",
            fontSize = 32.sp,
            color = ZaloCyan,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 30.dp)
        )

        ZaloTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = "Email"
        )
        
        ZaloTextField(
            value = password,
            onValueChange = { password = it },
<<<<<<< HEAD
            placeholder = "Mật khẩu",
=======
            placeholder = "Password",
>>>>>>> feature/lan-with-android
            visualTransformation = PasswordVisualTransformation()
        )
        
        ZaloTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
<<<<<<< HEAD
            placeholder = "Nhập lại mật khẩu",
=======
            placeholder = "confirm Password",
>>>>>>> feature/lan-with-android
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(30.dp))

        PrimaryButton(
<<<<<<< HEAD
            text = "Đăng ký", 
            onClick = {
                when {
                    name.isBlank() -> Toast.makeText(context, "Vui lòng nhập tên", Toast.LENGTH_SHORT).show()
                    phoneNumber.isBlank() -> Toast.makeText(context, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show()
                    password.isBlank() -> Toast.makeText(context, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show()
                    password != confirmPassword -> Toast.makeText(context, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                    else -> {
                        Toast.makeText(context, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show()
                        onRegisterClick()
                    }
                }
            },
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        SecondaryButton(
            text = "Quay lại",
            onClick = onNavigateToWelcome,
=======
            text = "Register",
            onClick = {onRegisterClick(email, password, confirmPassword, "User")},
>>>>>>> feature/lan-with-android
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}
