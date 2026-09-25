package com.example.zaloclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zaloclone.ui.components.PrimaryButton
import com.example.zaloclone.ui.components.SecondaryButton
import com.example.zaloclone.ui.theme.TextGray
import com.example.zaloclone.ui.theme.ZaloCyan

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ZALO",
            fontSize = 80.sp,
            color = ZaloCyan,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 80.dp)
        )

        PrimaryButton(text = "ĐĂNG NHẬP", onClick = onNavigateToLogin)
        
        Spacer(modifier = Modifier.height(20.dp))
        
        SecondaryButton(text = "ĐĂNG KÝ", onClick = onNavigateToRegister)
        
        Spacer(modifier = Modifier.height(40.dp))
        
        TextButton(onClick = { /* Handle Forgot Password */ }) {
            Text(text = "Quên mật khẩu", color = TextGray, fontSize = 16.sp)
        }
    }
}
