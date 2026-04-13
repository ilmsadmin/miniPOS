package com.minipos.ui.customer

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.minipos.ui.components.ConfirmButtonStyle
import com.minipos.ui.components.MiniPosConfirmDialog
import com.minipos.ui.components.PopupType
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.ui.components.*

// ═══════════════════════════════════════════════════════════════
// CUSTOMER FORM SCREEN — Full-screen create/edit matching HTML mock
// ═══════════════════════════════════════════════════════════════

@Composable
fun CustomerFormScreen(
    customerId: String? = null,
    onBack: () -> Unit,
    viewModel: CustomerListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val isEditing = customerId != null

    // Load customer for edit
    LaunchedEffect(customerId) {
        if (customerId != null) {
            viewModel.loadCustomerForEdit(customerId)
        } else {
            viewModel.initNewCustomerForm()
        }
    }

    val formState by viewModel.customerFormState.collectAsState()

    // Listen for save success
    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect { onBack() }
    }

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
                text = if (isEditing) stringResource(R.string.edit_customer) else stringResource(R.string.add_customer_btn),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            // Save button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
                    .background(MiniPosGradients.primary())
                    .clickable(enabled = formState.name.isNotBlank()) {
                        viewModel.saveCustomerForm()
                    }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.save),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
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

            // ─── Avatar ───
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF0E9AA0), Color(0xFF5AEDC5)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = formState.initials,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                    )
                    // Camera badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MiniPosGradients.primary())
                            .border(2.dp, AppColors.Background, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.cf_tap_change_photo), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextTertiary)
            }

            Spacer(Modifier.height(24.dp))

            // ═══ BASIC INFO SECTION ═══
            FormSectionLabel(stringResource(R.string.pf_section_basic))

            FormTextField(
                value = formState.name,
                onValueChange = { viewModel.updateCustomerForm(formState.copy(name = it)) },
                label = stringResource(R.string.customer_name_required),
                placeholder = "e.g. John Doe",
                required = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FormTextField(
                    value = formState.phone,
                    onValueChange = { viewModel.updateCustomerForm(formState.copy(phone = it)) },
                    label = stringResource(R.string.phone_number),
                    placeholder = "0901 234 567",
                    keyboardType = KeyboardType.Phone,
                    modifier = Modifier.weight(1f),
                )
                FormTextField(
                    value = formState.email,
                    onValueChange = { viewModel.updateCustomerForm(formState.copy(email = it)) },
                    label = stringResource(R.string.email_label),
                    placeholder = "email@example.com",
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(20.dp))

            // ═══ CUSTOMER TYPE SECTION ═══
            FormSectionLabel(stringResource(R.string.cf_customer_type))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CustomerTypeChip(
                    label = stringResource(R.string.cf_type_individual),
                    icon = Icons.Rounded.Person,
                    selected = formState.customerType == "individual",
                    onClick = { viewModel.updateCustomerForm(formState.copy(customerType = "individual")) },
                    modifier = Modifier.weight(1f),
                )
                CustomerTypeChip(
                    label = stringResource(R.string.cf_type_wholesale),
                    icon = Icons.Rounded.Store,
                    selected = formState.customerType == "wholesale",
                    onClick = { viewModel.updateCustomerForm(formState.copy(customerType = "wholesale")) },
                    modifier = Modifier.weight(1f),
                )
                CustomerTypeChip(
                    label = stringResource(R.string.cf_type_vip),
                    icon = Icons.Rounded.Diamond,
                    selected = formState.customerType == "vip",
                    onClick = { viewModel.updateCustomerForm(formState.copy(customerType = "vip")) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Customer type affects discounts & offers",
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
            )

            Spacer(Modifier.height(20.dp))

            // ═══ ADDRESS SECTION ═══
            FormSectionLabel(stringResource(R.string.address_label))

            FormTextArea(
                value = formState.address,
                onValueChange = { viewModel.updateCustomerForm(formState.copy(address = it)) },
                label = stringResource(R.string.cf_delivery_address),
                placeholder = "Street, ward, district, city...",
            )

            Spacer(Modifier.height(20.dp))

            // ═══ DEBT MANAGEMENT SECTION ═══
            FormSectionLabel(stringResource(R.string.cf_debt_management))

            // Debt card
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
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.cf_current_debt), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                    Text("0đ", fontSize = 18.sp, fontWeight = FontWeight.Black, color = AppColors.Error)
                }
                Spacer(Modifier.height(8.dp))
                Text("Enter initial debt amount (if any)", fontSize = 11.sp, color = AppColors.TextTertiary)
            }

            Spacer(Modifier.height(12.dp))

            // Allow debt toggle
            FormToggleRow(
                title = stringResource(R.string.cf_allow_credit),
                description = stringResource(R.string.cf_allow_credit_desc),
                checked = formState.allowDebt,
                onCheckedChange = { viewModel.updateCustomerForm(formState.copy(allowDebt = it)) },
            )

            Spacer(Modifier.height(12.dp))

            FormTextField(
                value = formState.debtLimit,
                onValueChange = { viewModel.updateCustomerForm(formState.copy(debtLimit = it)) },
                label = stringResource(R.string.cf_credit_limit),
                placeholder = "e.g. 500,000",
                keyboardType = KeyboardType.Number,
            )
            Text(
                stringResource(R.string.cf_credit_limit_hint),
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(20.dp))

            // ═══ NOTES SECTION ═══
            FormSectionLabel(stringResource(R.string.notes))

            FormTextArea(
                value = formState.notes,
                onValueChange = { viewModel.updateCustomerForm(formState.copy(notes = it)) },
                label = "",
                placeholder = "Additional notes about the customer (optional)...",
            )

            Spacer(Modifier.height(12.dp))

            // Active toggle
            FormToggleRow(
                title = stringResource(R.string.cf_active),
                description = stringResource(R.string.cf_active_desc),
                checked = formState.isActive,
                onCheckedChange = { viewModel.updateCustomerForm(formState.copy(isActive = it)) },
            )

            // Delete button (only in edit mode)
            if (isEditing) {
                Spacer(Modifier.height(12.dp))
                FormDeleteButton(
                    text = stringResource(R.string.cf_delete_customer),
                    onClick = { viewModel.deleteCustomerFromForm() },
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// CUSTOMER TYPE CHIP
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CustomerTypeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) AppColors.Primary else AppColors.Border
    val bgColor = if (selected) AppColors.Primary.copy(alpha = 0.08f) else AppColors.Surface
    val contentColor = if (selected) AppColors.PrimaryLight else AppColors.TextSecondary

    Row(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = contentColor)
    }
}

