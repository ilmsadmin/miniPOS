package com.minipos.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minipos.core.theme.AppColors

// ═══════════════════════════════════════
// DESIGN TOKENS
// ═══════════════════════════════════════

object MiniPosTokens {
    val RadiusSm = 8.dp
    val RadiusMd = 12.dp
    val RadiusLg = 16.dp
    val RadiusXl = 20.dp
    val Radius2xl = 24.dp
    val RadiusFull = 999.dp
}

// ═══════════════════════════════════════
// GRADIENT HELPERS
// ═══════════════════════════════════════

object MiniPosGradients {
    @Composable
    fun primary() = Brush.linearGradient(
        colors = listOf(AppColors.Primary, AppColors.PrimaryLight)
    )

    @Composable
    fun accent() = Brush.linearGradient(
        colors = listOf(AppColors.Accent, AppColors.Primary)
    )

    @Composable
    fun fab() = Brush.linearGradient(
        colors = listOf(AppColors.Primary, AppColors.Accent, AppColors.PrimaryLight)
    )

    @Composable
    fun price() = Brush.linearGradient(
        colors = listOf(AppColors.Accent, AppColors.PrimaryLight)
    )

    @Composable
    fun brand() = Brush.linearGradient(
        colors = listOf(AppColors.BrandNavy, AppColors.BrandBlue, AppColors.BrandBlueLight)
    )
}

// ═══════════════════════════════════════
// TOP BAR
// ═══════════════════════════════════════

@Composable
fun MiniPosTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = AppColors.TextSecondary,
                )
            }
        }
        Spacer(Modifier.width(if (onBack != null) 4.dp else 16.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

// ═══════════════════════════════════════
// SEARCH BAR
// ═══════════════════════════════════════

@Composable
fun MiniPosSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Tìm kiếm...",
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                AppColors.InputBackground,
                RoundedCornerShape(MiniPosTokens.Radius2xl)
            )
            .border(
                1.dp,
                AppColors.Border,
                RoundedCornerShape(MiniPosTokens.Radius2xl)
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = AppColors.TextTertiary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = AppColors.TextTertiary,
                    fontSize = 15.sp,
                )
            }
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = AppColors.TextPrimary,
                    fontSize = 15.sp,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            trailingIcon()
        }
    }
}

// ═══════════════════════════════════════
// FILTER CHIPS
// ═══════════════════════════════════════

@Composable
fun MiniPosFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    count: Int? = null,
) {
    val bgColor = if (selected) AppColors.Primary else Color.Transparent
    val textColor = if (selected) Color.White else AppColors.TextSecondary
    val borderColor = if (selected) AppColors.Primary else AppColors.BorderLight

    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(MiniPosTokens.RadiusFull))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        if (icon != null && !selected) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .background(
                        if (selected) Color.White.copy(alpha = 0.2f) else AppColors.InputBackground,
                        RoundedCornerShape(MiniPosTokens.RadiusFull)
                    )
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                )
            }
        }
    }
}

// ═══════════════════════════════════════
// GRADIENT BUTTON
// ═══════════════════════════════════════

@Composable
fun MiniPosGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 54.dp,
    gradient: Brush? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(MiniPosTokens.Radius2xl))
            .background(
                if (enabled) (gradient ?: MiniPosGradients.primary())
                else Brush.linearGradient(listOf(AppColors.TextTertiary, AppColors.TextTertiary))
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// ═══════════════════════════════════════
// STAT CARD
// ═══════════════════════════════════════

@Composable
fun MiniPosStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color = AppColors.Primary,
    trend: String? = null,
    trendUp: Boolean = true,
) {
    Column(
        modifier = modifier
            .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null || trend != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
                }
                if (trend != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (trendUp) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                            null,
                            tint = if (trendUp) AppColors.Success else AppColors.Error,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = trend,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (trendUp) AppColors.Success else AppColors.Error,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = AppColors.TextPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextTertiary,
        )
    }
}

// ═══════════════════════════════════════
// CATEGORY ICON HELPER
// ═══════════════════════════════════════

@Composable
fun CategoryIconColors(category: String): Pair<Color, Color> {
    return when (category.lowercase()) {
        "drinks", "nước uống", "đồ uống" -> Color(0xFF4BB8F0) to Color(0xFF2196F3)
        "food", "thực phẩm" -> Color(0xFFFF8A65) to Color(0xFFF44336)
        "snacks", "bánh kẹo", "snack" -> Color(0xFFFFD54F) to Color(0xFFFFB300)
        "household", "gia dụng", "đồ dùng" -> Color(0xFF81C784) to Color(0xFF388E3C)
        "dairy", "sữa" -> Color(0xFFCE93D8) to Color(0xFF8E24AA)
        else -> Color(0xFF90A4AE) to Color(0xFF546E7A)
    }
}

@Composable
fun CategoryIconBox(
    category: String,
    icon: ImageVector,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
) {
    val (startColor, endColor) = CategoryIconColors(category)
    Box(
        modifier = Modifier
            .size(size)
            .background(
                Brush.linearGradient(listOf(startColor, endColor)),
                RoundedCornerShape(MiniPosTokens.RadiusMd)
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}

// ═══════════════════════════════════════
// TOAST NOTIFICATION
// ═══════════════════════════════════════

@Composable
fun MiniPosToast(
    message: String,
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 40.dp, vertical = 16.dp)
                .background(
                    AppColors.SurfaceElevated,
                    RoundedCornerShape(MiniPosTokens.RadiusLg)
                )
                .border(1.dp, AppColors.BorderLight, RoundedCornerShape(MiniPosTokens.RadiusLg))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(AppColors.SuccessSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Check,
                    null,
                    tint = AppColors.Success,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
            )
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(1400)
            onDismiss()
        }
    }
}

// ═══════════════════════════════════════
// STOCK STATUS BADGE
// ═══════════════════════════════════════

@Composable
fun StockBadge(
    stock: Int,
    lowThreshold: Int = 10,
) {
    val (text, bgColor, textColor) = when {
        stock <= 0 -> Triple("Hết hàng", AppColors.Error.copy(alpha = 0.12f), AppColors.Error)
        stock <= lowThreshold -> Triple("Còn $stock", AppColors.Warning.copy(alpha = 0.12f), AppColors.Warning)
        else -> Triple("Kho: $stock", AppColors.SuccessSoft, AppColors.Success)
    }
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        color = textColor,
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(MiniPosTokens.RadiusFull))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

// ═══════════════════════════════════════
// SECTION TITLE
// ═══════════════════════════════════════

@Composable
fun SectionTitle(
    title: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = AppColors.TextTertiary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title.uppercase(),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.TextTertiary,
            letterSpacing = 0.8.sp,
        )
    }
}
