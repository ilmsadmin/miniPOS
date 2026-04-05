package com.minipos.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.domain.model.StoreSettings
import com.minipos.domain.model.User
import com.minipos.domain.model.UserRole
import com.minipos.ui.components.*

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToStoreSettings: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show messages
    LaunchedEffect(state.message) {
        state.message?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    // Store Info Dialog
    if (state.showStoreInfoDialog) {
        StoreInfoDialog(
            store = state.store,
            ownerUser = state.currentUser,
            ownerHasPin = state.currentUserHasPin,
            onDismiss = { viewModel.dismissStoreInfoDialog() },
            onSave = { name, address, phone, ownerName, currentPin, newPin ->
                viewModel.updateStoreInfo(name, address, phone)
                if (ownerName.isNotBlank()) viewModel.updateOwnerName(ownerName)
                if (newPin.isNotBlank()) viewModel.updateOwnerPin(currentPin, newPin)
            },
        )
    }

    // Sales Settings Dialog
    if (state.showSalesSettingsDialog) {
        SalesSettingsDialog(
            settings = state.store?.settings ?: StoreSettings(),
            onDismiss = { viewModel.dismissSalesSettingsDialog() },
            onSave = { viewModel.updateSalesSettings(it) },
        )
    }

    // User Management Sheet
    if (state.showUserManagementSheet) {
        UserManagementDialog(
            users = state.users,
            currentUserId = state.currentUser?.id,
            onDismiss = { viewModel.dismissUserManagement() },
            onAddUser = { viewModel.showAddUserDialog() },
            onEditUser = { viewModel.showEditUserDialog(it) },
            onResetPin = { viewModel.showResetPinDialog(it) },
            onDeleteUser = { viewModel.showDeleteUserConfirm(it) },
        )
    }

    // Add User Dialog
    if (state.showAddUserDialog) {
        AddUserDialog(
            onDismiss = { viewModel.dismissAddUserDialog() },
            onSave = { name, pin, role -> viewModel.addUser(name, pin, role) },
        )
    }

    // Edit User Dialog
    state.showEditUserDialog?.let { user ->
        EditUserDialog(
            user = user,
            onDismiss = { viewModel.dismissEditUserDialog() },
            onSave = { viewModel.updateUser(it) },
        )
    }

    // Reset PIN Dialog
    state.showResetPinDialog?.let { user ->
        ResetPinDialog(
            user = user,
            onDismiss = { viewModel.dismissResetPinDialog() },
            onSave = { newPin -> viewModel.resetUserPin(user.id, newPin) },
        )
    }

    // Delete User Confirm
    state.showDeleteUserConfirm?.let { user ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteUserConfirm() },
            title = { Text(stringResource(R.string.delete_user_title)) },
            text = { Text(stringResource(R.string.delete_user_confirm_msg, user.displayName)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteUser(user.id) }) {
                    Text(stringResource(R.string.delete_btn), color = AppColors.Error)
                }
            },
            dismissButton = { TextButton(onClick = { viewModel.dismissDeleteUserConfirm() }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    // Backup info dialog
    if (state.showBackupDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBackupDialog() },
            title = { Text(stringResource(R.string.backup_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.backup_msg))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.backup_msg_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.dismissBackupDialog() }) { Text(stringResource(R.string.understood)) } },
        )
    }

    // Restore info dialog
    if (state.showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestoreDialog() },
            title = { Text(stringResource(R.string.restore_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.restore_msg))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.restore_msg_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.dismissRestoreDialog() }) { Text(stringResource(R.string.understood)) } },
        )
    }

    Scaffold(
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.Primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                MiniPosTopBar(
                    title = stringResource(R.string.settings_label),
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = onNavigateToStoreSettings) {
                            Icon(
                                Icons.Rounded.Storefront,
                                contentDescription = stringResource(R.string.store_settings_title),
                                tint = AppColors.TextSecondary,
                            )
                        }
                    },
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                // ─── Profile card (like mock's .profile-card) ───
                item {
                    state.currentUser?.let { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
                                .background(AppColors.Surface)
                                .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusXl))
                                .clickable { viewModel.showStoreInfoDialog() }
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Avatar with accent gradient
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MiniPosGradients.accent()),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    user.displayName.firstOrNull()?.uppercase() ?: "?",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    user.displayName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AppColors.TextPrimary,
                                )
                                Text(
                                    when (user.role) {
                                        UserRole.OWNER -> stringResource(R.string.role_owner)
                                        UserRole.MANAGER -> stringResource(R.string.role_manager)
                                        UserRole.CASHIER -> stringResource(R.string.role_cashier)
                                    },
                                    fontSize = 12.sp,
                                    color = AppColors.TextTertiary,
                                    fontWeight = FontWeight.Medium,
                                )
                                state.store?.let { store ->
                                    Text(
                                        store.name,
                                        fontSize = 11.sp,
                                        color = AppColors.PrimaryLight,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = AppColors.TextTertiary,
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // ─── Store settings group ───
                item { SettingsGroupTitle(stringResource(R.string.section_store)) }
                item {
                    SettingsGroup {
                        SettingsItemStyled(
                            icon = Icons.Rounded.Storefront,
                            iconGradient = listOf(Color(0xFF6C5CE7), Color(0xFFA29BFE)),
                            title = stringResource(R.string.store_info_label),
                            subtitle = stringResource(R.string.store_info_desc),
                            onClick = { onNavigateToStoreSettings() },
                        )
                        SettingsItemDivider()
                        val settings = state.store?.settings ?: StoreSettings()
                        SettingsItemStyled(
                            icon = Icons.Rounded.Receipt,
                            iconGradient = listOf(Color(0xFF00D2FF), Color(0xFF3B9FDB)),
                            title = stringResource(R.string.sales_settings_label),
                            subtitle = buildString {
                                if (settings.taxEnabled) append(stringResource(R.string.tax_enabled_format, settings.defaultTaxRate.toString()))
                                else append(stringResource(R.string.tax_disabled))
                            },
                            trailingValue = if (settings.taxEnabled) "${settings.defaultTaxRate.toInt()}%" else null,
                            onClick = { viewModel.showSalesSettingsDialog() },
                        )
                        SettingsItemDivider()
                        SettingsItemStyled(
                            icon = Icons.Rounded.Print,
                            iconGradient = listOf(Color(0xFFFF8A65), Color(0xFFFF5252)),
                            title = stringResource(R.string.bluetooth_printer_label),
                            subtitle = stringResource(R.string.bluetooth_not_connected),
                            onClick = { /* TODO: Bluetooth printer setup */ },
                        )
                        SettingsItemDivider()
                        SettingsItemStyled(
                            icon = Icons.Rounded.CloudSync,
                            iconGradient = listOf(Color(0xFF00E676), Color(0xFF69F0AE)),
                            title = stringResource(R.string.backup_label),
                            subtitle = stringResource(R.string.backup_desc),
                            onClick = { viewModel.showBackupDialog() },
                        )
                        SettingsItemDivider()
                        SettingsItemStyled(
                            icon = Icons.Rounded.ManageAccounts,
                            iconGradient = listOf(Color(0xFFFFD54F), Color(0xFFF9A825)),
                            title = stringResource(R.string.staff_label),
                            subtitle = stringResource(R.string.staff_count, state.users.size),
                            onClick = { viewModel.showUserManagement() },
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }

                // ─── Appearance group ───
                item { SettingsGroupTitle(stringResource(R.string.section_appearance)) }
                item {
                    SettingsGroup {
                        SettingsItemStyled(
                            icon = Icons.Rounded.DarkMode,
                            iconGradient = listOf(Color(0xFF0A0E1A), Color(0xFF334155)),
                            title = stringResource(R.string.dark_mode_label),
                            subtitle = stringResource(R.string.dark_mode_desc),
                            trailingContent = {
                                // Dark mode toggle would need integration with theme state
                                // For now, show a toggle placeholder
                                Switch(
                                    checked = true, // TODO: bind to actual theme state
                                    onCheckedChange = { /* TODO: toggle theme */ },
                                    colors = SwitchDefaults.colors(checkedTrackColor = AppColors.Primary),
                                )
                            },
                            onClick = { /* TODO: toggle theme */ },
                        )
                        SettingsItemDivider()
                        SettingsItemStyled(
                            icon = Icons.Rounded.Translate,
                            iconGradient = listOf(Color(0xFF4DD0E1), Color(0xFF0097A7)),
                            title = stringResource(R.string.language_label),
                            trailingValue = stringResource(R.string.language_vietnamese),
                            onClick = { /* TODO: language picker */ },
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }

                // ─── Other group ───
                item { SettingsGroupTitle(stringResource(R.string.section_other)) }
                item {
                    SettingsGroup {
                        SettingsItemStyled(
                            icon = Icons.Rounded.Help,
                            iconGradient = listOf(Color(0xFFCE93D8), Color(0xFFAB47BC)),
                            title = stringResource(R.string.help_feedback_label),
                            onClick = { /* TODO */ },
                        )
                        SettingsItemDivider()
                        SettingsItemStyled(
                            icon = Icons.Rounded.Info,
                            iconGradient = listOf(Color(0xFF90A4AE), Color(0xFF607D8B)),
                            title = stringResource(R.string.app_info_label),
                            subtitle = stringResource(R.string.version_value),
                            onClick = { /* TODO */ },
                        )
                    }
                }

                // Version footer
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row {
                            Text(
                                "Mini POS ",
                                fontSize = 12.sp,
                                color = AppColors.TextTertiary,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "v1.0.0",
                                fontSize = 12.sp,
                                color = AppColors.PrimaryLight,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Zenix Labs",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.BrandBlue,
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
        }
    }
}

// ============ Store Info Dialog ============

@Composable
private fun StoreInfoDialog(
    store: com.minipos.domain.model.Store?,
    ownerUser: com.minipos.domain.model.User?,
    ownerHasPin: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, address: String?, phone: String?, ownerName: String, currentPin: String, newPin: String) -> Unit,
) {
    var name by remember { mutableStateOf(store?.name ?: "") }
    var address by remember { mutableStateOf(store?.address ?: "") }
    var phone by remember { mutableStateOf(store?.phone ?: "") }
    var ownerName by remember { mutableStateOf(ownerUser?.displayName ?: "") }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }

    val hasExistingPin = ownerHasPin

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.store_info_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text(stringResource(R.string.store_name_label)) },
                    isError = nameError,
                    supportingText = if (nameError) {{ Text(stringResource(R.string.name_empty_error)) }} else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(MiniPosTokens.RadiusSm),
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(stringResource(R.string.address_label)) },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(MiniPosTokens.RadiusSm),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.store_phone_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(MiniPosTokens.RadiusSm),
                )
                HorizontalDivider()
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text(stringResource(R.string.display_name_required)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(MiniPosTokens.RadiusSm),
                )
                HorizontalDivider()
                // PIN section
                Text(
                    stringResource(R.string.pin_section_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextSecondary,
                )
                if (hasExistingPin) {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) { currentPin = it; pinError = null } },
                        label = { Text(stringResource(R.string.pin_current_label)) },
                        placeholder = { Text(stringResource(R.string.pin_current_hint)) },
                        isError = pinError != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(MiniPosTokens.RadiusSm),
                    )
                }
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) { newPin = it; pinError = null } },
                    label = { Text(stringResource(R.string.pin_new_label)) },
                    placeholder = { Text(stringResource(R.string.pin_leave_blank)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(MiniPosTokens.RadiusSm),
                )
                if (newPin.isNotBlank()) {
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) { confirmPin = it; pinError = null } },
                        label = { Text(stringResource(R.string.pin_confirm_label)) },
                        isError = pinError != null,
                        supportingText = pinError?.let {{ Text(pinError!!) }},
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(MiniPosTokens.RadiusSm),
                    )
                }
            }
        },
        confirmButton = {
            val pinLengthError = stringResource(R.string.pin_length_error)
            val pinMismatchError = stringResource(R.string.pin_mismatch_error)
            TextButton(onClick = {
                if (name.isBlank()) { nameError = true; return@TextButton }
                if (newPin.isNotBlank()) {
                    if (newPin.length < 4) { pinError = pinLengthError; return@TextButton }
                    if (newPin != confirmPin) { pinError = pinMismatchError; return@TextButton }
                }
                onSave(
                    name.trim(),
                    address.trim().ifBlank { null },
                    phone.trim().ifBlank { null },
                    ownerName.trim(),
                    currentPin,
                    newPin,
                )
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ============ Sales Settings Dialog ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SalesSettingsDialog(
    settings: StoreSettings,
    onDismiss: () -> Unit,
    onSave: (StoreSettings) -> Unit,
) {
    var taxEnabled by remember { mutableStateOf(settings.taxEnabled) }
    var taxRate by remember { mutableStateOf(settings.defaultTaxRate.toString()) }
    var receiptHeader by remember { mutableStateOf(settings.receiptHeader) }
    var receiptFooter by remember { mutableStateOf(settings.receiptFooter) }
    var lowStockAlert by remember { mutableStateOf(settings.lowStockAlert) }
    var autoPrintReceipt by remember { mutableStateOf(settings.autoPrintReceipt) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sales_settings_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Tax toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.enable_tax), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = taxEnabled,
                        onCheckedChange = { taxEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = AppColors.Primary),
                    )
                }

                // Tax rate
                if (taxEnabled) {
                    OutlinedTextField(
                        value = taxRate,
                        onValueChange = { taxRate = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.tax_rate_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text("%") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(MiniPosTokens.RadiusSm),
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Receipt header
                OutlinedTextField(
                    value = receiptHeader,
                    onValueChange = { receiptHeader = it },
                    label = { Text("Header hóa đơn") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(MiniPosTokens.RadiusSm),
                )

                // Receipt footer
                OutlinedTextField(
                    value = receiptFooter,
                    onValueChange = { receiptFooter = it },
                    label = { Text("Footer hóa đơn") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(MiniPosTokens.RadiusSm),
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Auto print receipt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.auto_print_receipt), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = autoPrintReceipt,
                        onCheckedChange = { autoPrintReceipt = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = AppColors.Primary),
                    )
                }

                // Low stock alert
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.low_stock_alert_label), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = lowStockAlert,
                        onCheckedChange = { lowStockAlert = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = AppColors.Primary),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val rate = taxRate.toDoubleOrNull() ?: 0.0
                onSave(
                    StoreSettings(
                        receiptHeader = receiptHeader,
                        receiptFooter = receiptFooter,
                        taxEnabled = taxEnabled,
                        defaultTaxRate = rate,
                        lowStockAlert = lowStockAlert,
                        autoPrintReceipt = autoPrintReceipt,
                    )
                )
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ============ User Management Dialog ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserManagementDialog(
    users: List<User>,
    currentUserId: String?,
    onDismiss: () -> Unit,
    onAddUser: () -> Unit,
    onEditUser: (User) -> Unit,
    onResetPin: (User) -> Unit,
    onDeleteUser: (User) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.user_management_title))
                IconButton(onClick = onAddUser) {
                    Icon(Icons.Default.PersonAdd, contentDescription = stringResource(R.string.add_staff), tint = AppColors.Primary)
                }
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp),
            ) {
                items(users) { user ->
                    UserCard(
                        user = user,
                        isCurrent = user.id == currentUserId,
                        onEdit = { onEditUser(user) },
                        onResetPin = { onResetPin(user) },
                        onDelete = { onDeleteUser(user) },
                    )
                }
                if (users.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.no_staff_yet),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun UserCard(
    user: User,
    isCurrent: Boolean,
    onEdit: () -> Unit,
    onResetPin: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(MiniPosTokens.RadiusMd),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) AppColors.Primary.copy(alpha = 0.08f) else AppColors.Surface
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = if (isCurrent) AppColors.Primary else AppColors.TextSecondary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        user.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.you_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.Primary,
                        )
                    }
                }
                Text(
                    when (user.role) {
                        UserRole.OWNER -> stringResource(R.string.role_owner)
                        UserRole.MANAGER -> stringResource(R.string.role_manager)
                        UserRole.CASHIER -> stringResource(R.string.role_cashier)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = AppColors.TextSecondary)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_label)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        onClick = { showMenu = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.reset_pin_label)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        onClick = { showMenu = false; onResetPin() },
                    )
                    if (!isCurrent && user.role != UserRole.OWNER) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete), color = AppColors.Error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AppColors.Error, modifier = Modifier.size(20.dp)) },
                            onClick = { showMenu = false; onDelete() },
                        )
                    }
                }
            }
        }
    }
}

