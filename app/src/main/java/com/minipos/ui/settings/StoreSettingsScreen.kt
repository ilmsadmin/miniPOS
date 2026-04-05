package com.minipos.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.ui.components.*

@Composable
fun StoreSettingsScreen(
    onBack: () -> Unit,
    viewModel: StoreSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AppColors.Primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                // ── Top bar with back + save ──
                StoreSettingsTopBar(
                    onBack = onBack,
                    onSave = { viewModel.save() },
                )

                // ── Scrollable body ──
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // ── Store logo ──
                    StoreLogoSection(
                        storeName = state.storeName,
                        onChangeLogo = { /* TODO: image picker */ },
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ═══ Store Information ═══
                    SsSecLabel(stringResource(R.string.ss_section_store_info))
                    Spacer(modifier = Modifier.height(12.dp))

                    SsField(
                        label = stringResource(R.string.ss_store_name_label),
                        required = true,
                    ) {
                        SsTextInput(
                            value = state.storeName,
                            onValueChange = { viewModel.updateStoreName(it) },
                            placeholder = stringResource(R.string.ss_store_name_hint),
                            fontWeight = FontWeight.Bold,
                            isError = state.nameError,
                        )
                    }

                    SsField(
                        label = stringResource(R.string.ss_address_label),
                    ) {
                        SsTextArea(
                            value = state.storeAddress,
                            onValueChange = { viewModel.updateStoreAddress(it) },
                            placeholder = stringResource(R.string.ss_address_hint),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SsField(
                            label = stringResource(R.string.ss_phone_label),
                            modifier = Modifier.weight(1f),
                        ) {
                            SsTextInputWithIcon(
                                value = state.storePhone,
                                onValueChange = { viewModel.updateStorePhone(it) },
                                icon = Icons.Rounded.Call,
                                keyboardType = KeyboardType.Phone,
                            )
                        }
                        SsField(
                            label = stringResource(R.string.ss_email_label),
                            modifier = Modifier.weight(1f),
                        ) {
                            SsTextInputWithIcon(
                                value = state.storeEmail,
                                onValueChange = { viewModel.updateStoreEmail(it) },
                                icon = Icons.Rounded.Email,
                                keyboardType = KeyboardType.Email,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ═══ Tax & Currency ═══
                    SsSecLabel(stringResource(R.string.ss_section_tax_currency))
                    Spacer(modifier = Modifier.height(12.dp))

                    SsSelectorList {
                        SsSelectorItem(
                            icon = Icons.Rounded.Receipt,
                            iconGradient = listOf(Color(0xFF00D2FF), Color(0xFF3B9FDB)),
                            name = stringResource(R.string.ss_default_vat),
                            desc = stringResource(R.string.ss_vat_desc),
                            value = if (state.taxEnabled) "${state.defaultTaxRate.toInt()}%" else "0%",
                            onClick = { /* TODO: tax rate picker */ },
                        )
                        SsSelectorDivider()
                        SsSelectorItem(
                            icon = Icons.Rounded.CurrencyExchange,
                            iconGradient = listOf(Color(0xFFFFD54F), Color(0xFFF9A825)),
                            name = stringResource(R.string.ss_currency_unit),
                            desc = stringResource(R.string.ss_currency_vnd),
                            value = "VNĐ",
                            onClick = { /* TODO: currency picker */ },
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ═══ Receipt ═══
                    SsSecLabel(stringResource(R.string.ss_section_receipt))
                    Spacer(modifier = Modifier.height(12.dp))

                    SsToggleRow(
                        label = stringResource(R.string.ss_auto_print_receipt),
                        desc = stringResource(R.string.ss_auto_print_desc),
                        checked = state.autoPrintReceipt,
                        onToggle = { viewModel.toggleAutoPrintReceipt() },
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SsToggleRow(
                        label = stringResource(R.string.ss_show_logo_receipt),
                        desc = stringResource(R.string.ss_show_logo_desc),
                        checked = state.showLogoOnReceipt,
                        onToggle = { viewModel.toggleShowLogoOnReceipt() },
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SsField(
                        label = stringResource(R.string.ss_receipt_thank_you),
                    ) {
                        SsTextInput(
                            value = state.receiptThankYou,
                            onValueChange = { viewModel.updateReceiptThankYou(it) },
                            placeholder = "Cảm ơn quý khách! Hẹn gặp lại! 🙏",
                        )
                        Text(
                            stringResource(R.string.ss_receipt_thank_you_hint),
                            fontSize = 11.sp,
                            color = AppColors.TextTertiary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    // Receipt preview
                    ReceiptPreview(
                        storeName = state.storeName,
                        storeAddress = state.storeAddress,
                        storePhone = state.storePhone,
                        thankYou = state.receiptThankYou,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        stringResource(R.string.ss_receipt_preview_hint),
                        fontSize = 11.sp,
                        color = AppColors.TextTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ═══ Business Rules ═══
                    SsSecLabel(stringResource(R.string.ss_section_business))
                    Spacer(modifier = Modifier.height(12.dp))

                    SsToggleRow(
                        label = stringResource(R.string.ss_allow_debt),
                        desc = stringResource(R.string.ss_allow_debt_desc),
                        checked = state.allowDebt,
                        onToggle = { viewModel.toggleAllowDebt() },
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SsToggleRow(
                        label = stringResource(R.string.ss_low_stock_warning),
                        desc = stringResource(R.string.ss_low_stock_desc),
                        checked = state.lowStockAlert,
                        onToggle = { viewModel.toggleLowStockAlert() },
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SsToggleRow(
                        label = stringResource(R.string.ss_sales_sound),
                        desc = stringResource(R.string.ss_sales_sound_desc),
                        checked = state.salesSound,
                        onToggle = { viewModel.toggleSalesSound() },
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SsField(
                        label = stringResource(R.string.ss_default_low_stock_level),
                    ) {
                        SsTextInput(
                            value = state.defaultLowStockLevel,
                            onValueChange = { viewModel.updateDefaultLowStockLevel(it) },
                            keyboardType = KeyboardType.Number,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.ss_default_low_stock_hint),
                            fontSize = 11.sp,
                            color = AppColors.TextTertiary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ═══ Operational Hours ═══
                    SsSecLabel(stringResource(R.string.ss_section_hours))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SsField(
                            label = stringResource(R.string.ss_open_time),
                            modifier = Modifier.weight(1f),
                        ) {
                            SsTextInput(
                                value = state.openTime,
                                onValueChange = { viewModel.updateOpenTime(it) },
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        SsField(
                            label = stringResource(R.string.ss_close_time),
                            modifier = Modifier.weight(1f),
                        ) {
                            SsTextInput(
                                value = state.closeTime,
                                onValueChange = { viewModel.updateCloseTime(it) },
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// Top bar
// ═══════════════════════════════════════════════════

@Composable
private fun StoreSettingsTopBar(
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = AppColors.TextSecondary,
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            stringResource(R.string.store_settings_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        // Save button – gradient pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
                .background(MiniPosGradients.primary())
                .clickable(onClick = onSave)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(MiniPosTokens.RadiusFull),
                    ambientColor = AppColors.Primary.copy(alpha = 0.3f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.store_settings_save),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
        }
    }
}

// ═══════════════════════════════════════════════════
// Store logo section
// ═══════════════════════════════════════════════════

@Composable
private fun StoreLogoSection(
    storeName: String,
    onChangeLogo: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(MiniPosTokens.RadiusXl),
                    ambientColor = AppColors.BrandNavy.copy(alpha = 0.4f),
                )
                .background(MiniPosGradients.brand())
                .clickable(onClick = onChangeLogo),
            contentAlignment = Alignment.Center,
        ) {
            // Store icon or first letter
            if (storeName.isNotBlank()) {
                Text(
                    storeName.first().uppercase(),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                )
            } else {
                Icon(
                    Icons.Rounded.Storefront,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }

            // Camera edit badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MiniPosGradients.primary())
                    .border(2.dp, AppColors.Background, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PhotoCamera,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            stringResource(R.string.store_logo_hint),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextTertiary,
        )
    }
}

// ═══════════════════════════════════════════════════
// Section label
// ═══════════════════════════════════════════════════

@Composable
private fun SsSecLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        color = AppColors.TextTertiary,
        letterSpacing = 0.8.sp,
    )
}

// ═══════════════════════════════════════════════════
// Field wrapper
// ═══════════════════════════════════════════════════

@Composable
private fun SsField(
    label: String,
    required: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.padding(bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextSecondary,
            )
            if (required) {
                Spacer(modifier = Modifier.width(4.dp))
                Text("*", fontSize = 12.sp, color = AppColors.Error, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}

// ═══════════════════════════════════════════════════
// Text input
// ═══════════════════════════════════════════════════

@Composable
private fun SsTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    fontWeight: FontWeight = FontWeight.Normal,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
) {
    val borderColor = if (isError) AppColors.Error else AppColors.Border

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = AppColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = fontWeight,
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(AppColors.InputBackground, RoundedCornerShape(MiniPosTokens.RadiusLg))
                    .border(1.dp, borderColor, RoundedCornerShape(MiniPosTokens.RadiusLg))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        placeholder,
                        color = AppColors.TextTertiary,
                        fontSize = 14.sp,
                    )
                }
                innerTextField()
            }
        },
    )
}

// ═══════════════════════════════════════════════════
// Text area
// ═══════════════════════════════════════════════════

@Composable
private fun SsTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = AppColors.TextPrimary,
            fontSize = 14.sp,
        ),
        maxLines = 3,
        keyboardOptions = KeyboardOptions.Default,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp)
                    .background(AppColors.InputBackground, RoundedCornerShape(MiniPosTokens.RadiusLg))
                    .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        placeholder,
                        color = AppColors.TextTertiary,
                        fontSize = 14.sp,
                    )
                }
                innerTextField()
            }
        },
    )
}

// ═══════════════════════════════════════════════════
// Text input with icon
// ═══════════════════════════════════════════════════

@Composable
private fun SsTextInputWithIcon(
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = AppColors.TextPrimary,
            fontSize = 14.sp,
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(AppColors.InputBackground, RoundedCornerShape(MiniPosTokens.RadiusLg))
                    .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
                    .padding(start = 14.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = AppColors.TextTertiary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    innerTextField()
                }
            }
        },
    )
}

// ═══════════════════════════════════════════════════
// Toggle row
// ═══════════════════════════════════════════════════

@Composable
private fun SsToggleRow(
    label: String,
    desc: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
            )
            Text(
                desc,
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedTrackColor = AppColors.Primary,
            ),
        )
    }
}

