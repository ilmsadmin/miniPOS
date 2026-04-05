package com.minipos.ui.supplier

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.ui.components.*
import com.minipos.ui.customer.FormDeleteButton
import com.minipos.ui.customer.FormSectionLabel
import com.minipos.ui.customer.FormTextArea
import com.minipos.ui.customer.FormTextField
import com.minipos.ui.customer.FormToggleRow

// ═══════════════════════════════════════════════════════════════
// SUPPLIER FORM SCREEN — Full-screen create/edit matching HTML mock
// ═══════════════════════════════════════════════════════════════

@Composable
fun SupplierFormScreen(
    supplierId: String? = null,
    onBack: () -> Unit,
    viewModel: SupplierListViewModel = hiltViewModel(),
) {
    val isEditing = supplierId != null

    LaunchedEffect(supplierId) {
        if (supplierId != null) {
            viewModel.loadSupplierForEdit(supplierId)
        } else {
            viewModel.initNewSupplierForm()
        }
    }

    val formState by viewModel.supplierFormState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect { onBack() }
    }

    // Payment term keys (for storage) and their display string resource IDs
    val paymentTermEntries = listOf(
        "Cash" to R.string.sf_payment_cash,
        "Bank Transfer" to R.string.sf_payment_transfer,
        "Credit 7 days" to R.string.sf_payment_credit_7,
        "Credit 15 days" to R.string.sf_payment_credit_15,
        "Credit 30 days" to R.string.sf_payment_credit_30,
    )

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
                text = if (isEditing) stringResource(R.string.edit_supplier) else stringResource(R.string.add_supplier_btn),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
                    .background(MiniPosGradients.primary())
                    .clickable(enabled = formState.name.isNotBlank()) {
                        viewModel.saveSupplierForm()
                    }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.save), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        // ─── Body ───
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ─── Logo ───
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
                        .background(Brush.linearGradient(listOf(Color(0xFFFF6B6B), Color(0xFFEE5A24)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.LocalShipping, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MiniPosGradients.primary())
                            .border(2.dp, AppColors.Background, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.sf_tap_change_logo), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextTertiary)
            }

            Spacer(Modifier.height(24.dp))

            // ═══ BASIC INFO SECTION ═══
            FormSectionLabel(stringResource(R.string.pf_section_basic))

            FormTextField(
                value = formState.name,
                onValueChange = { viewModel.updateSupplierForm(formState.copy(name = it)) },
                label = stringResource(R.string.supplier_name_required),
                placeholder = stringResource(R.string.sf_supplier_name_hint),
                required = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FormTextField(
                    value = formState.code,
                    onValueChange = { },
                    label = stringResource(R.string.sf_supplier_code),
                    placeholder = "Auto",
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                )
                FormTextField(
                    value = formState.taxCode,
                    onValueChange = { viewModel.updateSupplierForm(formState.copy(taxCode = it)) },
                    label = stringResource(R.string.tax_code),
                    placeholder = "MST",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(20.dp))

            // ═══ CONTACT SECTION ═══
            FormSectionLabel(stringResource(R.string.sf_section_contact))

            FormTextField(
                value = formState.contactPerson,
                onValueChange = { viewModel.updateSupplierForm(formState.copy(contactPerson = it)) },
                label = stringResource(R.string.contact_label),
                placeholder = stringResource(R.string.sf_contact_person_hint),
                required = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FormTextField(
                    value = formState.phone,
                    onValueChange = { viewModel.updateSupplierForm(formState.copy(phone = it)) },
                    label = stringResource(R.string.phone_number),
                    placeholder = stringResource(R.string.sf_phone_hint),
                    keyboardType = KeyboardType.Phone,
                    required = true,
                    modifier = Modifier.weight(1f),
                )
                FormTextField(
                    value = formState.mobile,
                    onValueChange = { viewModel.updateSupplierForm(formState.copy(mobile = it)) },
                    label = stringResource(R.string.sf_mobile),
                    placeholder = stringResource(R.string.sf_mobile_hint),
                    keyboardType = KeyboardType.Phone,
                    modifier = Modifier.weight(1f),
                )
            }

            FormTextField(
                value = formState.email,
                onValueChange = { viewModel.updateSupplierForm(formState.copy(email = it)) },
                label = stringResource(R.string.email_label),
                placeholder = stringResource(R.string.sf_email_hint),
                keyboardType = KeyboardType.Email,
            )

            FormTextArea(
                value = formState.address,
                onValueChange = { viewModel.updateSupplierForm(formState.copy(address = it)) },
                label = stringResource(R.string.address_label),
                placeholder = stringResource(R.string.sf_address_hint),
            )

            Spacer(Modifier.height(20.dp))

            // ═══ PAYMENT TERMS SECTION ═══
            FormSectionLabel(stringResource(R.string.sf_payment_terms))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                paymentTermEntries.take(2).forEach { (key, labelRes) ->
                    PaymentTermChip(
                        label = stringResource(labelRes),
                        selected = formState.paymentTerm == key,
                        onClick = { viewModel.updateSupplierForm(formState.copy(paymentTerm = key)) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                paymentTermEntries.drop(2).forEach { (key, labelRes) ->
                    PaymentTermChip(
                        label = stringResource(labelRes),
                        selected = formState.paymentTerm == key,
                        onClick = { viewModel.updateSupplierForm(formState.copy(paymentTerm = key)) },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.sf_payment_hint),
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(Modifier.height(20.dp))

            // ═══ BANKING INFO SECTION ═══
            FormSectionLabel(stringResource(R.string.sf_section_banking))

            FormTextField(
                value = formState.bankName,
                onValueChange = { viewModel.updateSupplierForm(formState.copy(bankName = it)) },
                label = stringResource(R.string.sf_bank_name),
                placeholder = stringResource(R.string.sf_bank_name_hint),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FormTextField(
                    value = formState.bankAccount,
                    onValueChange = { viewModel.updateSupplierForm(formState.copy(bankAccount = it)) },
                    label = stringResource(R.string.sf_bank_account),
                    placeholder = stringResource(R.string.sf_bank_account_hint),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                FormTextField(
                    value = formState.bankAccountHolder,
                    onValueChange = { viewModel.updateSupplierForm(formState.copy(bankAccountHolder = it)) },
                    label = stringResource(R.string.sf_bank_holder),
                    placeholder = stringResource(R.string.sf_bank_holder_hint),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(20.dp))

            // ═══ NOTES SECTION ═══
            FormSectionLabel(stringResource(R.string.notes))

            FormTextArea(
                value = formState.notes,
                onValueChange = { viewModel.updateSupplierForm(formState.copy(notes = it)) },
                label = "",
                placeholder = stringResource(R.string.sf_notes_hint),
            )

            Spacer(Modifier.height(12.dp))

            // ─── Active toggle ───
            FormToggleRow(
                title = stringResource(R.string.sf_active),
                description = stringResource(R.string.sf_active_desc),
                checked = formState.isActive,
                onCheckedChange = { viewModel.updateSupplierForm(formState.copy(isActive = it)) },
            )

            // ─── Delete button (only in edit mode) ───
            if (isEditing) {
                Spacer(Modifier.height(12.dp))
                FormDeleteButton(
                    text = stringResource(R.string.sf_delete_supplier),
                    onClick = { viewModel.deleteSupplierFromForm() },
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PAYMENT TERM CHIP
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PaymentTermChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) AppColors.Primary else AppColors.Border
    val bgColor = if (selected) AppColors.Primary.copy(alpha = 0.08f) else AppColors.Surface
    val textColor = if (selected) AppColors.PrimaryLight else AppColors.TextSecondary

    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(MiniPosTokens.RadiusFull))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
