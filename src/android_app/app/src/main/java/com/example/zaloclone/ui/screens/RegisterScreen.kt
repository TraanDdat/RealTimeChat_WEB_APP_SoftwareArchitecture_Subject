package com.example.zaloclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zaloclone.ui.components.PrimaryButton
import com.example.zaloclone.ui.components.ZaloTextField
import com.example.zaloclone.ui.theme.ZaloCyan

@Composable
fun RegisterScreen(onRegisterClick: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
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
            text = "Đăng ký",
            fontSize = 32.sp,
            color = ZaloCyan,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 30.dp)
        )

        ZaloTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Tên"
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
        
        ZaloTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            placeholder = "Nhập lại mật khẩu"
        )

        Spacer(modifier = Modifier.height(30.dp))

        PrimaryButton(
            text = "Đăng ký", 
            onClick = onRegisterClick,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}
