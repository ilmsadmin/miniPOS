package com.minipos.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.minipos.R
import com.minipos.core.theme.AppColors
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════
// POPUP SYSTEM — Thiết kế đồng nhất cho toàn bộ ứng dụng Mini POS
//
// Bao gồm:
//   1. Toast — Thông báo nhẹ, tự ẩn
//   2. Snackbar — Thông báo có action
//   3. Alert Dialog — Thông báo quan trọng (Success / Error / Warning / Info)
//   4. Confirm Dialog — Xác nhận hành động (2 buttons)
//   5. Prompt Dialog — Nhập dữ liệu nhanh
//   6. Stacked Confirm Dialog — Confirm với 3+ buttons dọc
//   7. Loading Dialog — Spinner chờ xử lý
//   8. Action Sheet — Menu hành động (iOS-style bottom)
//   9. Bottom Sheet — Select List, Edit Form, Image Picker, Quantity Edit
//
// Cách sử dụng: Import và gọi các composable trực tiếp.
// Tất cả đã follow design tokens từ AppColors + MiniPosTokens.
// ═══════════════════════════════════════════════════════════════


// ─────────────────────────────────────
// ENUMS & DATA CLASSES
// ─────────────────────────────────────

/** Popup type variants for icons and colors */
enum class PopupType {
    SUCCESS, ERROR, WARNING, INFO, QUESTION, DELETE
}

/** An action item shown in an Action Sheet */
data class ActionSheetItem(
    val label: String,
    val icon: ImageVector,
    val style: ActionSheetItemStyle = ActionSheetItemStyle.DEFAULT,
    val onClick: () -> Unit,
)

enum class ActionSheetItemStyle { DEFAULT, MUTED, DANGER }

/** A selectable item for the Select List bottom sheet */
data class SelectListItem(
    val id: String,
    val name: String,
    val description: String = "",
    val icon: ImageVector? = null,
    val iconTint: Color = Color.Unspecified,
    val iconBackground: Color = Color.Unspecified,
)

/** Image picker options */
enum class ImagePickerOption {
    CAMERA, GALLERY, FILE, REMOVE
}

/** Stacked dialog button */
data class StackedDialogButton(
    val label: String,
    val icon: ImageVector? = null,
    val style: StackedButtonStyle = StackedButtonStyle.CANCEL,
    val onClick: () -> Unit,
)

enum class StackedButtonStyle { PRIMARY, DANGER, CANCEL }


// ═══════════════════════════════════════════════════════════════
// 1. TOAST — Thông báo nhẹ, tự ẩn (với type support)
//    Nâng cấp MiniPosToast hiện tại thêm variant colors
// ═══════════════════════════════════════════════════════════════

@Composable
fun MiniPosToastStyled(
    message: String,
    visible: Boolean,
    type: PopupType = PopupType.SUCCESS,
    durationMs: Long = 2000L,
    onDismiss: () -> Unit,
) {
    val (bgColor, iconColor, icon) = toastTypeConfig(type)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)) +
                slideInVertically(initialOffsetY = { -it }, animationSpec = tween(300, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(200)) +
                slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(250)),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 40.dp, vertical = 16.dp)
                .shadow(8.dp, RoundedCornerShape(MiniPosTokens.RadiusLg))
                .background(AppColors.SurfaceElevated, RoundedCornerShape(MiniPosTokens.RadiusLg))
                .border(1.dp, AppColors.BorderLight, RoundedCornerShape(MiniPosTokens.RadiusLg))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(bgColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            delay(durationMs)
            onDismiss()
        }
    }
}