// ═══════════════════════════════════════════════════
// Selector list (Tax & Currency)
// ═══════════════════════════════════════════════════

@Composable
private fun SsSelectorList(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg)),
        content = content,
    )
}

@Composable
private fun SsSelectorDivider() {
    HorizontalDivider(color = AppColors.Divider)
}

@Composable
private fun SsSelectorItem(
    icon: ImageVector,
    iconGradient: List<Color>,
    name: String,
    desc: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon with gradient bg
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                .background(Brush.linearGradient(iconGradient)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
            )
            Text(
                desc,
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
            )
        }
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextTertiary,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = AppColors.TextTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ═══════════════════════════════════════════════════
// Receipt preview card
// ═══════════════════════════════════════════════════

@Composable
private fun ReceiptPreview(
    storeName: String,
    storeAddress: String,
    storePhone: String,
    thankYou: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Store name
        Text(
            storeName.ifBlank { "Mini POS" },
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = AppColors.TextPrimary,
        )

        // Address
        if (storeAddress.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                storeAddress,
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
                textAlign = TextAlign.Center,
            )
        }

        // Phone
        if (storePhone.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "ĐT: $storePhone",
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dashed divider
        ReceiptDashedDivider()

        Spacer(modifier = Modifier.height(8.dp))

        // Label
        Text(
            stringResource(R.string.ss_receipt_preview_label),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextTertiary,
            letterSpacing = 0.5.sp,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Sample items
        ReceiptSampleRow("Coca Cola 330ml x2", "20,000đ")
        ReceiptSampleRow("Mì Hảo Hảo x1", "5,000đ")

        Spacer(modifier = Modifier.height(4.dp))

        ReceiptDashedDivider()

        Spacer(modifier = Modifier.height(4.dp))

        // Total
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.ss_receipt_total),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.TextPrimary,
            )
            Text(
                "25,000đ",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.TextPrimary,
            )
        }

        // Thank you
        if (thankYou.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                thankYou,
                fontSize = 10.sp,
                color = AppColors.TextTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ReceiptSampleRow(left: String, right: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(left, fontSize = 11.sp, color = AppColors.TextSecondary)
        Text(right, fontSize = 11.sp, color = AppColors.TextSecondary)
    }
}

@Composable
private fun ReceiptDashedDivider() {
    // Simple dashed-line effect using a row of dots
    Text(
        "- - - - - - - - - - - - - - - - - - - -",
        fontSize = 10.sp,
        color = AppColors.BorderLight,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
        maxLines = 1,
    )
}
