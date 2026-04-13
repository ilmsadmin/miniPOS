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
    val PrimaryGlow: Color,

    val Secondary: Color,
    val SecondaryDark: Color,
    val SecondaryLight: Color,
    val SecondaryContainer: Color,

    val Accent: Color,
    val AccentDark: Color,
    val AccentLight: Color,
    val AccentContainer: Color,
    val AccentGlow: Color,

    val Error: Color,
    val ErrorDark: Color,
    val ErrorLight: Color,
    val ErrorContainer: Color,

    val Background: Color,
    val Surface: Color,
    val SurfaceVariant: Color,
    val SurfaceElevated: Color,
    val InputBackground: Color,

    val TextPrimary: Color,
    val TextSecondary: Color,
    val TextTertiary: Color,

    val Border: Color,
    val BorderLight: Color,
    val Divider: Color,

    val Success: Color,
    val SuccessSoft: Color,
    val Warning: Color,
    val WarningSoft: Color,
    val Info: Color,
    val InfoSoft: Color,
    val ErrorSoft: Color,

    val OverlayBg: Color,

    // Brand colors
    val BrandNavy: Color,
    val BrandBlue: Color,
    val BrandBlueLight: Color,

    // Category icon colors
    val IconDrinks: Color,
    val IconFood: Color,
    val IconSnacks: Color,
    val IconHousehold: Color,
    val IconDairy: Color,
)

val LightMiniPosColors = MiniPosColors(
    Primary = Color(0xFF0E9AA0),       // Teal — from logo center
    PrimaryDark = Color(0xFF0C8489),   // Darker teal
    PrimaryLight = Color(0xFF2EC4B6),  // Lighter teal-green
    PrimaryContainer = Color(0xFFE0F5F5), // Very light teal bg
    PrimaryGlow = Color(0x330E9AA0),

    Secondary = Color(0xFF059669),
    SecondaryDark = Color(0xFF047857),
    SecondaryLight = Color(0xFF6EE7B7),
    SecondaryContainer = Color(0xFFD1FAE5),

    Accent = Color(0xFF5AEDC5),        // Mint/cyan glow from logo ring
    AccentDark = Color(0xFF3DCBA8),
    AccentLight = Color(0xFF7FF5D6),
    AccentContainer = Color(0xFFE5FFF7),
    AccentGlow = Color(0x265AEDC5),

    Error = Color(0xFFE53935),
    ErrorDark = Color(0xFFC62828),
    ErrorLight = Color(0xFFEF5350),
    ErrorContainer = Color(0xFFFFEBEE),

    Background = Color(0xFFF5F9F9),    // Slight teal tint
    Surface = Color(0xFFFFFFFF),
    SurfaceVariant = Color(0xFFEDF3F3),
    SurfaceElevated = Color(0xFFEDF3F3),
    InputBackground = Color(0x0A000000),

    TextPrimary = Color(0xFF1A2B2E),
    TextSecondary = Color(0xFF5A6E72),
    TextTertiary = Color(0xFF8B9EA3),

    Border = Color(0x0F000000),
    BorderLight = Color(0x1A000000),
    Divider = Color(0x0D000000),

    Success = Color(0xFF00B862),
    SuccessSoft = Color(0x1A00B862),
    Warning = Color(0xFFE5A800),
    WarningSoft = Color(0x1AE5A800),
    Info = Color(0xFF0E9AA0),
    InfoSoft = Color(0x1A0E9AA0),
    ErrorSoft = Color(0x14E53935),

    OverlayBg = Color(0x59000000),

    BrandNavy = Color(0xFF0A3D40),     // Dark teal navy
    BrandBlue = Color(0xFF12AAA5),     // Main logo teal
    BrandBlueLight = Color(0xFF5AEDC5), // Logo glow ring

    IconDrinks = Color(0xFF1A9FC4),
    IconFood = Color(0xFFD84315),
    IconSnacks = Color(0xFFF9A825),
    IconHousehold = Color(0xFF388E3C),
    IconDairy = Color(0xFF8E24AA),
)