@Composable
private fun toastTypeConfig(type: PopupType): Triple<Color, Color, ImageVector> {
    return when (type) {
        PopupType.SUCCESS -> Triple(AppColors.SuccessSoft, AppColors.Success, Icons.Rounded.Check)
        PopupType.ERROR -> Triple(AppColors.ErrorSoft, AppColors.Error, Icons.Rounded.Close)
        PopupType.WARNING -> Triple(AppColors.WarningSoft, AppColors.Warning, Icons.Rounded.Warning)
        PopupType.INFO -> Triple(AppColors.InfoSoft, AppColors.PrimaryLight, Icons.Rounded.Info)
        PopupType.QUESTION -> Triple(AppColors.InfoSoft, AppColors.Accent, Icons.Rounded.Help)
        PopupType.DELETE -> Triple(AppColors.ErrorSoft, AppColors.Error, Icons.Rounded.Delete)
    }
}


// ═══════════════════════════════════════════════════════════════
// 2. SNACKBAR — Thông báo có action (phía dưới)
// ═══════════════════════════════════════════════════════════════

@Composable
fun MiniPosSnackbar(
    message: String,
    visible: Boolean,
    type: PopupType = PopupType.SUCCESS,
    actionText: String? = null,
    durationMs: Long = 4000L,
    onAction: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val (bgColor, iconColor, icon) = toastTypeConfig(type)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) +
                slideInVertically(initialOffsetY = { it / 2 }, animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                )),
        exit = fadeOut(tween(200)) +
                slideOutVertically(targetOffsetY = { it / 2 }),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(12.dp, RoundedCornerShape(MiniPosTokens.RadiusLg))
                .background(AppColors.SurfaceElevated, RoundedCornerShape(MiniPosTokens.RadiusLg))
                .border(1.dp, AppColors.BorderLight, RoundedCornerShape(MiniPosTokens.RadiusLg))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(bgColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            // Text
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // Action button
            if (actionText != null && onAction != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = actionText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.PrimaryLight,
                    modifier = Modifier
                        .clip(RoundedCornerShape(MiniPosTokens.RadiusSm))
                        .clickable(onClick = onAction)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            delay(durationMs)
            onDismiss()
        }
    }
}


// ═══════════════════════════════════════════════════════════════
// SHARED — Overlay + Dialog Icon
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DialogIconCircle(type: PopupType, icon: ImageVector) {
    val (bgColor, fgColor) = when (type) {
        PopupType.SUCCESS -> AppColors.SuccessSoft to AppColors.Success
        PopupType.ERROR -> AppColors.ErrorSoft to AppColors.Error
        PopupType.WARNING -> AppColors.WarningSoft to AppColors.Warning
        PopupType.INFO -> AppColors.PrimaryLight.copy(alpha = 0.1f) to AppColors.PrimaryLight
        PopupType.QUESTION -> AppColors.Accent.copy(alpha = 0.1f) to AppColors.Accent
        PopupType.DELETE -> AppColors.ErrorSoft to AppColors.Error
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .background(bgColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = fgColor, modifier = Modifier.size(28.dp))
    }
}


// ═══════════════════════════════════════════════════════════════
// 3. ALERT DIALOG — Thông báo quan trọng
//    1 nút xác nhận, có icon + title + message
// ═══════════════════════════════════════════════════════════════

