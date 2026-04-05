package com.minipos.ui.supplier

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.domain.model.Supplier
import com.minipos.ui.components.*

// ═══════════════════════════════════════
// ICON GRADIENT PALETTE (matches mock s1–s6)
// ═══════════════════════════════════════

private val SupplierIconGradients = listOf(
    listOf(Color(0xFFFF6B6B), Color(0xFFEE5A24)), // s1 — red-orange
    listOf(Color(0xFF4BB8F0), Color(0xFF2196F3)), // s2 — blue
    listOf(Color(0xFF81C784), Color(0xFF388E3C)), // s3 — green
    listOf(Color(0xFFFFD54F), Color(0xFFFFB300)), // s4 — amber
    listOf(Color(0xFFCE93D8), Color(0xFF8E24AA)), // s5 — purple
    listOf(Color(0xFF6C5CE7), Color(0xFFA78BFA)), // s6 — indigo
)

private fun supplierIconGradient(index: Int): Brush {
    val colors = SupplierIconGradients[index % SupplierIconGradients.size]
    return Brush.linearGradient(colors)
}

// ═══════════════════════════════════════
// SUPPLIER LIST SCREEN
// ═══════════════════════════════════════

@Composable
fun SupplierListScreen(
    onBack: () -> Unit,
    onNavigateToForm: (String?) -> Unit = {},
    onNavigateToPurchase: () -> Unit = {},
    viewModel: SupplierListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = AppColors.Background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // ── Top bar with add button (matches mock topbar) ──
            MiniPosTopBar(
                title = stringResource(R.string.supplier_title),
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { onNavigateToForm(null) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MiniPosGradients.primary()),
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.add_supplier_cd),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
            )

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            } else if (state.suppliers.isEmpty()) {
                // ── Empty state ──
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(AppColors.SurfaceElevated),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.LocalShipping, contentDescription = null, modifier = Modifier.size(40.dp), tint = AppColors.TextTertiary)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.no_suppliers), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        MiniPosGradientButton(
                            text = stringResource(R.string.add_supplier_btn),
                            onClick = { onNavigateToForm(null) },
                            modifier = Modifier.width(200.dp),
                            height = 44.dp,
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // ── Search Bar ──
                    item {
                        MiniPosSearchBar(
                            value = state.searchQuery,
                            onValueChange = { viewModel.search(it) },
                            placeholder = stringResource(R.string.search_supplier_hint),
                        )
                    }

                    // ── Supplier cards ──
                    itemsIndexed(state.filteredSuppliers) { index, supplier ->
                        SupplierCard(
                            supplier = supplier,
                            colorIndex = index,
                            productCount = state.productCounts[supplier.id] ?: 0,
                            onClick = { onNavigateToForm(supplier.id) },
                            onDelete = { viewModel.delete(supplier) },
                            onNavigateToPurchase = onNavigateToPurchase,
                        )
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// SUPPLIER CARD (matches mock .sup)
// ═══════════════════════════════════════

@Composable
private fun SupplierCard(
    supplier: Supplier,
    colorIndex: Int,
    productCount: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onNavigateToPurchase: () -> Unit,
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_supplier_title)) },
            text = { Text(stringResource(R.string.delete_confirm_msg, supplier.name)) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text(stringResource(R.string.delete), color = AppColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        // ── Top row: icon + name/contact + badge ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Gradient icon (matches mock .sup-icon.s*)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                    .background(supplierIconGradient(colorIndex)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.LocalShipping,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            // Name + contact person
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = supplier.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                )
                if (!supplier.contactPerson.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.supplier_contact_prefix, supplier.contactPerson),
                        fontSize = 12.sp,
                        color = AppColors.TextTertiary,
                    )
                }
            }

            // Product count badge (matches mock .sup-badge)
            if (productCount > 0) {
                Text(
                    text = stringResource(R.string.supplier_products_count, productCount),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.PrimaryLight,
                    modifier = Modifier
                        .background(
                            AppColors.PrimaryContainer,
                            RoundedCornerShape(MiniPosTokens.RadiusFull),
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Details row: phone + email (matches mock .sup-details) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Phone detail
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Call,
                    contentDescription = null,
                    tint = AppColors.TextTertiary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = supplier.phone ?: stringResource(R.string.supplier_no_phone),
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary,
                )
            }
            // Email detail
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Mail,
                    contentDescription = null,
                    tint = AppColors.TextTertiary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = supplier.email ?: stringResource(R.string.supplier_no_email),
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Action buttons row (matches mock .sup-actions) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Call button
            SupplierActionButton(
                icon = Icons.Rounded.Call,
                label = stringResource(R.string.supplier_call_btn),
                modifier = Modifier.weight(1f),
                onClick = {
                    supplier.phone?.let { phone ->
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                        context.startActivity(intent)
                    }
                },
            )
            // Email button
            SupplierActionButton(
                icon = Icons.Rounded.Mail,
                label = stringResource(R.string.supplier_email_btn),
                modifier = Modifier.weight(1f),
                onClick = {
                    supplier.email?.let { email ->
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                        context.startActivity(intent)
                    }
                },
            )
            // Purchase order button
            SupplierActionButton(
                icon = Icons.Rounded.AddShoppingCart,
                label = stringResource(R.string.supplier_purchase_btn),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToPurchase,
            )
        }
    }
}

// ═══════════════════════════════════════
// SUPPLIER ACTION BUTTON (matches mock .sup-act)
// ═══════════════════════════════════════

@Composable
private fun SupplierActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
            .background(Color.Transparent)
            .then(
                Modifier
                    .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusMd))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = AppColors.TextSecondary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// SupplierFormDialog removed — now uses full-screen SupplierFormScreen via navigation