// ═══════════════════════════════════════════════════════════════
// SHARED FORM COMPONENTS
// ═══════════════════════════════════════════════════════════════

@Composable
fun FormSectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        color = AppColors.TextTertiary,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    required: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
) {
    Column(modifier = modifier.padding(bottom = 12.dp)) {
        if (label.isNotBlank()) {
            Row(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary)
                if (required) {
                    Text(" *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.Error)
                }
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = AppColors.TextTertiary) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            readOnly = readOnly,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(MiniPosTokens.RadiusLg),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.Primary,
                unfocusedBorderColor = AppColors.Border,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = AppColors.InputBackground,
                cursorColor = AppColors.Primary,
            ),
        )
    }
}

@Composable
fun FormTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = 12.dp)) {
        if (label.isNotBlank()) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = AppColors.TextTertiary) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp),
            maxLines = 4,
            shape = RoundedCornerShape(MiniPosTokens.RadiusLg),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.Primary,
                unfocusedBorderColor = AppColors.Border,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = AppColors.InputBackground,
                cursorColor = AppColors.Primary,
            ),
        )
    }
}

@Composable
fun FormToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            Text(description, fontSize = 11.sp, color = AppColors.TextTertiary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppColors.Primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = AppColors.InputBackground,
            ),
        )
    }
}

@Composable
fun FormDeleteButton(
    text: String,
    onClick: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }

    MiniPosConfirmDialog(
        visible = showConfirm,
        type = PopupType.DELETE,
        icon = Icons.Rounded.Delete,
        title = stringResource(R.string.confirm_delete),
        message = text,
        cancelText = stringResource(R.string.cancel),
        confirmText = stringResource(R.string.delete),
        confirmStyle = ConfirmButtonStyle.DANGER,
        onCancel = { showConfirm = false },
        onConfirm = { showConfirm = false; onClick() },
    )

    OutlinedButton(
        onClick = { showConfirm = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(MiniPosTokens.RadiusLg),
        border = BorderStroke(1.dp, AppColors.Error),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Error),
    ) {
        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
