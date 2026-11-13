package com.example.zaloclone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    busy: Boolean,
    onSubmit: (String, String, String?) -> Unit,
    onRetrySession: () -> Unit
) {
    var creatingAccount by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val mismatch = creatingAccount && confirmation.isNotEmpty() && password != confirmation
    val valid = email.isNotBlank() && password.isNotBlank() &&
        (!creatingAccount || (username.isNotBlank() && password == confirmation))

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("CloneZalo", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text(if (creatingAccount) "Create account" else "Sign in", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        if (creatingAccount) {
            OutlinedTextField(
                value = username, onValueChange = { username = it }, label = { Text("Username") },
                enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        if (creatingAccount) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmation, onValueChange = { confirmation = it }, label = { Text("Confirm password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = mismatch,
                supportingText = { if (mismatch) Text("Passwords do not match.") },
                enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onSubmit(email, password, if (creatingAccount) username else null) },
            enabled = valid && !busy, modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (creatingAccount) "Create account" else "Sign in")
        }
        if (busy) CircularProgressIndicator(Modifier.padding(12.dp))
        TextButton(onClick = { creatingAccount = !creatingAccount }, enabled = !busy) {
            Text(if (creatingAccount) "Already have an account? Sign in" else "Create account")
        }
        TextButton(onClick = onRetrySession, enabled = !busy) { Text("Retry connection") }
    }
}