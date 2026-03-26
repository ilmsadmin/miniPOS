package com.minipos.ui.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.domain.model.Customer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    onBack: () -> Unit,
    viewModel: CustomerListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    if (state.showForm) {
        CustomerFormDialog(
            editing = state.editingCustomer,
            onSave = { name, phone, email, address, notes ->
                viewModel.save(name, phone, email, address, notes)
            },
            onDismiss = { viewModel.dismissForm() },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Khách hàng") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showCreateForm() }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Thêm khách hàng")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.search(it) },
                placeholder = { Text("Tìm khách hàng…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.customers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(64.dp), tint = AppColors.TextTertiary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Chưa có khách hàng", style = MaterialTheme.typography.titleMedium, color = AppColors.TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.showCreateForm() }, shape = RoundedCornerShape(8.dp)) {
                            Text("Thêm khách hàng")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.customers) { customer ->
                        CustomerItem(
                            customer = customer,
                            onClick = { viewModel.showEditForm(customer) },
                            onDelete = { viewModel.delete(customer) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerItem(
    customer: Customer,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Xóa khách hàng?") },
            text = { Text("Bạn có chắc muốn xóa \"${customer.name}\"?") },
            confirmButton = { TextButton(onClick = { showDeleteConfirm = false; onDelete() }) { Text("Xóa", color = AppColors.Error) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Hủy") } },
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(customer.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                if (!customer.phone.isNullOrBlank()) {
                    Text(customer.phone, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
                Row {
                    Text("${customer.visitCount} lần mua", style = MaterialTheme.typography.bodySmall, color = AppColors.TextTertiary)
                    if (customer.totalSpent > 0) {
                        Text(" · Tổng chi: ${CurrencyFormatter.formatCompact(customer.totalSpent)}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextTertiary)
                    }
                }
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = AppColors.TextTertiary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun CustomerFormDialog(
    editing: Customer?,
    onSave: (String, String?, String?, String?, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var phone by remember { mutableStateOf(editing?.phone ?: "") }
    var email by remember { mutableStateOf(editing?.email ?: "") }
    var address by remember { mutableStateOf(editing?.address ?: "") }
    var notes by remember { mutableStateOf(editing?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing != null) "Sửa khách hàng" else "Thêm khách hàng") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên khách hàng *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Số điện thoại") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Địa chỉ") }, modifier = Modifier.fillMaxWidth(), maxLines = 2, shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Ghi chú") }, modifier = Modifier.fillMaxWidth(), maxLines = 2, shape = RoundedCornerShape(8.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, phone.ifBlank { null }, email.ifBlank { null }, address.ifBlank { null }, notes.ifBlank { null }) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}
