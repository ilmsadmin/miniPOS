package com.minipos.core.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AppColors.Primary,
    onPrimary = Color.White,
    primaryContainer = AppColors.PrimaryContainer,
    onPrimaryContainer = AppColors.PrimaryDark,

    secondary = AppColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = AppColors.SecondaryContainer,
    onSecondaryContainer = AppColors.SecondaryDark,

    tertiary = AppColors.Accent,
    onTertiary = Color.White,
    tertiaryContainer = AppColors.AccentContainer,
    onTertiaryContainer = AppColors.AccentDark,

    error = AppColors.Error,
    onError = Color.White,
    errorContainer = AppColors.ErrorContainer,
    onErrorContainer = AppColors.ErrorDark,

    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,

    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.SurfaceVariant,
    onSurfaceVariant = AppColors.TextSecondary,

    outline = AppColors.Border,
    outlineVariant = AppColors.Divider,
)

@Composable
fun MiniPosTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