@Composable
fun MiniPosAlertDialog(
    visible: Boolean,
    type: PopupType = PopupType.SUCCESS,
    icon: ImageVector = Icons.Rounded.CheckCircle,
    title: String,
    message: String,
    confirmText: String = "",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = onConfirm,
) {
    if (!visible) return
    val resolvedConfirmText = confirmText.ifEmpty { stringResource(R.string.understood) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
            label = "alertScale",
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(16.dp, RoundedCornerShape(MiniPosTokens.Radius2xl))
                .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.Radius2xl))
                .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.Radius2xl)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Body
            Column(
                modifier = Modifier.padding(24.dp, 24.dp, 24.dp, 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DialogIconCircle(type, icon)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
            }
            // Divider + Button
            HorizontalDivider(color = AppColors.Divider)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable(onClick = onConfirm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = resolvedConfirmText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.PrimaryLight,
                )
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════
// 4. CONFIRM DIALOG — Xác nhận hành động (2 buttons)
// ═══════════════════════════════════════════════════════════════

@Composable
fun MiniPosConfirmDialog(
    visible: Boolean,
    type: PopupType = PopupType.DELETE,
    icon: ImageVector = Icons.Rounded.Delete,
    title: String,
    message: String,
    cancelText: String = "",
    confirmText: String = "",
    confirmStyle: ConfirmButtonStyle = ConfirmButtonStyle.DANGER,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!visible) return
    val resolvedCancelText = cancelText.ifEmpty { stringResource(R.string.cancel_btn_label) }
    val resolvedConfirmText = confirmText.ifEmpty { stringResource(R.string.delete_label) }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(MiniPosTokens.Radius2xl))
                .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.Radius2xl))
                .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.Radius2xl)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Body
            Column(
                modifier = Modifier.padding(24.dp, 24.dp, 24.dp, 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DialogIconCircle(type, icon)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
            }
            // Divider + Buttons row
            HorizontalDivider(color = AppColors.Divider)
            Row(modifier = Modifier.fillMaxWidth().height(52.dp)) {
                // Cancel button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = onCancel),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = resolvedCancelText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextSecondary,
                    )
                }
                // Vertical divider
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(AppColors.Divider)
                )
                // Confirm button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = onConfirm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = resolvedConfirmText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when (confirmStyle) {
                            ConfirmButtonStyle.DANGER -> AppColors.Error
                            ConfirmButtonStyle.PRIMARY -> AppColors.PrimaryLight
                            ConfirmButtonStyle.SUCCESS -> AppColors.Success
                        },
                    )
                }
            }
        }
    }
}

enum class ConfirmButtonStyle { DANGER, PRIMARY, SUCCESS }


// ═══════════════════════════════════════════════════════════════
// 5. PROMPT DIALOG — Nhập dữ liệu nhanh (1 input)
// ═══════════════════════════════════════════════════════════════

