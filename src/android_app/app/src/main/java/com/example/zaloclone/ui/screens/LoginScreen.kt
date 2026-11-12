package com.example.zaloclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zaloclone.ui.components.PrimaryButton
import com.example.zaloclone.ui.components.SecondaryButton
import com.example.zaloclone.ui.components.ZaloTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.zaloclone.ui.theme.TextGray
import com.example.zaloclone.ui.theme.ZaloCyan

@Composable
<<<<<<< HEAD
fun LoginScreen(
    onLoginClick: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateToWelcome: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
=======
fun LoginScreen(onLoginClick: (String, String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
>>>>>>> feature/lan-with-android
    var password by remember { mutableStateOf("") }

    // Kotlin, API connect backend

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to Zalo",
            fontSize = 32.sp,
            color = ZaloCyan,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 40.dp)
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
            placeholder = "password",
>>>>>>> feature/lan-with-android
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(30.dp))

        PrimaryButton(text = "Login", onClick = {onLoginClick(email, password, "User")})

        Spacer(modifier = Modifier.height(16.dp))

<<<<<<< HEAD
        SecondaryButton(text = "Quay lại", onClick = onNavigateToWelcome)

        Spacer(modifier = Modifier.height(20.dp))

        TextButton(onClick = onNavigateToForgotPassword) {
            Text(text = "Quên mật khẩu", color = TextGray, fontSize = 16.sp)
=======
        TextButton(onClick = { /* Handle Forgot Password */ }) {
            Text(text = "Forgot password", color = TextGray, fontSize = 16.sp)
>>>>>>> feature/lan-with-android
        }
    }
}