// ============ Add User Dialog ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddUserDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, pin: String, role: UserRole) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.CASHIER) }
    var nameError by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_staff_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text(stringResource(R.string.staff_name_label)) },
                    isError = nameError,
                    supportingText = if (nameError) {{ Text(stringResource(R.string.name_empty_error)) }} else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(MiniPosTokens.RadiusSm),
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6); pinError = null },
                    label = { Text(stringResource(R.string.pin_4_6_label)) },
                    isError = pinError != null,
                    supportingText = pinError?.let {{ Text(pinError!!) }},
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(MiniPosTokens.RadiusSm),
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it.filter { c -> c.isDigit() }.take(6); pinError = null },
                    label = { Text(stringResource(R.string.confirm_password)) },
                    isError = pinError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(MiniPosTokens.RadiusSm),
                )

                Text(stringResource(R.string.role_selection), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniPosFilterChip(
                        label = stringResource(R.string.role_manager),
                        selected = selectedRole == UserRole.MANAGER,
                        onClick = { selectedRole = UserRole.MANAGER },
                    )
                    MiniPosFilterChip(
                        label = stringResource(R.string.role_cashier),
                        selected = selectedRole == UserRole.CASHIER,
                        onClick = { selectedRole = UserRole.CASHIER },
                    )
                }
            }
        },
        confirmButton = {
            val pinLengthError = stringResource(R.string.pin_length_error)
            val pinMismatchError = stringResource(R.string.pin_mismatch_error)
            TextButton(onClick = {
                when {
                    name.isBlank() -> nameError = true
                    pin.length < 4 -> pinError = pinLengthError
                    pin != confirmPin -> pinError = pinMismatchError
                    else -> onSave(name.trim(), pin, selectedRole)
                }
            }) { Text(stringResource(R.string.add_btn)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ============ Edit User Dialog ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditUserDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit,
) {
    var name by remember { mutableStateOf(user.displayName) }
    var selectedRole by remember { mutableStateOf(user.role) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_staff_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text(stringResource(R.string.edit_staff_name)) },
                    isError = nameError,
                    supportingText = if (nameError) {{ Text(stringResource(R.string.name_empty_error)) }} else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(MiniPosTokens.RadiusSm),
                )

                if (user.role != UserRole.OWNER) {
                    Text(stringResource(R.string.role_selection), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniPosFilterChip(
                            label = stringResource(R.string.role_manager),
                            selected = selectedRole == UserRole.MANAGER,
                            onClick = { selectedRole = UserRole.MANAGER },
                        )
                        MiniPosFilterChip(
                            label = stringResource(R.string.role_cashier),
                            selected = selectedRole == UserRole.CASHIER,
                            onClick = { selectedRole = UserRole.CASHIER },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    nameError = true
                } else {
                    onSave(user.copy(displayName = name.trim(), role = selectedRole))
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ============ Reset PIN Dialog ============

@Composable
private fun ResetPinDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reset_pin_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.reset_pin_for, user.displayName), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6); pinError = null },
                    label = { Text(stringResource(R.string.new_pin_label)) },
                    isError = pinError != null,
                    supportingText = pinError?.let {{ Text(pinError!!) }},
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(MiniPosTokens.RadiusSm),
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it.filter { c -> c.isDigit() }.take(6); pinError = null },
                    label = { Text(stringResource(R.string.confirm_password)) },
                    isError = pinError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(MiniPosTokens.RadiusSm),
                )
            }
        },
        confirmButton = {
            val pinLengthError = stringResource(R.string.pin_length_error)
            val pinMismatchError = stringResource(R.string.pin_mismatch_error)
            TextButton(onClick = {
                when {
                    pin.length < 4 -> pinError = pinLengthError
                    pin != confirmPin -> pinError = pinMismatchError
                    else -> onSave(pin)
                }
            }) { Text(stringResource(R.string.reset_btn)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ============ Shared Components (matching mock design) ============

@Composable
private fun SettingsGroupTitle(title: String) {
    Text(
        title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = AppColors.TextTertiary,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsGroup(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusXl)),
        content = content,
    )
}

@Composable
private fun SettingsItemDivider() {
    HorizontalDivider(
        color = AppColors.Divider,
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}

@Composable
private fun SettingsItemStyled(
    icon: ImageVector,
    iconGradient: List<Color>,
    title: String,
    subtitle: String? = null,
    trailingValue: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon with gradient background
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                .background(Brush.linearGradient(iconGradient)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = AppColors.TextTertiary,
                )
            }
        }
        if (trailingValue != null) {
            Text(
                trailingValue,
                fontSize = 12.sp,
                color = AppColors.TextTertiary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        if (trailingContent != null) {
            trailingContent()
        } else {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = AppColors.TextTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