@Composable
fun MiniPosPromptDialog(
    visible: Boolean,
    type: PopupType = PopupType.INFO,
    icon: ImageVector = Icons.Rounded.Edit,
    title: String,
    message: String,
    inputLabel: String = "",
    inputPlaceholder: String = "",
    inputHint: String = "",
    inputValue: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    cancelText: String = "",
    confirmText: String = "",
    onInputChange: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    if (!visible) return
    val resolvedCancelText = cancelText.ifEmpty { stringResource(R.string.cancel_btn_label) }
    val resolvedConfirmText = confirmText.ifEmpty { stringResource(R.string.apply) }

    var localValue by remember(visible) { mutableStateOf(inputValue) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (visible) {
            delay(200)
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(MiniPosTokens.Radius2xl))
                .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.Radius2xl))
                .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.Radius2xl)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Body
            Column(
                modifier = Modifier.padding(24.dp, 24.dp, 24.dp, 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DialogIconCircle(type, icon)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
                // Input
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                ) {
                    if (inputLabel.isNotEmpty()) {
                        Text(
                            text = inputLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextSecondary,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    BasicTextField(
                        value = localValue,
                        onValueChange = {
                            localValue = it
                            onInputChange(it)
                        },
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Normal,
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        keyboardActions = KeyboardActions(onDone = { onConfirm(localValue) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .focusRequester(focusRequester)
                            .background(AppColors.InputBackground, RoundedCornerShape(MiniPosTokens.RadiusLg))
                            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
                            .padding(horizontal = 16.dp),
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (localValue.isEmpty()) {
                                    Text(
                                        text = inputPlaceholder,
                                        fontSize = 14.sp,
                                        color = AppColors.TextTertiary,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                    if (inputHint.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = inputHint,
                            fontSize = 11.sp,
                            color = AppColors.TextTertiary,
                        )
                    }
                }
            }
            // Actions
            HorizontalDivider(color = AppColors.Divider)
            Row(modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = onCancel),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(resolvedCancelText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary)
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(AppColors.Divider))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = { onConfirm(localValue) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(resolvedConfirmText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.PrimaryLight)
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════
// 6. STACKED CONFIRM DIALOG — 3+ buttons xếp dọc
// ═══════════════════════════════════════════════════════════════

@Composable
fun MiniPosStackedDialog(
    visible: Boolean,
    type: PopupType = PopupType.WARNING,
    icon: ImageVector = Icons.Rounded.Warning,
    title: String,
    message: String,
    buttons: List<StackedDialogButton>,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(MiniPosTokens.Radius2xl))
                .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.Radius2xl))
                .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.Radius2xl)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Body
            Column(
                modifier = Modifier.padding(24.dp, 24.dp, 24.dp, 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DialogIconCircle(type, icon)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
            }
            // Stacked buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                buttons.forEach { btn ->
                    when (btn.style) {
                        StackedButtonStyle.PRIMARY -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                                    .background(MiniPosGradients.primary())
                                    .clickable(onClick = btn.onClick),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    if (btn.icon != null) {
                                        Icon(btn.icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(btn.label, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                }
                            }
                        }
                        StackedButtonStyle.DANGER -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                                    .background(AppColors.Error)
                                    .clickable(onClick = btn.onClick),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(btn.label, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                        }
                        StackedButtonStyle.CANCEL -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                                    .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
                                    .clickable(onClick = btn.onClick),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(btn.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════
// 7. LOADING DIALOG — Spinner chờ xử lý
// ═══════════════════════════════════════════════════════════════

@Composable
fun MiniPosLoadingDialog(
    visible: Boolean,
    message: String = "",
    onDismiss: () -> Unit = {},
) {
    if (!visible) return
    val resolvedMessage = message.ifEmpty { stringResource(R.string.processing) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .width(160.dp)
                .shadow(16.dp, RoundedCornerShape(MiniPosTokens.Radius2xl))
                .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.Radius2xl))
                .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.Radius2xl))
                .padding(top = 32.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp,
                color = AppColors.Primary,
                trackColor = AppColors.BorderLight,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = resolvedMessage,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════════
// 8. ACTION SHEET — Menu hành động iOS-style (bottom)
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPosActionSheet(
    visible: Boolean,
    title: String = "",
    description: String = "",
    items: List<ActionSheetItem>,
    cancelText: String = "",
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val resolvedCancelText = cancelText.ifEmpty { stringResource(R.string.cancel_btn_label) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        shape = RoundedCornerShape(0.dp),
        scrimColor = AppColors.OverlayBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = 8.dp),
        ) {
            // Main group
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(MiniPosTokens.RadiusXl))
                    .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.RadiusXl))
                    .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusXl)),
            ) {
                // Header
                if (title.isNotEmpty() || description.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (title.isNotEmpty()) {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AppColors.TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (description.isNotEmpty()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = description,
                                fontSize = 11.sp,
                                color = AppColors.TextTertiary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                // Items
                items.forEachIndexed { index, item ->
                    if (index > 0 || title.isNotEmpty() || description.isNotEmpty()) {
                        HorizontalDivider(color = AppColors.Divider)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable(onClick = item.onClick),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val color = when (item.style) {
                            ActionSheetItemStyle.DEFAULT -> AppColors.PrimaryLight
                            ActionSheetItemStyle.MUTED -> AppColors.TextSecondary
                            ActionSheetItemStyle.DANGER -> AppColors.Error
                        }
                        Icon(item.icon, null, tint = color, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = item.label,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = color,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Cancel button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(4.dp, RoundedCornerShape(MiniPosTokens.RadiusXl))
                    .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.RadiusXl))
                    .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusXl))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = resolvedCancelText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.TextPrimary,
                )
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════
// 9. BOTTOM SHEET WRAPPER — Base component cho các bottom sheet
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPosBottomSheet(
    visible: Boolean,
    title: String,
    onDismiss: () -> Unit,
    footer: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.Surface,
        shape = RoundedCornerShape(topStart = MiniPosTokens.Radius2xl, topEnd = MiniPosTokens.Radius2xl),
        scrimColor = AppColors.OverlayBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppColors.TextTertiary.copy(alpha = 0.35f)),
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(AppColors.InputBackground, CircleShape),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Body (scrollable)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = if (footer == null) 20.dp else 0.dp),
            ) {
                content()
            }

            // Footer
            if (footer != null) {
                HorizontalDivider(color = AppColors.Divider)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    footer()
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════
// 9a. SELECT LIST BOTTOM SHEET — Chọn 1 item từ danh sách
// ═══════════════════════════════════════════════════════════════

@Composable
fun MiniPosSelectSheet(
    visible: Boolean,
    title: String,
    items: List<SelectListItem>,
    selectedId: String? = null,
    onSelect: (SelectListItem) -> Unit,
    onDismiss: () -> Unit,
) {
    MiniPosBottomSheet(
        visible = visible,
        title = title,
        onDismiss = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items.forEach { item ->
                val isSelected = item.id == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                        .background(
                            if (isSelected) AppColors.PrimaryLight.copy(alpha = 0.08f)
                            else Color.Transparent
                        )
                        .let {
                            if (isSelected) it.border(1.5.dp, AppColors.Primary, RoundedCornerShape(MiniPosTokens.RadiusLg))
                            else it
                        }
                        .clickable { onSelect(item) }
                        .padding(12.dp, 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Icon
                    if (item.icon != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (item.iconBackground != Color.Unspecified) item.iconBackground
                                    else AppColors.InputBackground,
                                    RoundedCornerShape(MiniPosTokens.RadiusMd)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                item.icon, null,
                                tint = if (item.iconTint != Color.Unspecified) item.iconTint else AppColors.TextSecondary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                    // Info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary,
                        )
                        if (item.description.isNotEmpty()) {
                            Text(
                                text = item.description,
                                fontSize = 11.sp,
                                color = AppColors.TextTertiary,
                            )
                        }
                    }
                    // Check
                    AnimatedVisibility(visible = isSelected) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            null,
                            tint = AppColors.PrimaryLight,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════
// 9b. BOTTOM SHEET FORM — Form chỉnh sửa nhanh
//     Dùng MiniPosBottomSheet wrapper + custom field composables
// ═══════════════════════════════════════════════════════════════

@Composable
fun BottomSheetField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    fontWeight: FontWeight = FontWeight.Normal,
    textColor: Color = AppColors.TextPrimary,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextSecondary,
            )
            if (required) {
                Text(" *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.Error)
            }
        }
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = textColor,
                fontWeight = fontWeight,
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(AppColors.InputBackground, RoundedCornerShape(MiniPosTokens.RadiusLg))
                .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
                .padding(horizontal = 16.dp),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(placeholder, fontSize = 14.sp, color = AppColors.TextTertiary)
                    }
                    inner()
                }
            },
        )
    }
}

