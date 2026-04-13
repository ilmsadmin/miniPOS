package com.minipos.core.receipt

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.domain.model.OrderDetail
import com.minipos.domain.model.Store
import com.minipos.ui.components.MiniPosGradients
import com.minipos.ui.components.MiniPosPopupState
import com.minipos.ui.components.MiniPosPopupHost
import com.minipos.ui.components.MiniPosTokens
import com.minipos.ui.components.PopupType

/**
 * Full-screen receipt preview dialog — redesigned to match Mini POS design system.
 * Features gradient header, styled action buttons, WebView receipt, and toast messages.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReceiptPreviewDialog(
    store: Store,
    orderDetail: OrderDetail,
    isPrinting: Boolean = false,
    isSharing: Boolean = false,
    errorMessage: String? = null,
    onPrint: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
    onErrorShown: () -> Unit = {},
) {
    val context = LocalContext.current
    val htmlContent = remember(orderDetail, store) {
        ReceiptGenerator.generateHtmlReceipt(context, store, orderDetail)
    }
    val popupState = remember { MiniPosPopupState() }

    // Show error messages via toast
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            popupState.showToast(errorMessage, PopupType.ERROR)
            onErrorShown()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.88f)
                    .shadow(24.dp, RoundedCornerShape(MiniPosTokens.Radius2xl))
                    .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.Radius2xl))
                    .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.Radius2xl)),
            ) {
                // ═══ Gradient Header ═══
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(AppColors.Primary, AppColors.PrimaryLight)),
                            RoundedCornerShape(
                                topStart = MiniPosTokens.Radius2xl,
                                topEnd = MiniPosTokens.Radius2xl,
                            ),
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Receipt icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Receipt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))

                        // Title + order code
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.receipt_preview_title),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            )
                            Text(
                                orderDetail.order.orderCode,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.75f),
                            )
                        }

                        // Close button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cancel),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                // ═══ WebView Receipt ═══
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                        .border(1.dp, AppColors.BorderLight, RoundedCornerShape(MiniPosTokens.RadiusMd)),
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = false
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                setBackgroundColor(android.graphics.Color.WHITE)
                                loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // ═══ Action Buttons ═══
                HorizontalDivider(color = AppColors.Divider)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Print button
                    ReceiptActionButton(
                        icon = Icons.Rounded.Print,
                        label = stringResource(R.string.print_receipt_btn),
                        isLoading = isPrinting,
                        modifier = Modifier.weight(1f),
                        onClick = onPrint,
                    )
                    // Share button — gradient primary
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
                            .background(MiniPosGradients.primary())
                            .clickable(enabled = !isSharing, onClick = onShare),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSharing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Share, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.share_btn),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }

            // Toast overlay
            MiniPosPopupHost(state = popupState, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ReceiptActionButton(
    icon: ImageVector,
    label: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
            .background(AppColors.InputBackground)
            .border(1.5.dp, AppColors.BorderLight, RoundedCornerShape(MiniPosTokens.RadiusXl))
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = AppColors.Primary,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = AppColors.TextSecondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                )
            }
        }
    }
}
