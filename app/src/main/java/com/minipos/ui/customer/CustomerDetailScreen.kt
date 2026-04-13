package com.minipos.ui.customer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
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
import com.minipos.ui.components.MiniPosAlertDialog
import com.minipos.ui.components.PopupType
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.domain.model.Customer
import com.minipos.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════════════════════════════════════
// CUSTOMER DETAIL SCREEN — Matches customer-detail.html mock
// ═══════════════════════════════════════════════════════════════

@Composable
fun CustomerDetailScreen(
    customerId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onNavigateToPos: () -> Unit = {},
    viewModel: CustomerListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Snackbar for messages
    val snackbarHostState = remember { SnackbarHostState() }
    var showFeatureDialog by remember { mutableStateOf(false) }
    var featureDialogTitle by remember { mutableStateOf("") }

    // Feature coming-soon dialog
    MiniPosAlertDialog(
        visible = showFeatureDialog,
        type = PopupType.INFO,
        icon = Icons.Rounded.Info,
        title = featureDialogTitle,
        message = stringResource(R.string.feature_coming_soon_msg),
        confirmText = stringResource(R.string.ok),
        onConfirm = { showFeatureDialog = false },
    )

    LaunchedEffect(customerId) {
        viewModel.loadCustomerDetail(customerId)
    }

    val customer = state.detailCustomer

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding(),
    ) {
        // ─── Top Bar ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = AppColors.TextSecondary)
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.customer_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { customer?.let { onEdit(it.id) } }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.cd_edit), tint = AppColors.TextSecondary, modifier = Modifier.size(22.dp))
            }
            Box {
                var showMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.cd_more), tint = AppColors.TextSecondary, modifier = Modifier.size(22.dp))
                }
                MiniPosActionSheet(
                    visible = showMenu,
                    title = stringResource(R.string.customer_title),
                    description = customer?.name ?: "",
                    items = listOf(
                        ActionSheetItem(stringResource(R.string.cd_quick_call), Icons.Rounded.Call) {
                            showMenu = false
                            customer?.phone?.let { phone ->
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            }
                        },
                        ActionSheetItem(stringResource(R.string.cd_quick_message), Icons.Rounded.Sms) {
                            showMenu = false
                            customer?.phone?.let { phone ->
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone"))
                                context.startActivity(intent)
                            }
                        },
                        ActionSheetItem(stringResource(R.string.cd_quick_new_order), Icons.Rounded.AddShoppingCart) {
                            showMenu = false
                            onNavigateToPos()
                        },
                    ),
                    onDismiss = { showMenu = false },
                )
            }
        }

        if (customer == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.Primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                // ─── Profile Header ───
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF0E9AA0), Color(0xFF5AEDC5)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(customer.initials, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(customer.name, fontSize = 20.sp, fontWeight = FontWeight.Black, color = AppColors.TextPrimary)
                    Text(customer.phone ?: "", fontSize = 13.sp, color = AppColors.TextTertiary)
                    Spacer(Modifier.height(8.dp))

                    // Badges
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BadgePill(text = stringResource(R.string.cd_active), bgColor = AppColors.SuccessSoft, textColor = AppColors.Success)
                        if (customer.hasDebt) {
                            BadgePill(
                                text = stringResource(R.string.debt_badge, CurrencyFormatter.formatCompact(customer.debtAmount)),
                                bgColor = AppColors.Error.copy(alpha = 0.12f),
                                textColor = AppColors.Error,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ─── Quick Actions ───
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickActionButton(
                        icon = Icons.Rounded.Call,
                        label = stringResource(R.string.cd_quick_call),
                        iconColor = AppColors.Success,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            customer.phone?.let { phone ->
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            }
                        },
                    )
                    QuickActionButton(
                        icon = Icons.Rounded.Sms,
                        label = stringResource(R.string.cd_quick_message),
                        iconColor = AppColors.Accent,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            customer.phone?.let { phone ->
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone"))
                                context.startActivity(intent)
                            }
                        },
                    )
                    QuickActionButton(
                        icon = Icons.Rounded.AddShoppingCart,
                        label = stringResource(R.string.cd_quick_new_order),
                        iconColor = AppColors.PrimaryLight,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToPos() },
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ─── Stats Row ───
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DetailStatCard(
                        value = customer.visitCount.toString(),
                        label = stringResource(R.string.cd_total_orders),
                        valueStyle = "primary",
                        modifier = Modifier.weight(1f),
                    )
                    DetailStatCard(
                        value = CurrencyFormatter.formatCompact(customer.totalSpent),
                        label = stringResource(R.string.cd_total_spent),
                        valueStyle = "accent",
                        modifier = Modifier.weight(1f),
                    )
                    DetailStatCard(
                        value = CurrencyFormatter.formatCompact(customer.debtAmount),
                        label = stringResource(R.string.cd_debt),
                        valueStyle = "danger",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ─── Contact Info ───
                FormSectionLabel(stringResource(R.string.cd_contact_info))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.RadiusLg))
                        .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
                        .padding(16.dp),
                ) {
                    InfoRow(icon = Icons.Rounded.Call, label = stringResource(R.string.cd_phone), value = customer.phone ?: stringResource(R.string.cd_na))
                    HorizontalDivider(color = AppColors.Divider, modifier = Modifier.padding(vertical = 6.dp))
                    InfoRow(icon = Icons.Rounded.Email, label = stringResource(R.string.cd_email), value = customer.email ?: stringResource(R.string.cd_na))
                    HorizontalDivider(color = AppColors.Divider, modifier = Modifier.padding(vertical = 6.dp))
                    InfoRow(icon = Icons.Rounded.LocationOn, label = stringResource(R.string.cd_address), value = customer.address ?: stringResource(R.string.cd_na))
                    HorizontalDivider(color = AppColors.Divider, modifier = Modifier.padding(vertical = 6.dp))
                    InfoRow(icon = Icons.Rounded.CalendarToday, label = stringResource(R.string.cd_created), value = formatDate(customer.createdAt))
                }

                Spacer(Modifier.height(20.dp))

                // ─── Debt Section ───
                if (customer.hasDebt) {
                    FormSectionLabel(stringResource(R.string.cd_debt))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.RadiusLg))
                            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(stringResource(R.string.cd_current_balance), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                                Text(stringResource(R.string.debt_limit_label, CurrencyFormatter.format(500000.0)), fontSize = 11.sp, color = AppColors.TextTertiary)
                            }
                            Text(
                                CurrencyFormatter.format(customer.debtAmount),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = AppColors.Error,
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Debt progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(AppColors.InputBackground),
                        ) {
                            val progress = (customer.debtAmount / 500000.0).coerceIn(0.0, 1.0).toFloat()
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(AppColors.Error),
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    featureDialogTitle = context.getString(R.string.cd_collect)
                                    showFeatureDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(MiniPosTokens.RadiusMd),
                                border = BorderStroke(1.dp, AppColors.Success),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Success),
                            ) {
                                Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.cd_collect), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    featureDialogTitle = context.getString(R.string.cd_history)
                                    showFeatureDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(MiniPosTokens.RadiusMd),
                                border = BorderStroke(1.dp, AppColors.Border),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextSecondary),
                            ) {
                                Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.cd_history), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                }

                // ─── Notes ───
                if (!customer.notes.isNullOrBlank()) {
                    FormSectionLabel(stringResource(R.string.notes))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.RadiusLg))
                            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
                            .padding(16.dp),
                    ) {
                        Text(customer.notes, fontSize = 13.sp, color = AppColors.TextSecondary)
                    }
                    Spacer(Modifier.height(20.dp))
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════════════

@Composable
private fun BadgePill(text: String, bgColor: Color, textColor: Color) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        color = textColor,
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(MiniPosTokens.RadiusFull))
            .padding(horizontal = 12.dp, vertical = 3.dp),
    )
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary)
    }
}

@Composable
private fun DetailStatCard(
    value: String,
    label: String,
    valueStyle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = when (valueStyle) {
                "primary" -> AppColors.Primary
                "accent" -> AppColors.Accent
                "danger" -> AppColors.Error
                else -> AppColors.TextPrimary
            },
        )
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextTertiary)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 13.sp, color = AppColors.TextTertiary)
        }
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, textAlign = TextAlign.End)
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "N/A"
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
