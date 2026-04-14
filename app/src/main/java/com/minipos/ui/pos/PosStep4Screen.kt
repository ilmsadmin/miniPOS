package com.minipos.ui.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.domain.model.PaymentMethod
import com.minipos.R
import com.minipos.ui.components.*

// ═══════════════════════════════════════
// POS STEP 4 — PAYMENT SCREEN
// Matches payment.html mock design:
// Amount display → Methods → Cash input + Numpad → Confirm
// ═══════════════════════════════════════

@Composable
fun PosStep4Screen(
    onPaymentSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: PosStep4ViewModel = hiltViewModel(),
) {
    val cart by viewModel.cartHolder.cart.collectAsState()
    val state by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current

    Scaffold(
        containerColor = AppColors.Background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
        ) {
            // ── Top bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.ArrowBack, "Back", tint = AppColors.TextSecondary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.step4_title), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.TextPrimary)
            }

            // ── Scrollable content ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── Amount display ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.total_to_pay_label),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextTertiary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        CurrencyFormatter.format(cart.grandTotal),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp,
                        color = AppColors.Primary,
                    )
                }

                // ── Payment methods ──
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        stringResource(R.string.payment_method_label).uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextTertiary,
                        letterSpacing = 0.8.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PaymentMethodCard(
                            icon = Icons.Rounded.Payments,
                            label = stringResource(R.string.payment_method_cash),
                            selected = state.selectedMethod == PaymentMethod.CASH,
                            onClick = { viewModel.selectMethod(PaymentMethod.CASH) },
                            modifier = Modifier.weight(1f),
                        )
                        PaymentMethodCard(
                            icon = Icons.Rounded.AccountBalance,
                            label = stringResource(R.string.payment_method_transfer),
                            selected = state.selectedMethod == PaymentMethod.TRANSFER,
                            onClick = { viewModel.selectMethod(PaymentMethod.TRANSFER) },
                            modifier = Modifier.weight(1f),
                        )
                        PaymentMethodCard(
                            icon = Icons.Rounded.Smartphone,
                            label = stringResource(R.string.payment_method_ewallet),
                            selected = state.selectedMethod == PaymentMethod.EWALLET,
                            onClick = { viewModel.selectMethod(PaymentMethod.EWALLET) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // ── Cash input section ──
                if (state.selectedMethod == PaymentMethod.CASH) {
                    // Customer pay input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            stringResource(R.string.customer_pays_label),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextSecondary,
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                                .border(2.dp, AppColors.BorderLight, RoundedCornerShape(MiniPosTokens.RadiusLg))
                                .background(AppColors.InputBackground)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Text(
                                if (state.receivedAmountText.isNotBlank()) {
                                    CurrencyFormatter.format((state.receivedAmountText.toDoubleOrNull() ?: 0.0))
                                } else "",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AppColors.TextPrimary,
                            )
                        }
                    }

                    // Quick amounts
                    val quickAmounts = viewModel.getQuickAmounts(cart.grandTotal)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Exact amount button
                        QuickAmountButton(
                            text = CurrencyFormatter.formatCompact(cart.grandTotal),
                            isExact = true,
                            onClick = { viewModel.updateReceivedAmount(cart.grandTotal.toLong().toString()) },
                            modifier = Modifier.weight(1f),
                        )
                        quickAmounts.take(2).forEach { amount ->
                            QuickAmountButton(
                                text = CurrencyFormatter.formatCompact(amount),
                                isExact = false,
                                onClick = { viewModel.updateReceivedAmount(amount.toLong().toString()) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (quickAmounts.size > 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            quickAmounts.drop(2).take(3).forEach { amount ->
                                QuickAmountButton(
                                    text = CurrencyFormatter.formatCompact(amount),
                                    isExact = false,
                                    onClick = { viewModel.updateReceivedAmount(amount.toLong().toString()) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat((3 - quickAmounts.drop(2).take(3).size).coerceAtLeast(0)) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    // Change display
                    val receivedAmount = state.receivedAmountText.toDoubleOrNull() ?: 0.0
                    val diff = receivedAmount - cart.grandTotal
                    if (receivedAmount > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
                                    .background(
                                        if (diff >= 0) AppColors.Success.copy(alpha = 0.1f)
                                        else AppColors.Error.copy(alpha = 0.1f),
                                    )
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    if (diff >= 0) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                    null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (diff >= 0) AppColors.Success else AppColors.Error,
                                )
                                Text(
                                    if (diff >= 0) stringResource(R.string.change_label) else stringResource(R.string.still_owe_label),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.TextSecondary,
                                )
                                Text(
                                    CurrencyFormatter.format(kotlin.math.abs(diff)),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (diff >= 0) AppColors.Success else AppColors.Error,
                                )
                            }
                        }
                    }

                    // Numpad
                    val numKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "000", "0", "del")
                    val rows = numKeys.chunked(3)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rows.forEach { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                row.forEach { key ->
                                    NumpadKey(
                                        key = key,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (key == "del") {
                                                val current = state.receivedAmountText
                                                if (current.isNotEmpty()) {
                                                    viewModel.updateReceivedAmount(current.dropLast(1).ifEmpty { "" })
                                                }
                                            } else {
                                                val current = state.receivedAmountText
                                                if (current.length < 10) {
                                                    viewModel.updateReceivedAmount(current + key)
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                    )
                                }
                            }
                        }
                    }
                }
            } // end scrollable content

            // ── Bottom action (always visible, outside scroll) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Background)
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 12.dp),
            ) {
                // Notes row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Rounded.EditNote,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = AppColors.TextTertiary,
                    )
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = { viewModel.updateNotes(it) },
                        label = { Text(stringResource(R.string.notes_label)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = AppColors.TextPrimary,
                            fontSize = 13.sp,
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Primary,
                            unfocusedBorderColor = AppColors.Border,
                            focusedLabelColor = AppColors.Primary,
                            unfocusedLabelColor = AppColors.TextTertiary,
                            cursorColor = AppColors.Primary,
                            focusedContainerColor = AppColors.InputBackground,
                            unfocusedContainerColor = AppColors.InputBackground,
                        ),
                        shape = RoundedCornerShape(MiniPosTokens.RadiusLg),
                    )
                }

                // Error
                if (state.error != null) {
                    Text(
                        state.error!!,
                        color = AppColors.Error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                // Confirm button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(MiniPosTokens.Radius2xl))
                        .background(
                            if (!state.isProcessing && state.canConfirm(cart.grandTotal))
                                Brush.linearGradient(listOf(AppColors.Primary, AppColors.Accent, AppColors.PrimaryLight))
                            else Brush.linearGradient(listOf(AppColors.TextTertiary.copy(alpha = 0.4f), AppColors.TextTertiary.copy(alpha = 0.4f))),
                        )
                        .clickable(
                            enabled = !state.isProcessing && state.canConfirm(cart.grandTotal),
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.confirmPayment(
                                grandTotal = cart.grandTotal,
                                onSuccess = onPaymentSuccess,
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Rounded.Verified, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Text(
                            stringResource(R.string.confirm_payment_btn),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

// ─── Payment method card ───
@Composable
private fun PaymentMethodCard(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .border(
                width = 2.dp,
                color = if (selected) AppColors.Primary else AppColors.Border,
                shape = RoundedCornerShape(MiniPosTokens.RadiusLg),
            )
            .background(
                if (selected) AppColors.Primary.copy(alpha = 0.08f) else AppColors.Surface,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                .background(
                    if (selected) Brush.linearGradient(listOf(AppColors.Primary, AppColors.Accent))
                    else Brush.linearGradient(listOf(AppColors.SurfaceElevated, AppColors.SurfaceElevated)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(24.dp),
                tint = if (selected) Color.White else AppColors.TextTertiary,
            )
        }
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) AppColors.TextPrimary else AppColors.TextSecondary,
        )
    }
}

// ─── Quick amount button ───
@Composable
private fun QuickAmountButton(
    text: String,
    isExact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
            .border(
                1.dp,
                if (isExact) AppColors.Primary else AppColors.BorderLight,
                RoundedCornerShape(MiniPosTokens.RadiusMd),
            )
            .background(
                if (isExact) AppColors.Primary.copy(alpha = 0.06f) else AppColors.Surface,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isExact) AppColors.PrimaryLight else AppColors.TextSecondary,
        )
    }
}

// ─── Numpad key ───
@Composable
private fun NumpadKey(
    key: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (key == "del") {
            Icon(
                Icons.Rounded.Backspace,
                contentDescription = "Delete",
                modifier = Modifier.size(24.dp),
                tint = AppColors.Error,
            )
        } else {
            Text(
                key,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
            )
        }
    }
}
