package com.minipos.ui.sync

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.sync.DiscoveredDevice
import com.minipos.core.sync.SyncStatus
import com.minipos.core.theme.AppColors
import com.minipos.ui.components.MiniPosConfirmDialog
import com.minipos.ui.components.MiniPosTopBar
import com.minipos.ui.components.MiniPosTokens
import com.minipos.ui.components.PopupType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WifiSyncScreen(
    onBack: () -> Unit,
    viewModel: WifiSyncViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Auto-start server + scan on open
    LaunchedEffect(Unit) {
        viewModel.startServer()
        viewModel.startScan()
    }

    // Confirm sync dialog
    if (state.confirmDevice != null) {
        MiniPosConfirmDialog(
            visible = true,
            type = PopupType.INFO,
            icon = Icons.Rounded.Sync,
            title = stringResource(R.string.wifi_sync_confirm_title),
            message = stringResource(R.string.wifi_sync_confirm_msg, state.confirmDevice!!.deviceName),
            cancelText = stringResource(R.string.cancel),
            confirmText = stringResource(R.string.wifi_sync_connect_btn),
            onCancel = { viewModel.dismissConfirm() },
            onConfirm = { viewModel.confirmSync() },
        )
    }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            MiniPosTopBar(
                title = stringResource(R.string.wifi_sync_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        viewModel.stopScan()
                        viewModel.startScan()
                    }) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.wifi_sync_refresh_btn),
                            tint = AppColors.TextSecondary,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Server status card
            item {
                ServerStatusCard(
                    storeName = state.storeName,
                    storeCode = state.storeCode,
                    deviceName = state.deviceName,
                    isRunning = state.isServerRunning,
                    lastSyncAt = state.lastSyncAt,
                )
            }

            // Sync status banner
            item {
                SyncStatusBanner(status = state.status, onDismiss = { viewModel.resetStatus() })
            }

            // Discovered devices header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.wifi_sync_label).uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.TextTertiary,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.status is SyncStatus.Scanning) {
                        val infiniteAnim = rememberInfiniteTransition(label = "scan")
                        val rotation by infiniteAnim.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
                            label = "scanRotate",
                        )
                        Icon(
                            Icons.Rounded.Radar,
                            contentDescription = null,
                            tint = AppColors.Primary,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayerRotation(rotation),
                        )
                    }
                }
            }

            if (state.status is SyncStatus.Scanning && state.devices.isEmpty()) {
                item {
                    ScanningEmptyState()
                }
            } else if (state.devices.isEmpty() && state.status !is SyncStatus.Scanning) {
                item {
                    NoDevicesState(onScan = {
                        viewModel.stopScan()
                        viewModel.startScan()
                    })
                }
            } else {
                items(state.devices, key = { it.host.hostAddress ?: it.deviceName }) { device ->
                    DeviceCard(
                        device = device,
                        isSyncing = state.status.let {
                            it is SyncStatus.Syncing && it.deviceName == device.deviceName ||
                            it is SyncStatus.Connecting && it.deviceName == device.deviceName
                        },
                        onSync = { viewModel.requestSync(device) },
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// Workaround for graphicsLayer rotation
@Composable
private fun Modifier.graphicsLayerRotation(rotation: Float): Modifier =
    this.graphicsLayer { rotationZ = rotation }

@Composable
private fun ServerStatusCard(
    storeName: String,
    storeCode: String,
    deviceName: String,
    isRunning: Boolean,
    lastSyncAt: Long?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(MiniPosTokens.RadiusXl))
            .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
            .background(AppColors.Surface)
            .border(1.dp, if (isRunning) AppColors.Primary.copy(alpha = 0.3f) else AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusXl))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                .background(
                    if (isRunning)
                        Brush.linearGradient(listOf(AppColors.Primary, AppColors.PrimaryLight))
                    else
                        Brush.linearGradient(listOf(AppColors.TextTertiary, AppColors.TextSecondary)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isRunning) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                storeName,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.TextPrimary,
            )
            Text(
                stringResource(R.string.wifi_sync_store_code, storeCode),
                fontSize = 12.sp,
                color = AppColors.TextTertiary,
            )
            if (lastSyncAt != null) {
                val fmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                Text(
                    stringResource(R.string.wifi_sync_last_sync, fmt.format(Date(lastSyncAt))),
                    fontSize = 11.sp,
                    color = AppColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                Text(
                    stringResource(R.string.wifi_sync_never),
                    fontSize = 11.sp,
                    color = AppColors.TextTertiary,
                )
            }
        }
        if (isRunning) {
            Box(
                modifier = Modifier
                    .background(AppColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(MiniPosTokens.RadiusFull))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    "●  Online",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.Primary,
                )
            }
        }
    }
}

@Composable
private fun SyncStatusBanner(status: SyncStatus, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = status !is SyncStatus.Idle && status !is SyncStatus.Scanning && status !is SyncStatus.Found,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        val (icon, text, bgColor, textColor) = when (status) {
            is SyncStatus.Connecting -> Quadruple(Icons.Rounded.Sync, stringResource(R.string.wifi_sync_connecting, status.deviceName), AppColors.Primary.copy(alpha = 0.1f), AppColors.Primary)
            is SyncStatus.Syncing -> Quadruple(Icons.Rounded.Sync, stringResource(R.string.wifi_sync_syncing, status.deviceName), AppColors.Primary.copy(alpha = 0.1f), AppColors.Primary)
            is SyncStatus.Success -> Quadruple(Icons.Rounded.CheckCircle, stringResource(R.string.wifi_sync_success, status.deviceName), AppColors.SuccessSoft, AppColors.Success)
            is SyncStatus.Error -> Quadruple(Icons.Rounded.Error, stringResource(R.string.wifi_sync_failed, status.message), AppColors.ErrorSoft, AppColors.Error)
            else -> return@AnimatedVisibility
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                .background(bgColor)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isAnimating = status is SyncStatus.Connecting || status is SyncStatus.Syncing
            if (isAnimating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = textColor,
                )
            } else {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor, modifier = Modifier.weight(1f))
            if (status is SyncStatus.Success || status is SyncStatus.Error) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onDismiss),
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun DeviceCard(
    device: DiscoveredDevice,
    isSyncing: Boolean,
    onSync: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(MiniPosTokens.RadiusXl))
            .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusXl))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                .background(Brush.linearGradient(listOf(Color(0xFF00B894), Color(0xFF00CEC9)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.PhoneAndroid,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(device.deviceName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            Text(
                device.host.hostAddress ?: "",
                fontSize = 12.sp,
                color = AppColors.TextTertiary,
            )
        }

        if (isSyncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = AppColors.Primary,
            )
        } else {
            Box(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(MiniPosTokens.RadiusLg))
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                    .background(Brush.linearGradient(listOf(AppColors.Primary, AppColors.PrimaryLight)))
                    .clickable(onClick = onSync)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    stringResource(R.string.wifi_sync_connect_btn),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ScanningEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val infiniteAnim = rememberInfiniteTransition(label = "scan_pulse")
        val scale by infiniteAnim.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
            label = "scanPulse",
        )
        Icon(
            Icons.Rounded.WifiFind,
            contentDescription = null,
            tint = AppColors.Primary.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.wifi_sync_scanning),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NoDevicesState(onScan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Rounded.WifiOff,
            contentDescription = null,
            tint = AppColors.TextTertiary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.wifi_sync_no_devices),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onScan,
            shape = RoundedCornerShape(MiniPosTokens.RadiusXl),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary),
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.wifi_sync_refresh_btn), fontWeight = FontWeight.SemiBold)
        }
    }
}
