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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import java.text.NumberFormat
import java.util.Locale

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
    placeholder: String = "Search…",
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
        stock <= 0 -> Triple(stringResource(R.string.badge_out_of_stock), AppColors.Error.copy(alpha = 0.12f), AppColors.Error)
        stock <= lowThreshold -> Triple(stringResource(R.string.badge_remaining_stock, stock), AppColors.Warning.copy(alpha = 0.12f), AppColors.Warning)
        else -> Triple(stringResource(R.string.badge_stock_count, stock), AppColors.SuccessSoft, AppColors.Success)
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

// ═══════════════════════════════════════════════════════════════
// CURRENCY INPUT FIELD
// Tap to open a numpad bottom sheet with thousand-separator display.
// rawValue   — chỉ chứa chữ số (e.g. "150000")
// onRawValue — callback nhận chuỗi chữ số mới
// label      — nhãn hiển thị phía trên (optional, truyền "" để ẩn)
// suffix     — đơn vị hiển thị sau số (mặc định "đ")
// accentColor — màu text số (mặc định AppColors.Accent)
// ═══════════════════════════════════════════════════════════════

private val vnNumberFormat = NumberFormat.getNumberInstance(Locale("vi", "VN"))

/** Format raw digit string with thousand-separator, e.g. "150000" → "150.000" */
fun formatWithSeparator(raw: String): String {
    if (raw.isEmpty()) return ""
    return if (raw.contains('.')) {
        val parts = raw.split('.')
        val intPart = parts[0].toLongOrNull() ?: return raw
        val decPart = parts.getOrElse(1) { "" }
        "${vnNumberFormat.format(intPart)}.$decPart"
    } else {
        val num = raw.toLongOrNull() ?: return raw
        vnNumberFormat.format(num)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyInputField(
    rawValue: String,          // only digits, e.g. "150000"
    onRawValue: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "0",
    suffix: String = "đ",
    accentColor: Color = AppColors.Accent,
    /** true = price (integer, no decimal), false = quantity (allows single decimal) */
    isCurrency: Boolean = true,
    maxDigits: Int = 13,
) {
    var showNumpad by remember { mutableStateOf(false) }

    // Display text: formatted raw  +  suffix (if not empty)
    val displayText = remember(rawValue) {
        if (rawValue.isEmpty()) "" else "${formatWithSeparator(rawValue)}$suffix"
    }

    // Tap area — styled like FormInput
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.InputBackground)
            .clickable { showNumpad = true }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (rawValue.isEmpty()) {
            Text(placeholder, fontSize = 14.sp, color = AppColors.TextTertiary)
        } else {
            Text(
                displayText,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
            )
        }
        // Calculator icon on the right
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(28.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusSm))
                .background(accentColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Calculate,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp),
            )
        }
    }

    if (showNumpad) {
        CurrencyNumpadSheet(
            rawValue = rawValue,
            onRawValue = onRawValue,
            label = label,
            suffix = suffix,
            accentColor = accentColor,
            isCurrency = isCurrency,
            maxDigits = maxDigits,
            onDismiss = { showNumpad = false },
        )
    }
}

// ─── Numpad bottom sheet ───────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyNumpadSheet(
    rawValue: String,
    onRawValue: (String) -> Unit,
    label: String,
    suffix: String,
    accentColor: Color,
    isCurrency: Boolean,
    maxDigits: Int,
    onDismiss: () -> Unit,
) {
    // Local editable copy while sheet is open
    var localRaw by remember { mutableStateOf(rawValue) }

    fun appendKey(key: String) {
        when (key) {
            "del" -> {
                localRaw = localRaw.dropLast(1)
            }
            "." -> {
                if (!isCurrency && !localRaw.contains('.') && localRaw.isNotEmpty()) {
                    localRaw += "."
                }
            }
            else -> {
                // Prevent leading zeros (e.g. "000" when raw is empty → keep "0")
                if (localRaw.isEmpty() && key == "0") {
                    // skip — no leading zero for currency
                    return
                }
                if (localRaw.isEmpty() && key in listOf("00", "000")) return
                val stripped = key.trimStart('0')
                val toAdd = if (stripped.isEmpty() && localRaw.isNotEmpty()) key else stripped.ifEmpty { "0" }
                if ((localRaw + toAdd).replace(".", "").length <= maxDigits) {
                    localRaw += if (key.all { it == '0' }) key else toAdd
                }
            }
        }
        // Trim excessive leading zeros (keep "0" but not "00...")
        if (localRaw.length > 1 && localRaw.startsWith("0") && !localRaw.startsWith("0.")) {
            localRaw = localRaw.trimStart('0').ifEmpty { "" }
        }
        onRawValue(localRaw)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
                    .background(AppColors.TextTertiary.copy(alpha = 0.3f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Label
            if (label.isNotEmpty()) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp, top = 4.dp),
                )
            }

            // Amount display area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
                    .background(
                        Brush.linearGradient(
                            listOf(accentColor.copy(alpha = 0.06f), accentColor.copy(alpha = 0.02f))
                        )
                    )
                    .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(MiniPosTokens.RadiusXl))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (localRaw.isEmpty()) {
                        Text(
                            "0$suffix",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = AppColors.TextTertiary,
                        )
                    } else {
                        Text(
                            text = "${formatWithSeparator(localRaw)}$suffix",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                        )
                    }
                    // Blinking cursor indicator
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(start = 3.dp)
                                .width(2.dp)
                                .height(36.dp)
                                .background(accentColor),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Numpad grid: 3 columns
            // Row 1: 1 2 3
            // Row 2: 4 5 6
            // Row 3: 7 8 9
            // Row 4: 000  0  ⌫
            // Row 5 (if not currency): 00  .  Done  (optional)
            val numRows: List<List<String>> = if (isCurrency) {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("000", "0", "del"),
                )
            } else {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("00", "0", "del"),
                    listOf(".", "", "done"),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                numRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "" -> Spacer(Modifier.weight(1f))
                                "done" -> {
                                    // Done button
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(AppColors.Primary, AppColors.PrimaryLight)
                                                )
                                            )
                                            .clickable { onDismiss() },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                                "del" -> {
                                    // Delete key
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                                            .background(AppColors.Error.copy(alpha = 0.08f))
                                            .border(
                                                1.dp,
                                                AppColors.Error.copy(alpha = 0.2f),
                                                RoundedCornerShape(MiniPosTokens.RadiusLg),
                                            )
                                            .clickable { appendKey("del") },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Rounded.Backspace,
                                            contentDescription = null,
                                            tint = AppColors.Error,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                                else -> {
                                    // Normal digit / 00 / 000 / .
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                                            .background(AppColors.SurfaceVariant)
                                            .border(
                                                1.dp,
                                                AppColors.Border,
                                                RoundedCornerShape(MiniPosTokens.RadiusLg),
                                            )
                                            .clickable { appendKey(key) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            key,
                                            fontSize = if (key.length > 1) 16.sp else 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AppColors.TextPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Done button (for currency mode — no separate done key in grid)
            if (isCurrency) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(MiniPosTokens.Radius2xl))
                        .background(
                            Brush.linearGradient(listOf(AppColors.Primary, AppColors.PrimaryLight))
                        )
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Xong",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}
