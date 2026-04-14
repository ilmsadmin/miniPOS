package com.minipos.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.minipos.core.theme.MiniPosTheme
import com.minipos.core.theme.ThemeManager
import com.minipos.core.theme.ThemeMode
import com.minipos.ui.navigation.MiniPosNavGraph
import com.minipos.ui.navigation.Screen
import com.minipos.ui.onboarding.CreateStoreScreen
import com.minipos.ui.onboarding.JoinStoreScreen
import com.minipos.ui.onboarding.OnboardingScreen
import com.minipos.ui.login.LoginScreen
import com.minipos.ui.pinlock.PinLockScreen
import com.minipos.ui.splash.SplashScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install system splash — exits immediately, just shows dark bg
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by themeManager.themeMode.collectAsState()
            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            MiniPosTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MiniPosApp()
                }
            }
        }
    }
}

@Composable
fun MiniPosApp() {
    val viewModel: MainViewModel = hiltViewModel()
    val appState by viewModel.appState.collectAsState()
    val navController = rememberNavController()

    AnimatedContent(
        targetState = appState,
        transitionSpec = {
            when {
                // Home → Login (Switch User): slide in từ trái, giống popBack
                initialState == AppState.Home && targetState == AppState.Login -> {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 10 }) togetherWith
                        (fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { it / 10 })
                }
                // Login → Home (sau đăng nhập): slide in từ phải
                initialState == AppState.Login && targetState == AppState.Home -> {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 10 }) togetherWith
                        (fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { -it / 10 })
                }
                // Các chuyển đổi khác (Splash, Onboarding...): fade đơn giản
                else -> {
                    fadeIn(tween(350, easing = LinearOutSlowInEasing)) togetherWith
                        fadeOut(tween(250, easing = FastOutLinearInEasing))
                }
            }
        },
        label = "app_state_transition",
    ) { state ->
        when (state) {
            AppState.Splash -> {
                SplashScreen(
                    onSplashFinished = { viewModel.onSplashFinished() },
                )
            }
            AppState.Onboarding -> {
                val onboardingNavController = rememberNavController()
                NavHost(
                    navController = onboardingNavController,
                    startDestination = Screen.Onboarding.route,
                ) {
                    composable(Screen.Onboarding.route) {
                        OnboardingScreen(
                            onCreateStore = {
                                onboardingNavController.navigate(Screen.CreateStore.route)
                            },
                            onJoinStore = {
                                onboardingNavController.navigate(Screen.JoinStore.route)
                            },
                        )
                    }
                    composable(Screen.CreateStore.route) {
                        CreateStoreScreen(
                            onStoreCreated = { viewModel.onOnboardingComplete() },
                            onBack = { onboardingNavController.popBackStack() },
                        )
                    }
                    composable(Screen.JoinStore.route) {
                        JoinStoreScreen(
                            onBack = { onboardingNavController.popBackStack() },
                            onJoinComplete = { viewModel.onOnboardingComplete() },
                        )
                    }
                }
            }
            AppState.Locked -> {
                PinLockScreen(onUnlocked = { viewModel.unlock() })
            }
            AppState.Login -> {
                val canGoBack by viewModel.loginCanGoBack.collectAsState()
                LoginScreen(
                    onLoginSuccess = { viewModel.onLoginSuccess() },
                    onBack = if (canGoBack) ({ viewModel.cancelSwitchUser() }) else null,
                )
            }
            AppState.Home -> {
                MiniPosNavGraph(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    onLogout = { viewModel.logout() },
                    onSwitchUser = { viewModel.switchUser() },
                )
            }
        }
    }
}
