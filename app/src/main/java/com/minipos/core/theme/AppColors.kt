package com.minipos.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * App color palette that supports light and dark themes.
 * Access colors via `AppColors.Primary`, `AppColors.Surface`, etc.
 * The actual values are resolved at runtime based on the current theme.
 */
data class MiniPosColors(
    val Primary: Color,
    val PrimaryDark: Color,
    val PrimaryLight: Color,
    val PrimaryContainer: Color,

    val Secondary: Color,
    val SecondaryDark: Color,
    val SecondaryLight: Color,
    val SecondaryContainer: Color,

    val Accent: Color,
    val AccentDark: Color,
    val AccentLight: Color,
    val AccentContainer: Color,

    val Error: Color,
    val ErrorDark: Color,
    val ErrorLight: Color,
    val ErrorContainer: Color,

    val Background: Color,
    val Surface: Color,
    val SurfaceVariant: Color,

    val TextPrimary: Color,
    val TextSecondary: Color,
    val TextTertiary: Color,

    val Border: Color,
    val Divider: Color,

    val Success: Color,
    val Warning: Color,
    val Info: Color,
)

val LightMiniPosColors = MiniPosColors(
    Primary = Color(0xFF2563EB),
    PrimaryDark = Color(0xFF1D4ED8),
    PrimaryLight = Color(0xFF93C5FD),
    PrimaryContainer = Color(0xFFDBEAFE),

    Secondary = Color(0xFF059669),
    SecondaryDark = Color(0xFF047857),
    SecondaryLight = Color(0xFF6EE7B7),
    SecondaryContainer = Color(0xFFD1FAE5),

    Accent = Color(0xFFF59E0B),
    AccentDark = Color(0xFFD97706),
    AccentLight = Color(0xFFFDE68A),
    AccentContainer = Color(0xFFFEF3C7),

    Error = Color(0xFFDC2626),
    ErrorDark = Color(0xFFB91C1C),
    ErrorLight = Color(0xFFFCA5A5),
    ErrorContainer = Color(0xFFFEE2E2),

    Background = Color(0xFFF8FAFC),
    Surface = Color(0xFFFFFFFF),
    SurfaceVariant = Color(0xFFF1F5F9),

    TextPrimary = Color(0xFF1E293B),
    TextSecondary = Color(0xFF64748B),
    TextTertiary = Color(0xFF94A3B8),

    Border = Color(0xFFE2E8F0),
    Divider = Color(0xFFF1F5F9),

    Success = Color(0xFF059669),
    Warning = Color(0xFFF59E0B),
    Info = Color(0xFF2563EB),
)

val DarkMiniPosColors = MiniPosColors(
    Primary = Color(0xFF60A5FA),
    PrimaryDark = Color(0xFF93C5FD),
    PrimaryLight = Color(0xFF1D4ED8),
    PrimaryContainer = Color(0xFF1E3A5F),

    Secondary = Color(0xFF34D399),
    SecondaryDark = Color(0xFF6EE7B7),
    SecondaryLight = Color(0xFF047857),
    SecondaryContainer = Color(0xFF064E3B),

    Accent = Color(0xFFFBBF24),
    AccentDark = Color(0xFFFDE68A),
    AccentLight = Color(0xFF92400E),
    AccentContainer = Color(0xFF78350F),

    Error = Color(0xFFF87171),
    ErrorDark = Color(0xFFFCA5A5),
    ErrorLight = Color(0xFF991B1B),
    ErrorContainer = Color(0xFF7F1D1D),

    Background = Color(0xFF0F172A),
    Surface = Color(0xFF1E293B),
    SurfaceVariant = Color(0xFF334155),

    TextPrimary = Color(0xFFF1F5F9),
    TextSecondary = Color(0xFF94A3B8),
    TextTertiary = Color(0xFF64748B),

    Border = Color(0xFF475569),
    Divider = Color(0xFF334155),

    Success = Color(0xFF34D399),
    Warning = Color(0xFFFBBF24),
    Info = Color(0xFF60A5FA),
)

val LocalMiniPosColors = staticCompositionLocalOf { LightMiniPosColors }

/**
 * Global accessor for app colors.
 * Works like a static object but resolves light/dark dynamically.
 */
object AppColors {
    val Primary: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Primary
    val PrimaryDark: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.PrimaryDark
    val PrimaryLight: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.PrimaryLight
    val PrimaryContainer: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.PrimaryContainer

    val Secondary: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Secondary
    val SecondaryDark: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.SecondaryDark
    val SecondaryLight: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.SecondaryLight
    val SecondaryContainer: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.SecondaryContainer

    val Accent: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Accent
    val AccentDark: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.AccentDark
    val AccentLight: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.AccentLight
    val AccentContainer: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.AccentContainer

    val Error: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Error
    val ErrorDark: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.ErrorDark
    val ErrorLight: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.ErrorLight
    val ErrorContainer: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.ErrorContainer

    val Background: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Background
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Surface
    val SurfaceVariant: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.SurfaceVariant

    val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.TextPrimary
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.TextSecondary
    val TextTertiary: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.TextTertiary

    val Border: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Border
    val Divider: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Divider

    val Success: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Success
    val Warning: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Warning
    val Info: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Info
}
