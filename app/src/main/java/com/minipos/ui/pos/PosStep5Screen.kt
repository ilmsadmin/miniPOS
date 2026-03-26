package com.minipos.ui.pos

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.core.theme.AppColors

@Composable
fun PosStep5Screen(
    onNewOrder: () -> Unit,
    onGoHome: () -> Unit,
    viewModel: PosStep5ViewModel = hiltViewModel(),
) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                    contentDescription = "Thành công",
                    modifier = Modifier.size(64.dp),
                    tint = AppColors.Secondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Thanh toán thành công!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.Secondary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Đơn hàng đã được lưu thành công.",
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
            Text("Tạo đơn hàng mới", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
            Text("Về trang chủ", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Secondary actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TextButton(onClick = { /* TODO: Print receipt */ }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = AppColors.TextSecondary)
                    Text("In hóa đơn", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
            }
            TextButton(onClick = { /* TODO: Share receipt */ }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = AppColors.TextSecondary)
                    Text("Chia sẻ", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
            }
        }
    }

    // Clear cart when this screen appears
    LaunchedEffect(Unit) {
        // Cart is kept for display; it will be cleared on "new order" or "go home"
    }
}