val DarkMiniPosColors = MiniPosColors(
    Primary = Color(0xFF14B8B0),       // Bright teal for dark mode
    PrimaryDark = Color(0xFF0E9AA0),
    PrimaryLight = Color(0xFF5AEDC5),  // Mint glow
    PrimaryContainer = Color(0xFF0A3038),
    PrimaryGlow = Color(0x6614B8B0),

    Secondary = Color(0xFF34D399),
    SecondaryDark = Color(0xFF6EE7B7),
    SecondaryLight = Color(0xFF047857),
    SecondaryContainer = Color(0xFF064E3B),

    Accent = Color(0xFF5AEDC5),        // Mint/cyan glow
    AccentDark = Color(0xFF3DCBA8),
    AccentLight = Color(0xFF7FF5D6),
    AccentContainer = Color(0xFF0D3B35),
    AccentGlow = Color(0x4D5AEDC5),

    Error = Color(0xFFFF5252),
    ErrorDark = Color(0xFFFF8A80),
    ErrorLight = Color(0xFFD32F2F),
    ErrorContainer = Color(0xFF4A1C1C),

    Background = Color(0xFF0A0E1A),
    Surface = Color(0xFF111827),
    SurfaceVariant = Color(0xFF1A2236),
    SurfaceElevated = Color(0xFF161D2F),
    InputBackground = Color(0x0FFFFFFF),

    TextPrimary = Color(0xFFF1F5F9),
    TextSecondary = Color(0xFF94A3B8),
    TextTertiary = Color(0xFF64748B),

    Border = Color(0x0FFFFFFF),
    BorderLight = Color(0x1AFFFFFF),
    Divider = Color(0x0AFFFFFF),

    Success = Color(0xFF00E676),
    SuccessSoft = Color(0x1F00E676),
    Warning = Color(0xFFFFD600),
    WarningSoft = Color(0x1FFFD600),
    Info = Color(0xFF14B8B0),
    InfoSoft = Color(0x1F14B8B0),
    ErrorSoft = Color(0x1FFF5252),

    OverlayBg = Color(0x99000000),

    BrandNavy = Color(0xFF0A3D40),
    BrandBlue = Color(0xFF14B8B0),
    BrandBlueLight = Color(0xFF5AEDC5),

    IconDrinks = Color(0xFF4BB8F0),
    IconFood = Color(0xFFFF8A65),
    IconSnacks = Color(0xFFFFD54F),
    IconHousehold = Color(0xFF81C784),
    IconDairy = Color(0xFFCE93D8),
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
    val PrimaryGlow: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.PrimaryGlow

    val Secondary: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Secondary
    val SecondaryDark: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.SecondaryDark
    val SecondaryLight: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.SecondaryLight
    val SecondaryContainer: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.SecondaryContainer

    val Accent: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Accent
    val AccentDark: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.AccentDark
    val AccentLight: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.AccentLight
    val AccentContainer: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.AccentContainer
    val AccentGlow: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.AccentGlow

    val Error: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Error
    val ErrorDark: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.ErrorDark
    val ErrorLight: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.ErrorLight
    val ErrorContainer: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.ErrorContainer

    val Background: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Background
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Surface
    val SurfaceVariant: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.SurfaceVariant
    val SurfaceElevated: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.SurfaceElevated
    val InputBackground: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.InputBackground

    val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.TextPrimary
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.TextSecondary
    val TextTertiary: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.TextTertiary

    val Border: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Border
    val BorderLight: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.BorderLight
    val Divider: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Divider

    val Success: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Success
    val SuccessSoft: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.SuccessSoft
    val Warning: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Warning
    val WarningSoft: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.WarningSoft
    val Info: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.Info
    val InfoSoft: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.InfoSoft
    val ErrorSoft: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.ErrorSoft

    val OverlayBg: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.OverlayBg

    val BrandNavy: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.BrandNavy
    val BrandBlue: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.BrandBlue
    val BrandBlueLight: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.BrandBlueLight

    val IconDrinks: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.IconDrinks
    val IconFood: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.IconFood
    val IconSnacks: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.IconSnacks
    val IconHousehold: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.IconHousehold
    val IconDairy: Color @Composable @ReadOnlyComposable get() = LocalMiniPosColors.current.IconDairy
}