/** Primary gradient button for bottom sheet footers */
@Composable
fun BottomSheetPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
            .background(MiniPosGradients.primary())
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
}

/** Danger outlined button for bottom sheet footers */
@Composable
fun BottomSheetDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
            .border(1.5.dp, AppColors.Error, RoundedCornerShape(MiniPosTokens.RadiusXl))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, null, tint = AppColors.Error, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.Error)
        }
    }
}

/** Outline button for bottom sheet footers */
@Composable
fun BottomSheetOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
            .border(1.5.dp, AppColors.BorderLight, RoundedCornerShape(MiniPosTokens.RadiusXl))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary)
        }
    }
}


// ═══════════════════════════════════════════════════════════════
// 9c. IMAGE PICKER BOTTOM SHEET — Chọn ảnh (camera / gallery)
// ═══════════════════════════════════════════════════════════════

@Composable
fun MiniPosImagePickerSheet(
    visible: Boolean,
    title: String = "",
    showRemove: Boolean = true,
    onPick: (ImagePickerOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val resolvedTitle = title.ifEmpty { stringResource(R.string.pick_image_title) }
    MiniPosBottomSheet(
        visible = visible,
        title = resolvedTitle,
        onDismiss = onDismiss,
    ) {
        val columns = if (showRemove) 2 else 2
        val options = buildList {
            add(ImagePickerOption.CAMERA)
            add(ImagePickerOption.GALLERY)
            add(ImagePickerOption.FILE)
            if (showRemove) add(ImagePickerOption.REMOVE)
        }

        // 2-column grid
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { option ->
                        ImagePickerOptionCard(
                            option = option,
                            onClick = { onPick(option) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Fill remaining space if odd
                    if (row.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagePickerOptionCard(
    option: ImagePickerOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icon, label, gradient) = when (option) {
        ImagePickerOption.CAMERA -> Triple(
            Icons.Rounded.PhotoCamera,
            stringResource(R.string.take_photo),
            Brush.linearGradient(listOf(Color(0xFF0E9AA0), Color(0xFF2EC4B6))),
        )
        ImagePickerOption.GALLERY -> Triple(
            Icons.Rounded.PhotoLibrary,
            stringResource(R.string.pick_from_gallery),
            Brush.linearGradient(listOf(Color(0xFF14B8B0), Color(0xFF5AEDC5))),
        )
        ImagePickerOption.FILE -> Triple(
            Icons.Rounded.FolderOpen,
            stringResource(R.string.choose_file),
            Brush.linearGradient(listOf(Color(0xFFFF8A65), Color(0xFFFF5252))),
        )
        ImagePickerOption.REMOVE -> Triple(
            Icons.Rounded.Delete,
            stringResource(R.string.remove_image),
            Brush.linearGradient(listOf(AppColors.ErrorSoft, AppColors.ErrorSoft)),
        )
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
            .border(2.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusXl))
            .background(AppColors.Surface)
            .clickable(onClick = onClick)
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(gradient, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon, null,
                tint = if (option == ImagePickerOption.REMOVE) AppColors.Error else Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
        )
    }
}


// ═══════════════════════════════════════════════════════════════
// 9d. QUANTITY EDIT BOTTOM SHEET — Chỉnh số lượng
// ═══════════════════════════════════════════════════════════════

@Composable
fun MiniPosQuantitySheet(
    visible: Boolean,
    title: String = "",
    itemName: String,
    itemSubtitle: String = "",
    itemIcon: @Composable (() -> Unit)? = null,
    unitPrice: Long,
    currentQuantity: Int,
    formatPrice: (Long) -> String = { "${it}đ" },
    onQuantityChange: (Int) -> Unit,
    onDelete: (() -> Unit)? = null,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val resolvedTitle = title.ifEmpty { stringResource(R.string.adjust_quantity) }
    var qty by remember(visible, currentQuantity) { mutableIntStateOf(currentQuantity) }

    MiniPosBottomSheet(
        visible = visible,
        title = resolvedTitle,
        onDismiss = onDismiss,
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Delete button (optional)
                if (onDelete != null) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
                            .border(1.5.dp, AppColors.Error, RoundedCornerShape(MiniPosTokens.RadiusXl))
                            .clickable(onClick = onDelete),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Delete, null, tint = AppColors.Error, modifier = Modifier.size(20.dp))
                    }
                }
                // Confirm button
                BottomSheetPrimaryButton(
                    text = stringResource(R.string.confirm_btn),
                    icon = Icons.Rounded.Check,
                    onClick = { onConfirm(qty) },
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        // Product info row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.InputBackground, RoundedCornerShape(MiniPosTokens.RadiusLg))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (itemIcon != null) {
                itemIcon()
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = itemName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                )
                if (itemSubtitle.isNotEmpty()) {
                    Text(
                        text = itemSubtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.PrimaryLight,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Quantity controls: [-] [input] [+]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Minus button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .border(2.dp, AppColors.BorderLight, CircleShape)
                    .clip(CircleShape)
                    .background(AppColors.Surface)
                    .clickable {
                        if (qty > 1) {
                            qty--
                            onQuantityChange(qty)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Remove, null, tint = AppColors.TextPrimary, modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.width(20.dp))

            // Quantity display
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(64.dp)
                    .border(2.dp, AppColors.Primary, RoundedCornerShape(MiniPosTokens.RadiusXl))
                    .background(AppColors.InputBackground, RoundedCornerShape(MiniPosTokens.RadiusXl)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = qty.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.width(20.dp))

            // Plus button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MiniPosGradients.primary())
                    .clickable {
                        qty++
                        onQuantityChange(qty)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        // Total row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.InputBackground, RoundedCornerShape(MiniPosTokens.RadiusMd))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.line_total_label),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextSecondary,
                )
                Text(
                    text = formatPrice(unitPrice * qty),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = AppColors.TextPrimary,
                )
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════
// POPUP HOST STATE — Quản lý trạng thái popup tập trung
// Có thể dùng ở cấp App/Screen thay vì rải rác nhiều biến
// ═══════════════════════════════════════════════════════════════

/**
 * Central state holder for popup visibility + data.
 * Usage:
 * ```
 * val popupState = remember { MiniPosPopupState() }
 * popupState.showToast("Đã lưu!", PopupType.SUCCESS)
 * MiniPosPopupHost(popupState)
 * ```
 */
class MiniPosPopupState {
    // Toast
    var toastVisible by mutableStateOf(false)
        private set
    var toastMessage by mutableStateOf("")
        private set
    var toastType by mutableStateOf(PopupType.SUCCESS)
        private set

    fun showToast(message: String, type: PopupType = PopupType.SUCCESS) {
        toastMessage = message
        toastType = type
        toastVisible = true
    }

    fun hideToast() {
        toastVisible = false
    }

    // Snackbar
    var snackbarVisible by mutableStateOf(false)
        private set
    var snackbarMessage by mutableStateOf("")
        private set
    var snackbarType by mutableStateOf(PopupType.SUCCESS)
        private set
    var snackbarActionText by mutableStateOf<String?>(null)
        private set
    var snackbarAction by mutableStateOf<(() -> Unit)?>(null)
        private set

    fun showSnackbar(
        message: String,
        type: PopupType = PopupType.SUCCESS,
        actionText: String? = null,
        action: (() -> Unit)? = null,
    ) {
        snackbarMessage = message
        snackbarType = type
        snackbarActionText = actionText
        snackbarAction = action
        snackbarVisible = true
    }

    fun hideSnackbar() {
        snackbarVisible = false
    }

    // Loading
    var loadingVisible by mutableStateOf(false)
        private set
    var loadingMessage by mutableStateOf("")
        private set

    fun showLoading(message: String = "") {
        loadingMessage = message
        loadingVisible = true
    }

    fun hideLoading() {
        loadingVisible = false
    }
}

/**
 * Drop-in composable that renders Toast + Snackbar + Loading
 * from a shared [MiniPosPopupState]. Place in your screen's
 * Box overlay area.
 */
@Composable
fun MiniPosPopupHost(
    state: MiniPosPopupState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Toast (top-center)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp),
        ) {
            MiniPosToastStyled(
                message = state.toastMessage,
                visible = state.toastVisible,
                type = state.toastType,
                onDismiss = { state.hideToast() },
            )
        }

        // Snackbar (bottom)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 90.dp),
        ) {
            MiniPosSnackbar(
                message = state.snackbarMessage,
                visible = state.snackbarVisible,
                type = state.snackbarType,
                actionText = state.snackbarActionText,
                onAction = {
                    state.snackbarAction?.invoke()
                    state.hideSnackbar()
                },
                onDismiss = { state.hideSnackbar() },
            )
        }

        // Loading
        MiniPosLoadingDialog(
            visible = state.loadingVisible,
            message = state.loadingMessage,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// 14. SELECT BOX — Inline select field + bottom sheet picker
//     Thay thế ExposedDropdownMenu / DropdownMenu bằng design đồng nhất
// ═══════════════════════════════════════════════════════════════

/**
 * An inline select field that opens a [MiniPosSelectSheet] when tapped.
 * Replaces Material3 ExposedDropdownMenuBox / DropdownMenu with a unified
 * design matching the app's BottomSheetField style.
 *
 * @param label       Field label shown above the select box
 * @param title       Title for the bottom sheet when open
 * @param items       List of selectable items
 * @param selectedId  Currently selected item ID (null = nothing selected)
 * @param placeholder Text shown when nothing is selected
 * @param required    Show red asterisk after label
 * @param onSelect    Callback when an item is selected
 * @param modifier    Modifier for the root Column
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPosSelectBox(
    label: String,
    title: String,
    items: List<SelectListItem>,
    selectedId: String?,
    onSelect: (SelectListItem) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    required: Boolean = false,
    enabled: Boolean = true,
) {
    var showSheet by remember { mutableStateOf(false) }
    val selectedItem = items.find { it.id == selectedId }

    Column(modifier = modifier) {
        // Label
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextSecondary,
            )
            if (required) {
                Text(" *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.Error)
            }
        }
        Spacer(Modifier.height(4.dp))

        // Trigger box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    if (enabled) AppColors.InputBackground else AppColors.InputBackground.copy(alpha = 0.5f),
                    RoundedCornerShape(MiniPosTokens.RadiusLg),
                )
                .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
                .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                .then(
                    if (enabled) Modifier.clickable { showSheet = true }
                    else Modifier
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon (if selected item has one)
            if (selectedItem?.icon != null) {
                Icon(
                    selectedItem.icon, null,
                    tint = if (selectedItem.iconTint != Color.Unspecified) selectedItem.iconTint else AppColors.Primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
            }

            // Text
            Text(
                text = selectedItem?.name ?: placeholder,
                fontSize = 14.sp,
                fontWeight = if (selectedItem != null) FontWeight.Medium else FontWeight.Normal,
                color = if (selectedItem != null) AppColors.TextPrimary else AppColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            // Chevron
            Icon(
                Icons.Rounded.UnfoldMore,
                contentDescription = null,
                tint = AppColors.TextTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }

    // Bottom sheet picker
    if (showSheet) {
        MiniPosSelectSheet(
            visible = true,
            title = title,
            items = items,
            selectedId = selectedId,
            onSelect = { item ->
                onSelect(item)
                showSheet = false
            },
            onDismiss = { showSheet = false },
        )
    }
}

/**
 * Compact variant of [MiniPosSelectBox] without a label — for inline usage in
 * tables, list items, or tight layouts (e.g., stock audit reason column).
 */
@Composable
fun MiniPosSelectBoxCompact(
    title: String,
    items: List<SelectListItem>,
    selectedId: String?,
    onSelect: (SelectListItem) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    height: Dp = 40.dp,
) {
    var showSheet by remember { mutableStateOf(false) }
    val selectedItem = items.find { it.id == selectedId }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusSm))
            .background(AppColors.InputBackground)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusSm))
            .clickable { showSheet = true }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedItem?.name ?: placeholder,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selectedItem != null) AppColors.TextPrimary else AppColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Rounded.UnfoldMore,
                contentDescription = null,
                tint = AppColors.TextTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    }

    if (showSheet) {
        MiniPosSelectSheet(
            visible = true,
            title = title,
            items = items,
            selectedId = selectedId,
            onSelect = { item ->
                onSelect(item)
                showSheet = false
            },
            onDismiss = { showSheet = false },
        )
    }
}
