package com.minipos.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = LightMiniPosColors.Primary,
    onPrimary = Color.White,
    primaryContainer = LightMiniPosColors.PrimaryContainer,
    onPrimaryContainer = LightMiniPosColors.PrimaryDark,

    secondary = LightMiniPosColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = LightMiniPosColors.SecondaryContainer,
    onSecondaryContainer = LightMiniPosColors.SecondaryDark,

    tertiary = LightMiniPosColors.Accent,
    onTertiary = Color.White,
    tertiaryContainer = LightMiniPosColors.AccentContainer,
    onTertiaryContainer = LightMiniPosColors.AccentDark,

    error = LightMiniPosColors.Error,
    onError = Color.White,
    errorContainer = LightMiniPosColors.ErrorContainer,
    onErrorContainer = LightMiniPosColors.ErrorDark,

    background = LightMiniPosColors.Background,
    onBackground = LightMiniPosColors.TextPrimary,

    surface = LightMiniPosColors.Surface,
    onSurface = LightMiniPosColors.TextPrimary,
    surfaceVariant = LightMiniPosColors.SurfaceVariant,
    onSurfaceVariant = LightMiniPosColors.TextSecondary,

    outline = LightMiniPosColors.Border,
    outlineVariant = LightMiniPosColors.Divider,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkMiniPosColors.Primary,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = DarkMiniPosColors.PrimaryContainer,
    onPrimaryContainer = DarkMiniPosColors.PrimaryLight,

    secondary = DarkMiniPosColors.Secondary,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = DarkMiniPosColors.SecondaryContainer,
    onSecondaryContainer = DarkMiniPosColors.SecondaryLight,

    tertiary = DarkMiniPosColors.Accent,
    onTertiary = Color(0xFF0F172A),
    tertiaryContainer = DarkMiniPosColors.AccentContainer,
    onTertiaryContainer = DarkMiniPosColors.AccentLight,

    error = DarkMiniPosColors.Error,
    onError = Color(0xFF0F172A),
    errorContainer = DarkMiniPosColors.ErrorContainer,
    onErrorContainer = DarkMiniPosColors.ErrorLight,

    background = DarkMiniPosColors.Background,
    onBackground = DarkMiniPosColors.TextPrimary,

    surface = DarkMiniPosColors.Surface,
    onSurface = DarkMiniPosColors.TextPrimary,
    surfaceVariant = DarkMiniPosColors.SurfaceVariant,
    onSurfaceVariant = DarkMiniPosColors.TextSecondary,

    outline = DarkMiniPosColors.Border,
    outlineVariant = DarkMiniPosColors.Divider,
)

@Composable
fun MiniPosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val miniPosColors = if (darkTheme) DarkMiniPosColors else LightMiniPosColors

    // Update status bar and navigation bar colors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bgColor = if (darkTheme) {
                DarkMiniPosColors.Background.toArgb()
            } else {
                LightMiniPosColors.Background.toArgb()
            }
            window.statusBarColor = bgColor
            window.navigationBarColor = bgColor
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalMiniPosColors provides miniPosColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
