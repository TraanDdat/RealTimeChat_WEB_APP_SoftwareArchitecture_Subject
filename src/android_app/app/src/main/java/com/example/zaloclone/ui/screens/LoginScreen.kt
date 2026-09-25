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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zaloclone.ui.components.PrimaryButton
import com.example.zaloclone.ui.components.ZaloTextField
import com.example.zaloclone.ui.theme.TextGray
import com.example.zaloclone.ui.theme.ZaloCyan

@Composable
fun LoginScreen(onLoginClick: () -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Đăng nhập Zalo",
            fontSize = 32.sp,
            color = ZaloCyan,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 40.dp)
        )

        ZaloTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            placeholder = "Số điện thoại"
        )
        
        ZaloTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Mật khẩu"
        )

        Spacer(modifier = Modifier.height(30.dp))

        PrimaryButton(text = "Đăng nhập", onClick = onLoginClick)

        Spacer(modifier = Modifier.height(30.dp))

        TextButton(onClick = { /* Handle Forgot Password */ }) {
            Text(text = "Quên mật khẩu", color = TextGray, fontSize = 16.sp)
        }
    }
}
