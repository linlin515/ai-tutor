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
import com.aitutor.app.data.local.OnboardingDataStore
import com.aitutor.app.data.remote.interceptor.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import com.aitutor.app.ui.theme.AiTutorTheme

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val onboardingDataStore: OnboardingDataStore
) : ViewModel() {

    private val _isLoggedIn = mutableStateOf(false)
    val isLoggedIn: State<Boolean> = _isLoggedIn

    private val _isOnboardingDone = mutableStateOf(true)
    val isOnboardingDone: State<Boolean> = _isOnboardingDone

    suspend fun checkStartupState() {
        delay(1500)
        _isLoggedIn.value = tokenManager.isLoggedIn()
        _isOnboardingDone.value = onboardingDataStore.isOnboardingDone.first()
    }
}

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit,
    onNavigateToOnboarding: () -> Unit = {}
) {
    val viewModel: SplashViewModel = hiltViewModel()
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

        // Run startup check concurrently with splash timer
        val startupCheck = launch {
            viewModel.checkStartupState()
        }

        // Wait for minimum splash duration
        delay(2500)

        // Ensure startup check has completed (usually finishes at 1500ms)
        startupCheck.join()

        if (!navigationHandled) {
            navigationHandled = true
            if (!viewModel.isOnboardingDone.value) {
                onNavigateToOnboarding()
            } else if (viewModel.isLoggedIn.value) {
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
                text = "\uD83D\uDCDA",
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

// ===== Preview =====
@Preview(name = "启动屏 预览", showBackground = true, backgroundColor = 0xFF1C1B1F, showSystemUi = false, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "启动屏 预览 (深色)", showBackground = true, backgroundColor = 0xFFFEFBFF, showSystemUi = false, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSplashScreen() { AiTutorTheme { SplashScreen(onNavigateToLogin = {}, onNavigateToMain = {}) } }
