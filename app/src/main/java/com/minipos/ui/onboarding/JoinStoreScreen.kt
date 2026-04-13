package com.minipos.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.core.sync.DiscoveredDevice
import com.minipos.core.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinStoreScreen(
    onBack: () -> Unit,
    onJoinComplete: () -> Unit = {},
    viewModel: JoinStoreViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.step) {
        if (state.step == JoinStep.SUCCESS) {
            kotlinx.coroutines.delay(1500)
            onJoinComplete()
        }
    }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Tham gia cửa hàng", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.step == JoinStep.ENTER_CODE) onBack()
                        else viewModel.backToEnterCode()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background,
                    titleContentColor = AppColors.TextPrimary,
                    navigationIconContentColor = AppColors.TextPrimary,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInHorizontally { it / 3 })
                        .togetherWith(fadeOut(tween(200)) + slideOutHorizontally { -it / 3 })
                },
                label = "join_step",
            ) { step ->
                when (step) {
                    JoinStep.ENTER_CODE -> EnterCodeStep(
                        state = state,
                        onCodeChanged = viewModel::onStoreCodeChanged,
                        onStartScan = viewModel::startScan,
                    )
                    JoinStep.SCANNING -> ScanningStep(
                        state = state,
                        onRetry = viewModel::retryScanning,
                        onBack = viewModel::backToEnterCode,
                    )
                    JoinStep.FOUND -> FoundDevicesStep(
                        state = state,
                        onConnect = viewModel::connectToDevice,
                        onRescan = viewModel::retryScanning,
                    )
                    JoinStep.SYNCING -> SyncingStep(state = state)
                    JoinStep.SUCCESS -> SuccessStep()
                    JoinStep.ERROR -> ErrorStep(
                        state = state,
                        onRetry = viewModel::retryScanning,
                        onBack = viewModel::backToEnterCode,
                    )
                }
            }
        }
    }
}

// ─── Step 1: Nhập Store Code ──────────────────────────────────────────────────

@Composable
private fun EnterCodeStep(
    state: JoinStoreState,
    onCodeChanged: (String) -> Unit,
    onStartScan: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(AppColors.Primary.copy(alpha = 0.2f), Color.Transparent),
                    )
                )
                .border(2.dp, AppColors.Primary.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Store,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(40.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Nhập mã cửa hàng",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Liên hệ chủ cửa hàng để lấy mã. Cả hai thiết bị phải kết nối cùng mạng Wi-Fi.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = state.storeCode,
            onValueChange = onCodeChanged,
            label = { Text("Mã cửa hàng") },
            placeholder = { Text("VD: SHOP01") },
            leadingIcon = { Icon(Icons.Rounded.Tag, contentDescription = null) },
            isError = state.storeCodeError != null,
            supportingText = state.storeCodeError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                onStartScan()
            }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                onStartScan()
            },
            enabled = state.storeCode.length >= 4,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Tìm cửa hàng", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(32.dp))

        InfoCard(
            icon = Icons.Rounded.Info,
            text = "Yêu cầu: Mở app trên thiết bị chủ cửa hàng, vào Cài đặt → Đồng bộ Wi-Fi và bật máy chủ.",
        )
    }
}

// ─── Step 2: Đang quét ────────────────────────────────────────────────────────

@Composable
private fun ScanningStep(
    state: JoinStoreState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PulsingWifiLoader(color = AppColors.Primary)

        Spacer(Modifier.height(32.dp))

        Text(
            "Đang tìm cửa hàng «${state.storeCode}»…",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Đảm bảo cả hai thiết bị đang kết nối cùng mạng Wi-Fi và thiết bị chủ đã bật máy chủ.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Spacer(Modifier.height(40.dp))

        OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Quét lại")
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text("← Nhập lại mã", color = AppColors.TextSecondary)
        }
    }
}

// ─── Step 3: Tìm thấy thiết bị ───────────────────────────────────────────────

@Composable
private fun FoundDevicesStep(
    state: JoinStoreState,
    onConnect: (DiscoveredDevice) -> Unit,
    onRescan: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))

        Icon(
            Icons.Rounded.Devices,
            contentDescription = null,
            tint = AppColors.Success,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Tìm thấy ${state.devices.size} thiết bị",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Chọn thiết bị chủ cửa hàng «${state.storeCode}» để tham gia",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(state.devices) { device ->
                DeviceCard(device = device, onClick = { onConnect(device) })
            }
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRescan) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Quét lại", color = AppColors.TextSecondary)
        }
    }
}

@Composable
private fun DeviceCard(device: DiscoveredDevice, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = AppColors.Surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AppColors.Primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PhoneAndroid,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.deviceName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                )
                Text(
                    "${device.host.hostAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = AppColors.TextTertiary)
        }
    }
}

// ─── Step 4: Đang đồng bộ ────────────────────────────────────────────────────

@Composable
private fun SyncingStep(state: JoinStoreState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PulsingWifiLoader(color = AppColors.Accent)

        Spacer(Modifier.height(32.dp))

        Text(
            state.statusMessage.ifBlank { "Đang tải dữ liệu cửa hàng…" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Vui lòng không tắt ứng dụng",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
        )
    }
}

// ─── Step 5: Thành công ───────────────────────────────────────────────────────

@Composable
private fun SuccessStep() {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "success_scale",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(AppColors.Success.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = AppColors.Success,
                modifier = Modifier.size(56.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Tham gia thành công!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Dữ liệu cửa hàng đã được tải về.\nĐang chuyển sang màn hình đăng nhập…",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

// ─── Lỗi ─────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorStep(
    state: JoinStoreState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = AppColors.Error,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Không thể kết nối",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            state.errorMessage ?: "Đã xảy ra lỗi không xác định",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Thử lại")
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text("← Nhập lại mã", color = AppColors.TextSecondary)
        }
    }
}

// ─── Shared components ────────────────────────────────────────────────────────

@Composable
private fun PulsingWifiLoader(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale",
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "rotation",
    )
    Box(
        modifier = Modifier
            .size(88.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(88.dp).rotate(rotation),
            color = color,
            strokeWidth = 3.dp,
        )
        Icon(Icons.Rounded.Wifi, contentDescription = null, tint = color, modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun InfoCard(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Primary.copy(alpha = 0.08f))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}
