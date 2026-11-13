package com.example.zaloclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zaloclone.ui.screens.ChatListScreen
import com.example.zaloclone.ui.screens.ChattingScreen
import com.example.zaloclone.ui.screens.LoginScreen
import com.example.zaloclone.ui.theme.ZaloCyan

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = ZaloCyan)) {
                val model: ChatViewModel = viewModel()
                val state by model.state.collectAsStateWithLifecycle()
                val snackbar = remember { SnackbarHostState() }
                val lifecycleOwner = LocalLifecycleOwner.current

                LaunchedEffect(state.error) {
                    state.error?.let {
                        snackbar.showSnackbar(it)
                        model.dismissError()
                    }
                }
                DisposableEffect(lifecycleOwner, model) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START -> model.startRealtime()
                            Lifecycle.Event.ON_STOP -> model.stopRealtime()
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        model.stopRealtime()
                    }
                }
                BackHandler(enabled = state.selectedUser != null) { model.selectUser(null) }
                Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                        val user = state.user
                        when {
                            state.checkingSession -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                            user == null -> LoginScreen(
                                busy = state.authenticating,
                                onSubmit = model::authenticate,
                                onRetrySession = model::restoreSession
                            )
                            state.selectedUser == null -> ChatListScreen(
                                state = state,
                                onSelect = model::selectUser,
                                onRefresh = model::refreshUsers,
                                onLogout = model::logout
                            )
                            else -> ChattingScreen(
                                state = state,
                                currentUserId = user.id,
                                onBack = { model.selectUser(null) },
                                onRefresh = model::refreshHistory,
                                onSend = model::sendMessage
                            )
                        }
                    }
                }
            }
        }
    }
}