package com.minipos.ui.pos

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.receipt.ReceiptPreviewDialog
import com.minipos.core.theme.AppColors

@SuppressLint("MissingPermission")
@Composable
fun PosStep5Screen(
    onNewOrder: () -> Unit,
    onGoHome: () -> Unit,
    viewModel: PosStep5ViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show messages (only when receipt preview is NOT open)
    LaunchedEffect(state.message, state.showReceiptPreview) {
        if (!state.showReceiptPreview) {
            state.message?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearMessage()
            }
        }
    }

    // Printer selection dialog
    if (state.showPrinterDialog) {
        PosStep5PrinterDialog(
            devices = state.pairedDevices,
            onSelect = { viewModel.printToDevice(context, it) },
            onDismiss = { viewModel.dismissPrinterDialog() },
        )
    }

    // Share options dialog
    if (state.showShareOptions) {
        PosStep5ShareDialog(
            onSharePdf = { viewModel.shareAsPdf(context) },
            onShareText = { viewModel.shareAsText(context) },
            onDismiss = { viewModel.dismissShareOptions() },
        )
    }

    // Receipt preview dialog
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

    // Success animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Success icon
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale),
                shape = CircleShape,
                color = AppColors.SecondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.payment_success),
                        modifier = Modifier.size(64.dp),
                        tint = AppColors.Secondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                stringResource(R.string.step5_success_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.Secondary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                stringResource(R.string.order_saved_msg),
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Action buttons
            Button(
                onClick = { viewModel.clearCartAndNavigate(onNewOrder) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.new_order_btn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { viewModel.clearCartAndNavigate(onGoHome) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Home, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.back_to_home), style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Print & Share actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TextButton(
                    onClick = { viewModel.showReceiptPreview() },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = AppColors.TextSecondary)
                        Text(stringResource(R.string.print_receipt), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    }
                }
                TextButton(
                    onClick = { viewModel.onShareClick(context) },
                    enabled = !state.isSharing,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (state.isSharing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = AppColors.TextSecondary,
                            )
                        } else {
                            Icon(Icons.Default.Share, contentDescription = null, tint = AppColors.TextSecondary)
                        }
                        Text(stringResource(R.string.share_receipt), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    }
                }
            }
        }
    }
}

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
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Share, contentDescription = null, tint = AppColors.Primary) },
        title = { Text(stringResource(R.string.share_receipt_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.select_share_format),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onSharePdf() },
                    shape = RoundedCornerShape(8.dp),
                    color = AppColors.SurfaceVariant,
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AppColors.Error, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.share_pdf), fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.share_pdf_desc), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                        }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onShareText() },
                    shape = RoundedCornerShape(8.dp),
                    color = AppColors.SurfaceVariant,
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.TextSnippet, contentDescription = null, tint = AppColors.Secondary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.share_text), fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.share_text_desc), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}
