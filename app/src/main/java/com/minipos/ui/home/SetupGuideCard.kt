package com.minipos.ui.home

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.ui.components.MiniPosTokens

// ═══════════════════════════════════════════════════════
// SETUP GUIDE — Step-by-step onboarding card
// Shows on HomeScreen when store has no data yet
// ═══════════════════════════════════════════════════════

data class SetupStep(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val isCompleted: Boolean,
    val count: Int = 0,
    val onClick: () -> Unit,
)

@Composable
fun SetupGuideCard(
    categoryCount: Int,
    productCount: Int,
    supplierCount: Int,
    orderCount: Int,
    onNavigateCategory: () -> Unit,
    onNavigateProduct: () -> Unit,
    onNavigateSupplier: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val steps = listOf(
        SetupStep(
            titleRes = R.string.setup_step1_title,
            descRes = R.string.setup_step1_desc,
            icon = Icons.Rounded.Storefront,
            isCompleted = true, // Store already created during onboarding
            count = 1,
            onClick = {},
        ),
        SetupStep(
            titleRes = R.string.setup_step2_title,
            descRes = R.string.setup_step2_desc,
            icon = Icons.Rounded.Category,
            isCompleted = categoryCount > 0,
            count = categoryCount,
            onClick = onNavigateCategory,
        ),
        SetupStep(
            titleRes = R.string.setup_step3_title,
            descRes = R.string.setup_step3_desc,
            icon = Icons.Rounded.Inventory2,
            isCompleted = productCount > 0,
            count = productCount,
            onClick = onNavigateProduct,
        ),
        SetupStep(
            titleRes = R.string.setup_step4_title,
            descRes = R.string.setup_step4_desc,
            icon = Icons.Rounded.LocalShipping,
            isCompleted = supplierCount > 0,
            count = supplierCount,
            onClick = onNavigateSupplier,
        ),
        SetupStep(
            titleRes = R.string.setup_step5_title,
            descRes = R.string.setup_step5_desc,
            icon = Icons.Rounded.PointOfSale,
            isCompleted = orderCount > 0,
            count = orderCount,
            onClick = {},
        ),
    )

    val completedCount = steps.count { it.isCompleted }
    val progress = completedCount.toFloat() / steps.size

    // Animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "progress",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusXl))
            .padding(20.dp),
    ) {
        // ── Header ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                // Rocket icon with gradient bg
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(AppColors.Primary, AppColors.PrimaryLight),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.RocketLaunch,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(R.string.setup_guide_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        stringResource(R.string.setup_guide_progress, completedCount, steps.size),
                        fontSize = 12.sp,
                        color = AppColors.TextTertiary,
                    )
                }
            }

            // Dismiss button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.setup_guide_dismiss),
                    tint = AppColors.TextTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Progress bar ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AppColors.SurfaceElevated),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(AppColors.Primary, AppColors.Secondary),
                        ),
                    ),
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Steps ──
        steps.forEachIndexed { index, step ->
            SetupStepRow(
                step = step,
                stepNumber = index + 1,
                isLast = index == steps.lastIndex,
            )
            if (index < steps.lastIndex) {
                // Connector line
                Row(modifier = Modifier.padding(start = 18.dp)) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(8.dp)
                            .background(
                                if (step.isCompleted) AppColors.Primary.copy(alpha = 0.3f)
                                else AppColors.Border,
                            ),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Dismiss text ──
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.textButtonColors(contentColor = AppColors.TextTertiary),
        ) {
            Text(
                stringResource(R.string.setup_guide_skip),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun SetupStepRow(
    step: SetupStep,
    stepNumber: Int,
    isLast: Boolean,
) {
    val isClickable = !step.isCompleted && (stepNumber <= 4) // Step 5 = sell, not navigable directly

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isClickable) {
                    Modifier
                        .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                        .clickable { step.onClick() }
                        .padding(vertical = 6.dp)
                } else {
                    Modifier.padding(vertical = 6.dp)
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── Step indicator circle ──
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (step.isCompleted) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppColors.Secondary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                val borderColor = if (isClickable) AppColors.Primary else AppColors.Border
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppColors.SurfaceElevated)
                        .border(1.5.dp, borderColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        step.icon,
                        contentDescription = null,
                        tint = if (isClickable) AppColors.Primary else AppColors.TextTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // ── Text ──
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(step.titleRes),
                    fontSize = 14.sp,
                    fontWeight = if (step.isCompleted) FontWeight.Medium else FontWeight.SemiBold,
                    color = if (step.isCompleted) AppColors.TextSecondary else AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (step.isCompleted && step.count > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "(${step.count})",
                        fontSize = 12.sp,
                        color = AppColors.Secondary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Text(
                stringResource(step.descRes),
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // ── Arrow for actionable steps ──
        if (isClickable) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
