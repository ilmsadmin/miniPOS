package com.minipos.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.core.theme.AppColors
import com.minipos.domain.model.StoreSettings
import com.minipos.domain.model.User
import com.minipos.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show messages
    LaunchedEffect(state.message) {
        state.message?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    // Logout confirmation dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Đăng xuất?") },
            text = { Text("Bạn có chắc muốn đăng xuất khỏi ứng dụng?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    viewModel.logout { onLogout() }
                }) { Text("Đăng xuất", color = AppColors.Error) }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("Hủy") } },
        )
    }

    // Store Info Dialog
    if (state.showStoreInfoDialog) {
        StoreInfoDialog(
            store = state.store,
            onDismiss = { viewModel.dismissStoreInfoDialog() },
            onSave = { name, address, phone -> viewModel.updateStoreInfo(name, address, phone) },
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
            title = { Text("Xoá nhân viên?") },
            text = { Text("Bạn có chắc muốn xoá \"${user.displayName}\"? Thao tác này không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteUser(user.id) }) {
                    Text("Xoá", color = AppColors.Error)
                }
            },
            dismissButton = { TextButton(onClick = { viewModel.dismissDeleteUserConfirm() }) { Text("Hủy") } },
        )
    }

    // Backup info dialog
    if (state.showBackupDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBackupDialog() },
            title = { Text("Sao lưu dữ liệu") },
            text = {
                Column {
                    Text("Tính năng sao lưu lên Google Drive sẽ được cập nhật trong phiên bản tới.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Dữ liệu của bạn hiện đang được lưu trữ an toàn trên thiết bị này.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.dismissBackupDialog() }) { Text("Đã hiểu") } },
        )
    }

    // Restore info dialog
    if (state.showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestoreDialog() },
            title = { Text("Khôi phục dữ liệu") },
            text = {
                Column {
                    Text("Tính năng khôi phục từ Google Drive sẽ được cập nhật trong phiên bản tới.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Vui lòng đảm bảo đã sao lưu dữ liệu trước khi thực hiện khôi phục.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.dismissRestoreDialog() }) { Text("Đã hiểu") } },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Store info card
                item {
                    state.store?.let { store ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColors.PrimaryContainer),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Store, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(store.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("Mã: ${store.code}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                                    }
                                }
                                if (!store.address.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(store.address, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                                }
                                if (!store.phone.isNullOrBlank()) {
                                    Text("SĐT: ${store.phone}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                                }
                            }
                        }
                    }
                }

                // Current user
                item {
                    state.currentUser?.let { user ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = AppColors.Secondary, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(user.displayName, fontWeight = FontWeight.Medium)
                                    Text(
                                        "Vai trò: ${
                                            when (user.role) {
                                                UserRole.OWNER -> "Chủ cửa hàng"
                                                UserRole.MANAGER -> "Quản lý"
                                                UserRole.CASHIER -> "Thu ngân"
                                            }
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextSecondary,
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Settings items
                item { SectionHeader("Cửa hàng") }
                item {
                    SettingsItem(
                        Icons.Default.Store,
                        "Thông tin cửa hàng",
                        "Chỉnh sửa tên, địa chỉ, số điện thoại",
                    ) { viewModel.showStoreInfoDialog() }
                }
                item {
                    SettingsItem(
                        Icons.Default.People,
                        "Nhân viên",
                        "${state.users.size} người dùng",
                    ) { viewModel.showUserManagement() }
                }
                item {
                    val settings = state.store?.settings ?: StoreSettings()
                    SettingsItem(
                        Icons.Default.Tune,
                        "Cài đặt bán hàng",
                        buildString {
                            if (settings.taxEnabled) append("Thuế ${settings.defaultTaxRate}%")
                            else append("Thuế: tắt")
                            append(" · ")
                            if (settings.autoPrintReceipt) append("Tự động in") else append("In thủ công")
                        },
                    ) { viewModel.showSalesSettingsDialog() }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { SectionHeader("Dữ liệu") }
                item {
                    SettingsItem(
                        Icons.Default.CloudUpload,
                        "Sao lưu",
                        "Sao lưu dữ liệu lên Google Drive",
                    ) { viewModel.showBackupDialog() }
                }
                item {
                    SettingsItem(
                        Icons.Default.CloudDownload,
                        "Khôi phục",
                        "Khôi phục từ bản sao lưu",
                    ) { viewModel.showRestoreDialog() }
                }
                item {
                    SettingsItem(
                        Icons.Default.SyncAlt,
                        "Đồng bộ P2P",
                        "Sắp ra mắt",
                    ) {
                        // P2P Sync - Coming soon
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { SectionHeader("Ứng dụng") }
                item {
                    SettingsItem(
                        Icons.Default.Info,
                        "Phiên bản",
                        "miniPOS v1.0.0",
                    ) {}
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Logout button
                item {
                    OutlinedButton(
                        onClick = { showLogoutConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Error),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đăng xuất", fontWeight = FontWeight.Medium)
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

// ============ Store Info Dialog ============

@Composable
private fun StoreInfoDialog(
    store: com.minipos.domain.model.Store?,
    onDismiss: () -> Unit,
    onSave: (name: String, address: String?, phone: String?) -> Unit,
) {
    var name by remember { mutableStateOf(store?.name ?: "") }
    var address by remember { mutableStateOf(store?.address ?: "") }
    var phone by remember { mutableStateOf(store?.phone ?: "") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thông tin cửa hàng") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Tên cửa hàng *") },
                    isError = nameError,
                    supportingText = if (nameError) {{ Text("Tên không được để trống") }} else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Địa chỉ") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    nameError = true
                } else {
                    onSave(name.trim(), address.trim().ifBlank { null }, phone.trim().ifBlank { null })
                }
            }) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
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
        title = { Text("Cài đặt bán hàng") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Tax toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Bật thuế", style = MaterialTheme.typography.bodyLarge)
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
                        label = { Text("Thuế suất mặc định (%)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text("%") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
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
                    shape = RoundedCornerShape(8.dp),
                )

                // Receipt footer
                OutlinedTextField(
                    value = receiptFooter,
                    onValueChange = { receiptFooter = it },
                    label = { Text("Footer hóa đơn") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Auto print receipt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Tự động in hóa đơn", style = MaterialTheme.typography.bodyLarge)
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
                    Text("Cảnh báo tồn kho thấp", style = MaterialTheme.typography.bodyLarge)
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
            }) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
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
                Text("Quản lý nhân viên")
                IconButton(onClick = onAddUser) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Thêm nhân viên", tint = AppColors.Primary)
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
                            "Chưa có nhân viên nào",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng") } },
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
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) AppColors.PrimaryContainer else AppColors.SurfaceVariant
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
                            "(Bạn)",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.Primary,
                        )
                    }
                }
                Text(
                    when (user.role) {
                        UserRole.OWNER -> "Chủ cửa hàng"
                        UserRole.MANAGER -> "Quản lý"
                        UserRole.CASHIER -> "Thu ngân"
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
                        text = { Text("Chỉnh sửa") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        onClick = { showMenu = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text("Đặt lại PIN") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        onClick = { showMenu = false; onResetPin() },
                    )
                    if (!isCurrent && user.role != UserRole.OWNER) {
                        DropdownMenuItem(
                            text = { Text("Xoá", color = AppColors.Error) },
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
        title = { Text("Thêm nhân viên") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Tên nhân viên *") },
                    isError = nameError,
                    supportingText = if (nameError) {{ Text("Tên không được để trống") }} else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6); pinError = null },
                    label = { Text("Mã PIN (4-6 số) *") },
                    isError = pinError != null,
                    supportingText = pinError?.let {{ Text(pinError!!) }},
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it.filter { c -> c.isDigit() }.take(6); pinError = null },
                    label = { Text("Xác nhận PIN *") },
                    isError = pinError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )

                Text("Vai trò", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedRole == UserRole.MANAGER,
                        onClick = { selectedRole = UserRole.MANAGER },
                        label = { Text("Quản lý") },
                    )
                    FilterChip(
                        selected = selectedRole == UserRole.CASHIER,
                        onClick = { selectedRole = UserRole.CASHIER },
                        label = { Text("Thu ngân") },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    name.isBlank() -> nameError = true
                    pin.length < 4 -> pinError = "PIN phải từ 4-6 số"
                    pin != confirmPin -> pinError = "PIN không khớp"
                    else -> onSave(name.trim(), pin, selectedRole)
                }
            }) { Text("Thêm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
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
        title = { Text("Chỉnh sửa nhân viên") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Tên nhân viên *") },
                    isError = nameError,
                    supportingText = if (nameError) {{ Text("Tên không được để trống") }} else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )

                if (user.role != UserRole.OWNER) {
                    Text("Vai trò", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedRole == UserRole.MANAGER,
                            onClick = { selectedRole = UserRole.MANAGER },
                            label = { Text("Quản lý") },
                        )
                        FilterChip(
                            selected = selectedRole == UserRole.CASHIER,
                            onClick = { selectedRole = UserRole.CASHIER },
                            label = { Text("Thu ngân") },
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
            }) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
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
        title = { Text("Đặt lại PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Đặt lại PIN cho ${user.displayName}", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6); pinError = null },
                    label = { Text("PIN mới (4-6 số) *") },
                    isError = pinError != null,
                    supportingText = pinError?.let {{ Text(pinError!!) }},
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it.filter { c -> c.isDigit() }.take(6); pinError = null },
                    label = { Text("Xác nhận PIN *") },
                    isError = pinError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    pin.length < 4 -> pinError = "PIN phải từ 4-6 số"
                    pin != confirmPin -> pinError = "PIN không khớp"
                    else -> onSave(pin)
                }
            }) { Text("Đặt lại") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

// ============ Shared Components ============

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        color = AppColors.Primary,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppColors.TextTertiary)
        }
    }
}
