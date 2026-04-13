package com.minipos.ui.pos

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.receipt.ReceiptPreviewDialog
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin
import kotlin.random.Random

// ═══════════════════════════════════════
// POS STEP 5 — ORDER COMPLETE SCREEN
// Matches order-complete.html mock design
// ═══════════════════════════════════════

@SuppressLint("MissingPermission")
@Composable
fun PosStep5Screen(
    onNewOrder: () -> Unit,
    onGoHome: () -> Unit,
    onViewOrderDetail: ((String) -> Unit)? = null,
    viewModel: PosStep5ViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Auto-redirect countdown ──
    // 15 seconds gives cashier enough time to print/share before being redirected.
    // Pause when any dialog/overlay is active so an in-progress action never gets cut off.
    var countdown by remember { mutableIntStateOf(15) }
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000L)
            // Only count down when no dialog is open — prevents redirect mid-action
            if (!state.showReceiptPreview && !state.showPrinterDialog && !state.showShareOptions) {
                countdown--
            }
        }
        viewModel.clearCartAndNavigate(onGoHome)
    }

    // Show messages (only when receipt preview is NOT open)
    LaunchedEffect(state.message, state.showReceiptPreview) {
        if (!state.showReceiptPreview) {
            state.message?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearMessage()
            }
        }
    }

    // ── Dialogs ──

    if (state.showPrinterDialog) {
        PosStep5PrinterDialog(
            devices = state.pairedDevices,
            onSelect = { viewModel.printToDevice(context, it) },
            onDismiss = { viewModel.dismissPrinterDialog() },
        )
    }

    if (state.showShareOptions) {
        PosStep5ShareDialog(
            onSharePdf = { viewModel.shareAsPdf(context) },
            onShareText = { viewModel.shareAsText(context) },
            onDismiss = { viewModel.dismissShareOptions() },
        )
    }

    if (state.showReceiptPreview && state.store != null && state.orderDetail != null) {
        ReceiptPreviewDialog(
            store = state.store!!,
            orderDetail = state.orderDetail!!,
            isPrinting = state.isPrinting,
            isSharing = state.isSharing,
            errorMessage = state.message,
            onPrint = { viewModel.onPrintClick(context) },
            onShare = { viewModel.shareAsPdf(context) },
            onDismiss = { viewModel.dismissReceiptPreview() },
            onErrorShown = { viewModel.clearMessage() },
        )
    }

    // ── Staggered entrance animations ──
    val enterAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enterAnim.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
    }

    // ── Derive display data from state ──
    val orderCode = state.orderDetail?.order?.orderCode ?: "#---"
    val totalAmount = state.orderDetail?.order?.totalAmount ?: 0.0
    val createdAt = state.orderDetail?.order?.createdAt ?: System.currentTimeMillis()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy — HH:mm", Locale.getDefault()) }
    val dateStr = remember(createdAt) { dateFormat.format(Date(createdAt)) }
    val amountStr = remember(totalAmount) { CurrencyFormatter.format(totalAmount) }

    Scaffold(
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // ── Confetti particles layer ──
            ConfettiOverlay()

            // ── Main content ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // ── Success check circle with animated rings ──
                SuccessCheckAnimation()

                Spacer(modifier = Modifier.height(28.dp))

                // ── Title ──
                AnimatedEntrance(delayMs = 600) {
                    Text(
                        text = stringResource(R.string.step5_success_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = AppColors.TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ── Order ID ──
                AnimatedEntrance(delayMs = 700) {
                    Text(
                        text = stringResource(R.string.step5_order_label, orderCode),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextTertiary,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ── Amount with gradient text ──
                AnimatedEntrance(delayMs = 800) {
                    Text(
                        text = amountStr,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        style = LocalTextStyle.current.copy(
                            brush = MiniPosGradients.price(),
                        ),
                        letterSpacing = (-0.5).sp,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ── Date ──
                AnimatedEntrance(delayMs = 900) {
                    Text(
                        text = dateStr,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextTertiary,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ── Action buttons ──
                AnimatedEntrance(delayMs = 1000) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Print receipt
                        OrderCompleteActionButton(
                            icon = Icons.Default.Print,
                            text = stringResource(R.string.step5_print_receipt_btn),
                            onClick = { viewModel.showReceiptPreview() },
                        )

                        // Share via Zalo / SMS
                        OrderCompleteActionButton(
                            icon = Icons.Default.Share,
                            text = stringResource(R.string.step5_share_zalo_sms),
                            onClick = { viewModel.onShareClick(context) },
                            enabled = !state.isSharing,
                            isLoading = state.isSharing,
                        )

                        // View order detail — navigates to full OrderDetailScreen
                        // Falls back to receipt preview if caller hasn't wired the navigation yet
                        OrderCompleteActionButton(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            text = stringResource(R.string.step5_view_order_detail),
                            onClick = {
                                val orderId = state.orderDetail?.order?.id
                                if (onViewOrderDetail != null && orderId != null) {
                                    onViewOrderDetail(orderId)
                                } else {
                                    viewModel.showReceiptPreview()
                                }
                            },
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Primary CTA — New Order
                        OrderCompletePrimaryButton(
                            icon = Icons.Default.AddCircle,
                            text = stringResource(R.string.step5_create_new_order),
                            onClick = { viewModel.clearCartAndNavigate(onNewOrder) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Auto-redirect text ──
                AnimatedEntrance(delayMs = 1200) {
                    Text(
                        text = stringResource(R.string.step5_auto_redirect, countdown),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextTertiary,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// ═══════════════════════════════════════
// SUCCESS CHECK ANIMATION
// Animated circle with pulsing rings
// ═══════════════════════════════════════

@Composable
private fun SuccessCheckAnimation() {
    // Ring animations
    val ringScale1 = remember { Animatable(0.5f) }
    val ringAlpha1 = remember { Animatable(0f) }
    val ringScale2 = remember { Animatable(0.5f) }
    val ringAlpha2 = remember { Animatable(0f) }

    // Circle animation
    val circleScale = remember { Animatable(0.3f) }
    val circleAlpha = remember { Animatable(0f) }

    // Check icon animation
    val iconScale = remember { Animatable(0f) }
    val iconAlpha = remember { Animatable(0f) }
    val iconRotation = remember { Animatable(-30f) }

    LaunchedEffect(Unit) {
        // Circle pops in first
        launch {
            delay(100)
            circleAlpha.animateTo(1f, tween(500, easing = EaseOutBack))
        }
        launch {
            delay(100)
            circleScale.animateTo(1f, tween(500, easing = EaseOutBack))
        }

        // Inner ring
        launch {
            delay(200)
            ringAlpha1.animateTo(1f, tween(600, easing = EaseOutBack))
        }
        launch {
            delay(200)
            ringScale1.animateTo(1f, tween(600, easing = EaseOutBack))
        }

        // Outer ring
        launch {
            delay(500)
            ringAlpha2.animateTo(1f, tween(600, easing = EaseOutBack))
        }
        launch {
            delay(500)
            ringScale2.animateTo(1f, tween(600, easing = EaseOutBack))
        }

        // Check icon
        launch {
            delay(500)
            iconAlpha.animateTo(1f, tween(400, easing = EaseOutBack))
        }
        launch {
            delay(500)
            iconScale.animateTo(1f, tween(400, easing = EaseOutBack))
        }
        launch {
            delay(500)
            iconRotation.animateTo(0f, tween(400, easing = EaseOutBack))
        }
    }

    val successColor = AppColors.Success

    Box(
        modifier = Modifier.size(148.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Outer ring
        Box(
            modifier = Modifier
                .size(148.dp)
                .graphicsLayer {
                    scaleX = ringScale2.value
                    scaleY = ringScale2.value
                    alpha = ringAlpha2.value
                }
                .border(
                    width = 2.dp,
                    color = successColor.copy(alpha = 0.2f),
                    shape = CircleShape,
                ),
        )

        // Inner ring
        Box(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {
                    scaleX = ringScale1.value
                    scaleY = ringScale1.value
                    alpha = ringAlpha1.value
                }
                .border(
                    width = 3.dp,
                    color = successColor,
                    shape = CircleShape,
                ),
        )

        // Main circle with gradient
        Box(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {
                    scaleX = circleScale.value
                    scaleY = circleScale.value
                    alpha = circleAlpha.value
                }
                .shadow(
                    elevation = 24.dp,
                    shape = CircleShape,
                    ambientColor = successColor.copy(alpha = 0.3f),
                    spotColor = successColor.copy(alpha = 0.3f),
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(successColor, Color(0xFF69F0AE)),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                    ),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = stringResource(R.string.payment_success),
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                        alpha = iconAlpha.value
                        rotationZ = iconRotation.value
                    },
                tint = Color.White,
            )
        }
    }
}

// ═══════════════════════════════════════
// CONFETTI OVERLAY
// Colorful falling particles
// ═══════════════════════════════════════

private data class ConfettiParticle(
    val color: Color,
    val xFraction: Float,
    val delay: Float,
    val width: Float,
    val height: Float,
    val speed: Float,
)

@Composable
private fun ConfettiOverlay() {
    val primary = AppColors.Primary
    val accent = AppColors.Accent
    val success = AppColors.Success
    val warning = AppColors.Warning
    val primaryLight = AppColors.PrimaryLight
    val error = AppColors.Error

    val particles = remember {
        listOf(
            ConfettiParticle(primary, 0.10f, 0.2f, 8f, 8f, 1.0f),
            ConfettiParticle(accent, 0.25f, 0.4f, 6f, 10f, 0.8f),
            ConfettiParticle(success, 0.40f, 0.1f, 8f, 8f, 1.1f),
            ConfettiParticle(warning, 0.55f, 0.5f, 10f, 6f, 0.9f),
            ConfettiParticle(primaryLight, 0.70f, 0.3f, 8f, 8f, 1.0f),
            ConfettiParticle(accent, 0.85f, 0.6f, 6f, 6f, 0.85f),
            ConfettiParticle(error, 0.15f, 0.7f, 8f, 8f, 0.95f),
            ConfettiParticle(success, 0.60f, 0.15f, 7f, 9f, 1.05f),
            ConfettiParticle(primary, 0.80f, 0.45f, 8f, 8f, 0.9f),
            ConfettiParticle(warning, 0.35f, 0.55f, 8f, 8f, 1.0f),
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "confettiProgress",
    )

    // One-shot entrance animation
    val entranceAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entranceAlpha.animateTo(1f, tween(500))
        delay(2500)
        entranceAlpha.animateTo(0f, tween(500))
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = entranceAlpha.value },
    ) {
        particles.forEach { p ->
            val adjustedProgress = (progress + p.delay) % 1f
            val y = -0.1f + adjustedProgress * 1.2f
            val x = p.xFraction + sin(adjustedProgress * 6.28 * 2).toFloat() * 0.03f
            val particleAlpha = if (adjustedProgress < 0.5f) 1f else (1f - (adjustedProgress - 0.5f) * 2f)
            val rotation = adjustedProgress * 720f
            val particleScale = 1f - adjustedProgress * 0.7f

            drawContext.canvas.save()
            drawContext.canvas.translate(
                size.width * x,
                size.height * y,
            )
            drawContext.canvas.rotate(rotation)
            drawContext.canvas.scale(particleScale, particleScale)

            drawRect(
                color = p.color.copy(alpha = particleAlpha.coerceIn(0f, 1f)),
                topLeft = Offset(-p.width / 2, -p.height / 2),
                size = androidx.compose.ui.geometry.Size(p.width, p.height),
            )

            drawContext.canvas.restore()
        }
    }
}

// ═══════════════════════════════════════
// ANIMATED ENTRANCE — Fade Up
// ═══════════════════════════════════════

@Composable
private fun AnimatedEntrance(
    delayMs: Int,
    content: @Composable () -> Unit,
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(12f) }

    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        launch { alpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        launch { offsetY.animateTo(0f, tween(500, easing = FastOutSlowInEasing)) }
    }

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = offsetY.value * density
        },
    ) {
        content()
    }
}

// ═══════════════════════════════════════
// ACTION BUTTON — Outlined style
// Matches .act-btn from mock
// ═══════════════════════════════════════

@Composable
private fun OrderCompleteActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(MiniPosTokens.RadiusLg),
        color = AppColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.BorderLight),
        onClick = onClick,
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = AppColors.TextSecondary,
                )
            } else {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = AppColors.TextSecondary,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
            )
        }
    }
}

// ═══════════════════════════════════════
// PRIMARY BUTTON — Gradient FAB style
// Matches .act-primary from mock
// ═══════════════════════════════════════

@Composable
private fun OrderCompletePrimaryButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(MiniPosTokens.Radius2xl),
                ambientColor = AppColors.PrimaryGlow,
                spotColor = AppColors.PrimaryGlow,
            )
            .clip(RoundedCornerShape(MiniPosTokens.Radius2xl))
            .background(MiniPosGradients.fab())
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = Color.White,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
        }
    }
}

