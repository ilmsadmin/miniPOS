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

    // Wait until initial state is resolved
    if (appState == AppState.Loading) return

    // Remember the initial start destination so NavHost is only created once
    val startDestination = remember {
        when (appState) {
            AppState.Loading -> Screen.Onboarding.route // fallback, won't reach here
            AppState.Onboarding -> Screen.Onboarding.route
            AppState.Login -> Screen.Login.route
            AppState.Home -> Screen.Home.route
        }
    }

    // React to appState changes AFTER initial composition by navigating
    var previousState by remember { mutableStateOf(appState) }
    LaunchedEffect(appState) {
        if (appState != previousState) {
            previousState = appState
            val targetRoute = when (appState) {
                AppState.Loading -> return@LaunchedEffect
                AppState.Onboarding -> Screen.Onboarding.route
                AppState.Login -> Screen.Login.route
                AppState.Home -> Screen.Home.route
            }
            navController.navigate(targetRoute) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    MiniPosNavGraph(
        navController = navController,
        startDestination = startDestination,
    )
}
