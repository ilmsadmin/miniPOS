package com.minipos.core.receipt

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.domain.model.OrderDetail
import com.minipos.domain.model.Store

/**
 * Full-screen receipt preview dialog with WebView rendering
 * and action buttons for Print and Share.
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
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error messages inside the dialog
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
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
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            color = AppColors.Surface,
            shadowElevation = 8.dp,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.receipt_preview_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                        }
                    }

                    HorizontalDivider(color = AppColors.Divider)

                    // WebView receipt preview
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    webViewClient = WebViewClient()
                                    settings.javaScriptEnabled = false
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    setBackgroundColor(android.graphics.Color.WHITE)
                                    loadDataWithBaseURL(
                                        null,
                                        htmlContent,
                                        "text/html",
                                        "UTF-8",
                                        null,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    HorizontalDivider(color = AppColors.Divider)

                    // Action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onPrint,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isPrinting,
                        ) {
                            if (isPrinting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.print_receipt_btn), fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = onShare,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                            enabled = !isSharing,
                        ) {
                            if (isSharing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = androidx.compose.ui.graphics.Color.White,
                                )
                            } else {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.share_btn), fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Snackbar overlay inside the dialog
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp),
                )
            }
        }
    }
}