// ═══════════════════════════════════════
// DIALOGS (unchanged from original)
// ═══════════════════════════════════════

@SuppressLint("MissingPermission")
@Composable
private fun PosStep5PrinterDialog(
    devices: List<android.bluetooth.BluetoothDevice>,
    onSelect: (android.bluetooth.BluetoothDevice) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Print, contentDescription = null, tint = AppColors.Primary) },
        title = { Text(stringResource(R.string.select_printer)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.select_paired_printer),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                devices.forEach { device ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(device) },
                        shape = RoundedCornerShape(8.dp),
                        color = AppColors.SurfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = AppColors.Primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    device.name ?: stringResource(R.string.unknown_device),
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    device.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextSecondary,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun PosStep5ShareDialog(
    onSharePdf: () -> Unit,
    onShareText: () -> Unit,
    onDismiss: () -> Unit,
) {
    MiniPosActionSheet(
        visible = true,
        title = stringResource(R.string.share_receipt_title),
        description = stringResource(R.string.select_share_format),
        items = listOf(
            ActionSheetItem(
                label = stringResource(R.string.share_pdf),
                icon = Icons.Filled.PictureAsPdf,
                onClick = onSharePdf,
            ),
            ActionSheetItem(
                label = stringResource(R.string.share_text),
                icon = Icons.AutoMirrored.Filled.TextSnippet,
                onClick = onShareText,
            ),
        ),
        onDismiss = onDismiss,
    )
}
