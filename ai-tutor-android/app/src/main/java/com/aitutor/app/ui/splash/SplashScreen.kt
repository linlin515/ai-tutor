package com.aitutor.app.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.app.data.remote.interceptor.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _isLoggedIn = mutableStateOf(false)
    val isLoggedIn: State<Boolean> = _isLoggedIn

    fun checkAuth() {
        viewModelScope.launch {
            delay(1500)
            _isLoggedIn.value = tokenManager.isLoggedIn()
        }
    }
}

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    val viewModel: SplashViewModel = hiltViewModel()
    val isLoggedIn by viewModel.isLoggedIn
    var startAnimation by remember { mutableStateOf(false) }
    var navigationHandled by remember { mutableStateOf(false) }

    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "alpha"
    )

    // Single LaunchedEffect for the entire splash flow
    LaunchedEffect(Unit) {
        startAnimation = true
        viewModel.checkAuth()

        // Wait for auth check + minimum splash duration
        delay(2500)

        if (!navigationHandled) {
            navigationHandled = true
            if (isLoggedIn) {
                onNavigateToMain()
            } else {
                onNavigateToLogin()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alphaAnim.value)
        ) {
            Text(
                text = "📚",
                fontSize = 72.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "AI 学习助手",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "随时随地，智能解答",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
