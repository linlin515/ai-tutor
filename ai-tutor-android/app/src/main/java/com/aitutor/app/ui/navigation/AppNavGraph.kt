package com.aitutor.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aitutor.app.ui.camera.CameraScreen
import com.aitutor.app.ui.chat.ChatScreen
import com.aitutor.app.ui.auth.LoginScreen
import com.aitutor.app.ui.auth.LoginViewModel
import com.aitutor.app.ui.profile.ProfileScreen
import com.aitutor.app.ui.settings.SettingsScreen
import com.aitutor.app.ui.splash.SplashScreen

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem("对话", Icons.Default.Chat, Routes.CHAT),
        BottomNavItem("解题", Icons.Default.CameraAlt, Routes.SOLVE),
        BottomNavItem("设置", Icons.Default.Settings, Routes.SETTINGS)
    )

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onNavigateToLogin = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateToMain = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.LOGIN) {
                val viewModel: LoginViewModel = hiltViewModel()
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.MAIN) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.CHAT) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
                // Placeholder while redirecting
                Box(modifier = Modifier.fillMaxSize())
            }

            composable(Routes.CHAT) {
                ChatScreen(
                    onNavigateToCamera = {
                        navController.navigate(Routes.SOLVE)
                    },
                    onNavigateToProfile = {
                        navController.navigate(Routes.PROFILE)
                    }
                )
            }

            composable(
                route = Routes.CHAT_CONVERSATION,
                arguments = listOf(navArgument("conversationId") { type = NavType.LongType })
            ) { backStackEntry ->
                val conversationId = backStackEntry.arguments?.getLong("conversationId") ?: 0L
                ChatScreen(
                    conversationId = conversationId,
                    onNavigateToCamera = {
                        navController.navigate(Routes.SOLVE)
                    },
                    onNavigateToProfile = {
                        navController.navigate(Routes.PROFILE)
                    }
                )
            }

            composable(Routes.SOLVE) {
                CameraScreen(
                    onNavigateToChat = { result ->
                        navController.navigate(Routes.CHAT) {
                            popUpTo(Routes.CHAT)
                        }
                    }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onNavigateToProfile = {
                        navController.navigate(Routes.PROFILE)
                    },
                    onNavigateToSubscription = {
                        navController.navigate(Routes.SUBSCRIPTION)
                    }
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SUBSCRIPTION) {
                SubscriptionPlaceholder(onBack = { navController.popBackStack() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPlaceholder(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("订阅管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("订阅管理页面", style = MaterialTheme.typography.titleLarge)
        }
    }
}
