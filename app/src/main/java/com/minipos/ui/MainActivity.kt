package com.minipos.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
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
import com.minipos.ui.navigation.MiniPosNavGraph
import com.minipos.ui.navigation.Screen
import com.minipos.ui.onboarding.CreateStoreScreen
import com.minipos.ui.onboarding.JoinStoreScreen
import com.minipos.ui.onboarding.OnboardingScreen
import com.minipos.ui.pinlock.PinLockScreen
import com.minipos.ui.splash.SplashScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Keep native splash until our Compose splash is ready
        splash.setKeepOnScreenCondition { false }
        setContent {
            MiniPosTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
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

    when (appState) {
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
                    )
                }
            }
        }
        AppState.Locked -> {
            PinLockScreen(onUnlocked = { viewModel.unlock() })
        }
        AppState.Home -> {
            MiniPosNavGraph(
                navController = navController,
                startDestination = Screen.Home.route,
            )
        }
    }
}
