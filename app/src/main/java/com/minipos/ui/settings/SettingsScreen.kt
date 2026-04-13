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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.BuildConfig
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.core.theme.AppLanguage
import com.minipos.core.theme.ThemeMode
import com.minipos.domain.model.StoreSettings
import com.minipos.domain.model.User
import com.minipos.domain.model.UserRole
import com.minipos.core.backup.BackupFileInfo
import com.minipos.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToStoreSettings: () -> Unit = {},
    onNavigateToWifiSync: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Info dialog states
    var showBluetoothInfoDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAppInfoDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Bluetooth Printer Info Dialog
    MiniPosAlertDialog(
        visible = showBluetoothInfoDialog,
        type = PopupType.INFO,
        icon = Icons.Rounded.Print,
        title = stringResource(R.string.bluetooth_printer_info_title),
        message = stringResource(R.string.bluetooth_printer_info_msg),
        confirmText = stringResource(R.string.ok),
        onConfirm = { showBluetoothInfoDialog = false },
    )

    // Help & Feedback Dialog
    MiniPosAlertDialog(
        visible = showHelpDialog,
        type = PopupType.INFO,
        icon = Icons.Rounded.Help,
        title = stringResource(R.string.help_feedback_title),
        message = stringResource(R.string.help_feedback_msg),
        confirmText = stringResource(R.string.ok),
        onConfirm = { showHelpDialog = false },
    )

    // App Info Dialog
    MiniPosAlertDialog(
        visible = showAppInfoDialog,
        type = PopupType.INFO,
        icon = Icons.Rounded.Info,
        title = stringResource(R.string.app_info_title),
        message = stringResource(R.string.app_info_msg),
        confirmText = stringResource(R.string.ok),
        onConfirm = { showAppInfoDialog = false },
    )

    // Language Dialog — select list bottom sheet
    val currentLang by viewModel.themeManager.language.collectAsState()
    MiniPosSelectSheet(
        visible = showLanguageDialog,
        title = stringResource(R.string.language_label),
        selectedId = currentLang.key,
        items = AppLanguage.entries.map { lang ->
            SelectListItem(
                id = lang.key,
                name = when (lang) {
                    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
                    AppLanguage.ENGLISH -> "English"
                    AppLanguage.VIETNAMESE -> "Tiếng Việt"
                },
                description = when (lang) {
                    AppLanguage.SYSTEM -> stringResource(R.string.language_system_desc)
                    AppLanguage.ENGLISH -> "English (US)"
                    AppLanguage.VIETNAMESE -> "Tiếng Việt"
                },
                icon = when (lang) {
                    AppLanguage.SYSTEM -> Icons.Rounded.Settings
                    AppLanguage.ENGLISH -> Icons.Rounded.Translate
                    AppLanguage.VIETNAMESE -> Icons.Rounded.Translate
                },
            )
        },
        onSelect = { item ->
            val lang = AppLanguage.fromKey(item.id)
            viewModel.setLanguage(lang)
            showLanguageDialog = false
        },
        onDismiss = { showLanguageDialog = false },
    )

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
            onChangePin = { viewModel.dismissStoreInfoDialog(); viewModel.showChangePinDialog() },
            onChangePassword = { viewModel.dismissStoreInfoDialog(); viewModel.showChangePasswordDialog() },
            onSave = { name, address, phone, ownerName ->
                viewModel.updateStoreInfo(name, address, phone)
                if (ownerName.isNotBlank()) viewModel.updateOwnerName(ownerName)
            },
        )
    }

    // Change PIN Dialog (dedicated flow)
    if (state.showChangePinDialog) {
        ChangePinBottomSheet(
            hasExistingPin = state.currentUserHasPin,
            pinVerified = state.pinVerified,
            pinVerifyError = state.pinVerifyError,
            onVerifyPin = { viewModel.verifyCurrentPin(it) },
            onSaveNewPin = { viewModel.saveNewPin(it) },
            onDismiss = { viewModel.dismissChangePinDialog() },
        )
    }

    // Change Password Dialog (OWNER only)
    if (state.showChangePasswordDialog) {
        ChangePasswordBottomSheet(
            hasExistingPassword = state.currentUserHasPassword,
            onSave = { current, new -> viewModel.saveOwnerPassword(current, new) },
            onDismiss = { viewModel.dismissChangePasswordDialog() },
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
        MiniPosConfirmDialog(
            visible = true,
            type = PopupType.DELETE,
            icon = Icons.Rounded.PersonRemove,
            title = stringResource(R.string.delete_user_title),
            message = stringResource(R.string.delete_user_confirm_msg, user.displayName),
            cancelText = stringResource(R.string.cancel),
            confirmText = stringResource(R.string.delete_btn),
            confirmStyle = ConfirmButtonStyle.DANGER,
            onCancel = { viewModel.dismissDeleteUserConfirm() },
            onConfirm = { viewModel.deleteUser(user.id) },
        )
    }

    // ── Backup Dialog ──────────────────────────────────────────────────────
    if (state.showBackupDialog) {
        BackupDialog(
            isBackingUp = state.isBackingUp,
            backupFiles = state.backupFiles,
            onCreateBackup = { viewModel.createBackup() },
            onDeleteBackup = { viewModel.deleteBackupFile(it) },
            onDismiss = { viewModel.dismissBackupDialog() },
        )
    }

    // ── Restore Dialog ────────────────────────────────────────────────────
    if (state.showRestoreDialog) {
        RestoreDialog(
            isRestoring = state.isRestoring,
            backupFiles = state.backupFiles,
            confirmFile = state.restoreConfirmFile,
            onSelectFile = { viewModel.confirmRestoreFile(it) },
            onCancelConfirm = { viewModel.cancelRestoreConfirm() },
            onConfirmRestore = { viewModel.executeRestore(it) },
            onDismiss = { viewModel.dismissRestoreDialog() },
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
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // ─── Store settings group (OWNER only) ───
                val isOwner = state.currentUser?.role == UserRole.OWNER
                if (isOwner) {
                    item { SettingsGroupTitle(stringResource(R.string.section_store)) }
                    item {
                        SettingsGroup {
                            SettingsItemStyled(
                                icon = Icons.Rounded.Storefront,
                                iconGradient = listOf(Color(0xFF0E9AA0), Color(0xFF2EC4B6)),
                                title = stringResource(R.string.store_info_label),
                                subtitle = stringResource(R.string.store_info_desc),
                                onClick = { onNavigateToStoreSettings() },
                            )
                            SettingsItemDivider()
                            val settings = state.store?.settings ?: StoreSettings()
                            SettingsItemStyled(
                                icon = Icons.Rounded.Receipt,
                                iconGradient = listOf(Color(0xFF14B8B0), Color(0xFF5AEDC5)),
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
                                onClick = { showBluetoothInfoDialog = true },
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
                                icon = Icons.Rounded.Restore,
                                iconGradient = listOf(Color(0xFFFF8A65), Color(0xFFF4511E)),
                                title = stringResource(R.string.restore_label),
                                subtitle = stringResource(R.string.restore_desc),
                                onClick = { viewModel.showRestoreDialog() },
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
                }

                // ─── Appearance group ───
                item { SettingsGroupTitle(stringResource(R.string.section_appearance)) }
                item {
                    val currentThemeMode by viewModel.themeManager.themeMode.collectAsState()
                    val currentLanguage by viewModel.themeManager.language.collectAsState()
                    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
                    val isDark = when (currentThemeMode) {
                        com.minipos.core.theme.ThemeMode.SYSTEM -> isSystemDark
                        com.minipos.core.theme.ThemeMode.LIGHT -> false
                        com.minipos.core.theme.ThemeMode.DARK -> true
                    }
                    SettingsGroup {
                        SettingsItemStyled(
                            icon = Icons.Rounded.DarkMode,
                            iconGradient = listOf(Color(0xFF0A0E1A), Color(0xFF334155)),
                            title = stringResource(R.string.dark_mode_label),
                            subtitle = stringResource(R.string.dark_mode_desc),
                            trailingContent = {
                                Switch(
                                    checked = isDark,
                                    onCheckedChange = { enabled ->
                                        viewModel.setThemeMode(
                                            if (enabled) com.minipos.core.theme.ThemeMode.DARK
                                            else com.minipos.core.theme.ThemeMode.LIGHT,
                                        )
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = AppColors.Primary),
                                )
                            },
                            onClick = {
                                viewModel.setThemeMode(
                                    if (isDark) com.minipos.core.theme.ThemeMode.LIGHT
                                    else com.minipos.core.theme.ThemeMode.DARK,
                                )
                            },
                        )
                        SettingsItemDivider()
                        SettingsItemStyled(
                            icon = Icons.Rounded.Translate,
                            iconGradient = listOf(Color(0xFF5AEDC5), Color(0xFF0097A7)),
                            title = stringResource(R.string.language_label),
                            trailingValue = when (currentLanguage) {
                                com.minipos.core.theme.AppLanguage.SYSTEM -> stringResource(R.string.language_system)
                                com.minipos.core.theme.AppLanguage.ENGLISH -> "English"
                                com.minipos.core.theme.AppLanguage.VIETNAMESE -> "Tiếng Việt"
                            },
                            onClick = { showLanguageDialog = true },
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }

                // ─── Other group ───
                item { SettingsGroupTitle(stringResource(R.string.section_other)) }
                item {
                    SettingsGroup {
                        SettingsItemStyled(
                            icon = Icons.Rounded.Wifi,
                            iconGradient = listOf(Color(0xFF0097A7), Color(0xFF5AEDC5)),
                            title = stringResource(R.string.wifi_sync_label),
                            subtitle = stringResource(R.string.wifi_sync_desc),
                            onClick = onNavigateToWifiSync,
                        )
                        SettingsItemDivider()
                        SettingsItemStyled(
                            icon = Icons.Rounded.Help,
                            iconGradient = listOf(Color(0xFFCE93D8), Color(0xFFAB47BC)),
                            title = stringResource(R.string.help_feedback_label),
                            onClick = { showHelpDialog = true },
                        )
                        SettingsItemDivider()
                        SettingsItemStyled(
                            icon = Icons.Rounded.Info,
                            iconGradient = listOf(Color(0xFF90A4AE), Color(0xFF607D8B)),
                            title = stringResource(R.string.app_info_label),
                            subtitle = "ViPOS v${BuildConfig.VERSION_NAME}",
                            onClick = { showAppInfoDialog = true },
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
                                "ViPOS ",
                                fontSize = 12.sp,
                                color = AppColors.TextTertiary,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "v${BuildConfig.VERSION_NAME}",
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

// ============ Store Info Dialog → Bottom Sheet ============

@Composable
private fun StoreInfoDialog(
    store: com.minipos.domain.model.Store?,
    ownerUser: com.minipos.domain.model.User?,
    ownerHasPin: Boolean,
    onDismiss: () -> Unit,
    onChangePin: () -> Unit,
    onChangePassword: () -> Unit,
    onSave: (name: String, address: String?, phone: String?, ownerName: String) -> Unit,
) {
    var name by remember { mutableStateOf(store?.name ?: "") }
    var address by remember { mutableStateOf(store?.address ?: "") }
    var phone by remember { mutableStateOf(store?.phone ?: "") }
    var ownerName by remember { mutableStateOf(ownerUser?.displayName ?: "") }
    var nameError by remember { mutableStateOf(false) }

    MiniPosBottomSheet(
        visible = true,
        title = stringResource(R.string.store_info_title),
        onDismiss = onDismiss,
        footer = {
            BottomSheetPrimaryButton(
                text = stringResource(R.string.save),
                icon = Icons.Rounded.Check,
                onClick = {
                    if (name.isBlank()) { nameError = true; return@BottomSheetPrimaryButton }
                    onSave(
                        name.trim(),
                        address.trim().ifBlank { null },
                        phone.trim().ifBlank { null },
                        ownerName.trim(),
                    )
                },
            )
        },
    ) {
        BottomSheetField(
            label = stringResource(R.string.store_name_label),
            value = name,
            onValueChange = { name = it; nameError = false },
            placeholder = stringResource(R.string.store_name),
            required = true,
        )
        if (nameError) {
            Text(stringResource(R.string.name_empty_error), fontSize = 11.sp, color = AppColors.Error)
        }
        Spacer(Modifier.height(12.dp))

        BottomSheetField(
            label = stringResource(R.string.address_label),
            value = address,
            onValueChange = { address = it },
            placeholder = stringResource(R.string.store_address),
        )
        Spacer(Modifier.height(12.dp))

        BottomSheetField(
            label = stringResource(R.string.store_phone_label),
            value = phone,
            onValueChange = { phone = it },
            placeholder = stringResource(R.string.store_phone),
            keyboardType = KeyboardType.Phone,
        )
        Spacer(Modifier.height(16.dp))

        HorizontalDivider(color = AppColors.Divider)
        Spacer(Modifier.height(16.dp))

        BottomSheetField(
            label = stringResource(R.string.display_name_required),
            value = ownerName,
            onValueChange = { ownerName = it },
            placeholder = stringResource(R.string.display_name),
        )
        Spacer(Modifier.height(16.dp))

        HorizontalDivider(color = AppColors.Divider)
        Spacer(Modifier.height(12.dp))

        // Change PIN button — opens dedicated flow
        Text(
            stringResource(R.string.pin_section_title),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextSecondary,
        )
        Spacer(Modifier.height(8.dp))

        BottomSheetOutlineButton(
            text = if (ownerHasPin) stringResource(R.string.change_pin_title) else stringResource(R.string.set_pin_title),
            icon = Icons.Rounded.Lock,
            onClick = onChangePin,
        )
        Spacer(Modifier.height(8.dp))
        BottomSheetOutlineButton(
            text = if (ownerHasPin) stringResource(R.string.change_password_title) else stringResource(R.string.set_password_title),
            icon = Icons.Rounded.Key,
            onClick = onChangePassword,
        )

        Spacer(Modifier.height(12.dp))
    }
}

// ============ Change PIN Bottom Sheet (dedicated flow) ============

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ChangePinBottomSheet(
    hasExistingPin: Boolean,
    pinVerified: Boolean,
    pinVerifyError: String?,
    onVerifyPin: (String) -> Unit,
    onSaveNewPin: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showCurrentPin by remember { mutableStateOf(false) }
    var showNewPin by remember { mutableStateOf(false) }
    var showConfirmPin by remember { mutableStateOf(false) }

    // If no existing PIN, skip verification step
    val isVerified = !hasExistingPin || pinVerified

    MiniPosBottomSheet(
        visible = true,
        title = if (hasExistingPin) stringResource(R.string.change_pin_title) else stringResource(R.string.set_pin_title),
        onDismiss = onDismiss,
        footer = {
            val pinLengthError = stringResource(R.string.pin_length_error)
            val pinMismatchError = stringResource(R.string.pin_mismatch_error)
            val pinCurrentRequired = stringResource(R.string.pin_current_required)

            if (!isVerified) {
                // Step 1: Verify button
                BottomSheetPrimaryButton(
                    text = stringResource(R.string.verify_btn),
                    icon = Icons.Rounded.VerifiedUser,
                    onClick = {
                        if (currentPin.isBlank()) {
                            pinError = pinCurrentRequired
                            return@BottomSheetPrimaryButton
                        }
                        if (currentPin.length < 4) {
                            pinError = pinLengthError
                            return@BottomSheetPrimaryButton
                        }
                        pinError = null
                        onVerifyPin(currentPin)
                    },
                )
            } else {
                // Step 2: Save button
                BottomSheetPrimaryButton(
                    text = stringResource(R.string.save),
                    icon = Icons.Rounded.Check,
                    onClick = {
                        if (newPin.length < 4) {
                            pinError = pinLengthError
                            return@BottomSheetPrimaryButton
                        }
                        if (newPin != confirmPin) {
                            pinError = pinMismatchError
                            return@BottomSheetPrimaryButton
                        }
                        pinError = null
                        onSaveNewPin(newPin)
                    },
                )
            }
        },
    ) {
        if (hasExistingPin && !pinVerified) {
            // ── Step 1: Verify current PIN ──
            Text(
                stringResource(R.string.verify_pin_step),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Primary,
            )
            Spacer(Modifier.height(12.dp))

            BottomSheetField(
                label = stringResource(R.string.pin_current_label),
                value = currentPin,
                onValueChange = {
                    if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                        currentPin = it; pinError = null
                    }
                },
                placeholder = stringResource(R.string.pin_current_hint),
                keyboardType = KeyboardType.NumberPassword,
                visualTransformation = if (showCurrentPin) VisualTransformation.None else PasswordVisualTransformation(),
                autoFocus = true,
                trailingIcon = {
                    IconButton(onClick = { showCurrentPin = !showCurrentPin }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (showCurrentPin) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
            )

            // Show verify error from ViewModel
            if (pinVerifyError != null) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Error,
                        contentDescription = null,
                        tint = AppColors.Error,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(pinVerifyError, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Error)
                }
            }

            // Show local error
            if (pinError != null && pinVerifyError == null) {
                Spacer(Modifier.height(4.dp))
                Text(pinError!!, fontSize = 11.sp, color = AppColors.Error)
            }
        } else {
            // ── Step 2 (or only step if no existing PIN): Enter new PIN ──
            if (hasExistingPin) {
                // Show verified badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.SuccessSoft, RoundedCornerShape(MiniPosTokens.RadiusMd))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = AppColors.Success,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.pin_verified),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Success,
                    )
                }
                Spacer(Modifier.height(16.dp))

                Text(
                    stringResource(R.string.new_pin_step),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary,
                )
            } else {
                Text(
                    stringResource(R.string.set_new_pin_step),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary,
                )
            }
            Spacer(Modifier.height(12.dp))

            BottomSheetField(
                label = stringResource(R.string.pin_new_label),
                value = newPin,
                onValueChange = {
                    if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                        newPin = it; pinError = null
                    }
                },
                placeholder = stringResource(R.string.pin_4_6_label),
                keyboardType = KeyboardType.NumberPassword,
                visualTransformation = if (showNewPin) VisualTransformation.None else PasswordVisualTransformation(),
                autoFocus = true,
                trailingIcon = {
                    IconButton(onClick = { showNewPin = !showNewPin }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (showNewPin) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
            )
            Spacer(Modifier.height(12.dp))

            BottomSheetField(
                label = stringResource(R.string.pin_confirm_label),
                value = confirmPin,
                onValueChange = {
                    if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                        confirmPin = it; pinError = null
                    }
                },
                placeholder = stringResource(R.string.confirm_password),
                keyboardType = KeyboardType.NumberPassword,
                visualTransformation = if (showConfirmPin) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showConfirmPin = !showConfirmPin }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (showConfirmPin) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
            )

            if (pinError != null) {
                Spacer(Modifier.height(4.dp))
                Text(pinError!!, fontSize = 11.sp, color = AppColors.Error)
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

// ============ Change Password Bottom Sheet (OWNER only) ============

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ChangePasswordBottomSheet(
    hasExistingPassword: Boolean,
    onSave: (currentPassword: String, newPassword: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    MiniPosBottomSheet(
        visible = true,
        title = if (hasExistingPassword) stringResource(R.string.change_password_title) else stringResource(R.string.set_password_title),
        onDismiss = onDismiss,
        footer = {
            val pwLengthError = stringResource(R.string.error_password_length)
            val pwMismatchError = stringResource(R.string.error_password_mismatch)
            val pwCurrentRequired = stringResource(R.string.pin_current_required)
            BottomSheetPrimaryButton(
                text = stringResource(R.string.save),
                icon = Icons.Rounded.Check,
                onClick = {
                    if (hasExistingPassword && currentPassword.isBlank()) {
                        passwordError = pwCurrentRequired; return@BottomSheetPrimaryButton
                    }
                    if (newPassword.length < 6) {
                        passwordError = pwLengthError; return@BottomSheetPrimaryButton
                    }
                    if (newPassword != confirmPassword) {
                        passwordError = pwMismatchError; return@BottomSheetPrimaryButton
                    }
                    passwordError = null
                    onSave(currentPassword, newPassword)
                },
            )
        },
    ) {
        Text(
            stringResource(if (hasExistingPassword) R.string.change_password_desc else R.string.set_password_desc),
            fontSize = 13.sp,
            color = AppColors.TextSecondary,
        )
        Spacer(Modifier.height(16.dp))

        if (hasExistingPassword) {
            BottomSheetField(
                label = stringResource(R.string.current_password_label),
                value = currentPassword,
                onValueChange = { currentPassword = it; passwordError = null },
                placeholder = stringResource(R.string.login_password_hint),
                keyboardType = KeyboardType.Password,
                visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                autoFocus = true,
                trailingIcon = {
                    IconButton(onClick = { showCurrent = !showCurrent }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (showCurrent) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
        }

        BottomSheetField(
            label = stringResource(R.string.new_password_label),
            value = newPassword,
            onValueChange = { newPassword = it; passwordError = null },
            placeholder = stringResource(R.string.admin_password_hint),
            keyboardType = KeyboardType.Password,
            visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
            autoFocus = !hasExistingPassword,
            trailingIcon = {
                IconButton(onClick = { showNew = !showNew }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (showNew) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
        )
        Spacer(Modifier.height(12.dp))

        BottomSheetField(
            label = stringResource(R.string.confirm_password_required),
            value = confirmPassword,
            onValueChange = { confirmPassword = it; passwordError = null },
            placeholder = stringResource(R.string.confirm_password),
            keyboardType = KeyboardType.Password,
            visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showConfirm = !showConfirm }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (showConfirm) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
        )

        if (passwordError != null) {
            Spacer(Modifier.height(6.dp))
            Text(passwordError!!, fontSize = 11.sp, color = AppColors.Error)
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ============ Sales Settings Dialog → Bottom Sheet ============

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

    MiniPosBottomSheet(
        visible = true,
        title = stringResource(R.string.sales_settings_title),
        onDismiss = onDismiss,
        footer = {
            BottomSheetPrimaryButton(
                text = stringResource(R.string.save),
                icon = Icons.Rounded.Check,
                onClick = {
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
                },
            )
        },
    ) {
        // Tax toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.enable_tax), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
            Switch(
                checked = taxEnabled,
                onCheckedChange = { taxEnabled = it },
                colors = SwitchDefaults.colors(checkedTrackColor = AppColors.Primary),
            )
        }
        Spacer(Modifier.height(8.dp))

        // Tax rate
        if (taxEnabled) {
            BottomSheetField(
                label = stringResource(R.string.tax_rate_label),
                value = taxRate,
                onValueChange = { taxRate = it.filter { c -> c.isDigit() || c == '.' } },
                placeholder = "0",
                keyboardType = KeyboardType.Decimal,
            )
            Spacer(Modifier.height(12.dp))
        }

        HorizontalDivider(color = AppColors.Divider)
        Spacer(Modifier.height(12.dp))

        BottomSheetField(
            label = stringResource(R.string.receipt_header_label),
            value = receiptHeader,
            onValueChange = { receiptHeader = it },
        )
        Spacer(Modifier.height(12.dp))

        BottomSheetField(
            label = stringResource(R.string.receipt_footer_label),
            value = receiptFooter,
            onValueChange = { receiptFooter = it },
        )
        Spacer(Modifier.height(12.dp))

        HorizontalDivider(color = AppColors.Divider)
        Spacer(Modifier.height(12.dp))

        // Auto print receipt
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.auto_print_receipt), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
            Switch(
                checked = autoPrintReceipt,
                onCheckedChange = { autoPrintReceipt = it },
                colors = SwitchDefaults.colors(checkedTrackColor = AppColors.Primary),
            )
        }
        Spacer(Modifier.height(8.dp))

        // Low stock alert
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.low_stock_alert_label), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
            Switch(
                checked = lowStockAlert,
                onCheckedChange = { lowStockAlert = it },
                colors = SwitchDefaults.colors(checkedTrackColor = AppColors.Primary),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ============ User Management Dialog → Bottom Sheet ============

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
    MiniPosBottomSheet(
        visible = true,
        title = stringResource(R.string.user_management_title),
        onDismiss = onDismiss,
        footer = {
            BottomSheetPrimaryButton(
                text = stringResource(R.string.add_staff),
                icon = Icons.Rounded.PersonAdd,
                onClick = onAddUser,
            )
        },
    ) {
        if (users.isEmpty()) {
            Text(
                stringResource(R.string.no_staff_yet),
                fontSize = 13.sp,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                users.forEach { user ->
                    UserCard(
                        user = user,
                        isCurrent = user.id == currentUserId,
                        onEdit = { onEditUser(user) },
                        onResetPin = { onResetPin(user) },
                        onDelete = { onDeleteUser(user) },
                    )
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    user: User,
    isCurrent: Boolean,
    onEdit: () -> Unit,
    onResetPin: () -> Unit,
    onDelete: () -> Unit,
) {
    var showActions by remember { mutableStateOf(false) }

    // Action Sheet for user options
    MiniPosActionSheet(
        visible = showActions,
        title = user.displayName,
        description = when (user.role) {
            UserRole.OWNER -> stringResource(R.string.role_owner)
            UserRole.MANAGER -> stringResource(R.string.role_manager)
            UserRole.CASHIER -> stringResource(R.string.role_cashier)
        },
        items = buildList {
            add(ActionSheetItem(stringResource(R.string.edit_label), Icons.Rounded.Edit) { showActions = false; onEdit() })
            add(ActionSheetItem(stringResource(R.string.reset_pin_label), Icons.Rounded.Lock) { showActions = false; onResetPin() })
            if (!isCurrent && user.role != UserRole.OWNER) {
                add(ActionSheetItem(stringResource(R.string.delete), Icons.Rounded.Delete, ActionSheetItemStyle.DANGER) { showActions = false; onDelete() })
            }
        },
        cancelText = stringResource(R.string.cancel),
        onDismiss = { showActions = false },
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
            .background(if (isCurrent) AppColors.Primary.copy(alpha = 0.08f) else AppColors.InputBackground)
            .clickable { showActions = true }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isCurrent) Brush.linearGradient(listOf(AppColors.Primary, AppColors.PrimaryLight))
                    else Brush.linearGradient(listOf(AppColors.TextTertiary, AppColors.TextSecondary))
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                user.displayName.firstOrNull()?.uppercase() ?: "?",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    user.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                )
                if (isCurrent) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.you_label),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.PrimaryLight,
                        modifier = Modifier
                            .background(AppColors.PrimaryLight.copy(alpha = 0.1f), RoundedCornerShape(MiniPosTokens.RadiusFull))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                when (user.role) {
                    UserRole.OWNER -> stringResource(R.string.role_owner)
                    UserRole.MANAGER -> stringResource(R.string.role_manager)
                    UserRole.CASHIER -> stringResource(R.string.role_cashier)
                },
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
            )
        }
        Icon(
            Icons.Rounded.MoreVert,
            contentDescription = "Menu",
            tint = AppColors.TextTertiary,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable { showActions = true }
                .padding(4.dp),
        )
    }
}

// ============ Add User Dialog → Bottom Sheet ============

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

    MiniPosBottomSheet(
        visible = true,
        title = stringResource(R.string.add_staff_title),
        onDismiss = onDismiss,
        footer = {
            val pinLengthError = stringResource(R.string.pin_length_error)
            val pinMismatchError = stringResource(R.string.pin_mismatch_error)
            BottomSheetPrimaryButton(
                text = stringResource(R.string.add_btn),
                icon = Icons.Rounded.PersonAdd,
                onClick = {
                    when {
                        name.isBlank() -> nameError = true
                        pin.length < 4 -> pinError = pinLengthError
                        pin != confirmPin -> pinError = pinMismatchError
                        else -> onSave(name.trim(), pin, selectedRole)
                    }
                },
            )
        },
    ) {
        BottomSheetField(
            label = stringResource(R.string.staff_name_label),
            value = name,
            onValueChange = { name = it; nameError = false },
            required = true,
            placeholder = stringResource(R.string.display_name),
        )
        if (nameError) {
            Text(stringResource(R.string.name_empty_error), fontSize = 11.sp, color = AppColors.Error)
        }
        Spacer(Modifier.height(12.dp))

        BottomSheetField(
            label = stringResource(R.string.pin_4_6_label),
            value = pin,
            onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6); pinError = null },
            required = true,
            placeholder = "••••",
            keyboardType = KeyboardType.NumberPassword,
        )
        Spacer(Modifier.height(12.dp))

        BottomSheetField(
            label = stringResource(R.string.confirm_password),
            value = confirmPin,
            onValueChange = { confirmPin = it.filter { c -> c.isDigit() }.take(6); pinError = null },
            required = true,
            placeholder = "••••",
            keyboardType = KeyboardType.NumberPassword,
        )

        if (pinError != null) {
            Spacer(Modifier.height(4.dp))
            Text(pinError!!, fontSize = 11.sp, color = AppColors.Error)
        }
        Spacer(Modifier.height(16.dp))

        Text(stringResource(R.string.role_selection), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary)
        Spacer(Modifier.height(8.dp))
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
        Spacer(Modifier.height(12.dp))
    }
}

// ============ Edit User Dialog → Bottom Sheet ============

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

    MiniPosBottomSheet(
        visible = true,
        title = stringResource(R.string.edit_staff_title),
        onDismiss = onDismiss,
        footer = {
            BottomSheetPrimaryButton(
                text = stringResource(R.string.save),
                icon = Icons.Rounded.Check,
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        onSave(user.copy(displayName = name.trim(), role = selectedRole))
                    }
                },
            )
        },
    ) {
        BottomSheetField(
            label = stringResource(R.string.edit_staff_name),
            value = name,
            onValueChange = { name = it; nameError = false },
            required = true,
            placeholder = stringResource(R.string.display_name),
        )
        if (nameError) {
            Text(stringResource(R.string.name_empty_error), fontSize = 11.sp, color = AppColors.Error)
        }
        Spacer(Modifier.height(16.dp))

        if (user.role != UserRole.OWNER) {
            Text(stringResource(R.string.role_selection), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary)
            Spacer(Modifier.height(8.dp))
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
        Spacer(Modifier.height(12.dp))
    }
}

// ============ Reset PIN Dialog → Bottom Sheet ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResetPinDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    val pinLengthError = stringResource(R.string.pin_length_error)
    val pinMismatchError = stringResource(R.string.pin_mismatch_error)

    MiniPosBottomSheet(
        visible = true,
        title = stringResource(R.string.reset_pin_title),
        onDismiss = onDismiss,
        footer = {
            BottomSheetPrimaryButton(
                text = stringResource(R.string.reset_btn),
                icon = Icons.Rounded.LockReset,
                onClick = {
                    when {
                        pin.length < 4 -> pinError = pinLengthError
                        pin != confirmPin -> pinError = pinMismatchError
                        else -> onSave(pin)
                    }
                },
            )
        },
    ) {
        Text(
            stringResource(R.string.reset_pin_for, user.displayName),
            fontSize = 13.sp,
            color = AppColors.TextSecondary,
        )
        Spacer(Modifier.height(16.dp))

        BottomSheetField(
            label = stringResource(R.string.new_pin_label),
            value = pin,
            onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6); pinError = null },
            required = true,
            placeholder = "••••",
            keyboardType = KeyboardType.NumberPassword,
            visualTransformation = PasswordVisualTransformation(),
        )
        if (pinError != null) {
            Text(pinError!!, fontSize = 11.sp, color = AppColors.Error)
        }
        Spacer(Modifier.height(12.dp))

        BottomSheetField(
            label = stringResource(R.string.confirm_password),
            value = confirmPin,
            onValueChange = { confirmPin = it.filter { c -> c.isDigit() }.take(6); pinError = null },
            required = true,
            placeholder = "••••",
            keyboardType = KeyboardType.NumberPassword,
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(12.dp))
    }
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

// ============ Backup Dialog ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupDialog(
    isBackingUp: Boolean,
    backupFiles: List<BackupFileInfo>,
    onCreateBackup: () -> Unit,
    onDeleteBackup: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.Surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
                    .background(AppColors.Border),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            // ── Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF43A047), Color(0xFF66BB6A)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.CloudUpload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.backup_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        stringResource(R.string.backup_subtitle),
                        fontSize = 12.sp,
                        color = AppColors.TextTertiary,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = AppColors.TextTertiary)
                }
            }

            // ── Create Backup Button
            Button(
                onClick = onCreateBackup,
                enabled = !isBackingUp,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(MiniPosTokens.RadiusMd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF43A047),
                    disabledContainerColor = AppColors.Border,
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            ) {
                if (isBackingUp) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.TextSecondary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.backup_creating),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextSecondary,
                    )
                } else {
                    Icon(
                        Icons.Rounded.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.backup_create_btn),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Backup Files List
            if (backupFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                        .background(AppColors.Background)
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.FolderOff,
                            contentDescription = null,
                            tint = AppColors.TextTertiary,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.backup_no_files),
                            fontSize = 13.sp,
                            color = AppColors.TextTertiary,
                        )
                    }
                }
            } else {
                Text(
                    stringResource(R.string.backup_existing_files, backupFiles.size),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.TextTertiary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                        .background(AppColors.Background)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg)),
                ) {
                    backupFiles.forEachIndexed { index, file ->
                        if (index > 0) {
                            HorizontalDivider(color = AppColors.Divider, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        BackupFileRow(file = file, onDelete = { onDeleteBackup(file.filePath) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupFileRow(
    file: BackupFileInfo,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        MiniPosConfirmDialog(
            visible = true,
            type = PopupType.DELETE,
            icon = Icons.Rounded.DeleteOutline,
            title = stringResource(R.string.backup_delete_title),
            message = stringResource(R.string.backup_delete_confirm),
            cancelText = stringResource(R.string.cancel),
            confirmText = stringResource(R.string.delete_btn),
            confirmStyle = ConfirmButtonStyle.DANGER,
            onCancel = { showDeleteConfirm = false },
            onConfirm = { showDeleteConfirm = false; onDelete() },
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Lock,
            contentDescription = null,
            tint = Color(0xFF43A047),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                file.fileName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
                maxLines = 1,
            )
            Text(
                buildString {
                    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .format(Date(file.createdAt))
                    append(dateStr)
                    append("  •  ")
                    append(String.format("%.2f MB", file.sizeBytes / 1024.0 / 1024.0))
                },
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
            )
        }
        IconButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.delete_btn),
                tint = AppColors.Error,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ============ Restore Dialog ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestoreDialog(
    isRestoring: Boolean,
    backupFiles: List<BackupFileInfo>,
    confirmFile: BackupFileInfo?,
    onSelectFile: (BackupFileInfo) -> Unit,
    onCancelConfirm: () -> Unit,
    onConfirmRestore: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Confirm restore dialog
    if (confirmFile != null) {
        MiniPosConfirmDialog(
            visible = true,
            type = PopupType.WARNING,
            icon = Icons.Rounded.Warning,
            title = stringResource(R.string.restore_confirm_title),
            message = stringResource(
                R.string.restore_confirm_msg,
                confirmFile.fileName,
            ),
            cancelText = stringResource(R.string.cancel),
            confirmText = stringResource(R.string.restore_confirm_btn),
            confirmStyle = ConfirmButtonStyle.DANGER,
            onCancel = onCancelConfirm,
            onConfirm = { onConfirmRestore(confirmFile.filePath) },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.Surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
                    .background(AppColors.Border),
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
            ) {
                // ── Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFF8A65), Color(0xFFF4511E)),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Restore,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.restore_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary,
                        )
                        Text(
                            stringResource(R.string.restore_subtitle),
                            fontSize = 12.sp,
                            color = AppColors.TextTertiary,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = null, tint = AppColors.TextTertiary)
                    }
                }

                // ── Warning notice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                        .background(Color(0xFFFFF3E0))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF8A00),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.restore_warning),
                        fontSize = 12.sp,
                        color = Color(0xFFE65100),
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── File list
                if (backupFiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                            .background(AppColors.Background)
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.FolderOff,
                                contentDescription = null,
                                tint = AppColors.TextTertiary,
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.backup_no_files),
                                fontSize = 13.sp,
                                color = AppColors.TextTertiary,
                            )
                        }
                    }
                } else {
                    Text(
                        stringResource(R.string.restore_select_file),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.TextTertiary,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
                            .background(AppColors.Background)
                            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusLg)),
                    ) {
                        backupFiles.forEachIndexed { index, file ->
                            if (index > 0) {
                                HorizontalDivider(color = AppColors.Divider, modifier = Modifier.padding(horizontal = 16.dp))
                            }
                            RestoreFileRow(file = file, onSelect = { onSelectFile(file) })
                        }
                    }
                }
            }

            // ── Loading overlay
            if (isRestoring) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(AppColors.Surface.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFFF8A65))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.restore_in_progress),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RestoreFileRow(
    file: BackupFileInfo,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Lock,
            contentDescription = null,
            tint = Color(0xFFFF8A65),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                file.fileName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
                maxLines = 1,
            )
            Text(
                buildString {
                    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .format(Date(file.createdAt))
                    append(dateStr)
                    append("  •  ")
                    append(String.format("%.2f MB", file.sizeBytes / 1024.0 / 1024.0))
                },
                fontSize = 11.sp,
                color = AppColors.TextTertiary,
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = AppColors.TextTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}
