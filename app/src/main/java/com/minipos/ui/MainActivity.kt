package com.minipos.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.minipos.core.theme.MiniPosTheme
import com.minipos.ui.navigation.MiniPosNavGraph
import com.minipos.ui.navigation.Screen
import com.minipos.ui.pinlock.PinLockScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiniPosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
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
        AppState.Loading -> return
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
