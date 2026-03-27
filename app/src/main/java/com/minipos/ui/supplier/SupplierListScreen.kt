package com.minipos.ui.supplier

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.domain.model.Supplier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierListScreen(
    onBack: () -> Unit,
    viewModel: SupplierListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    if (state.showForm) {
        SupplierFormDialog(
            editing = state.editingSupplier,
            onSave = { name, contact, phone, email, address, taxCode, notes ->
                viewModel.save(name, contact, phone, email, address, taxCode, notes)
            },
            onDismiss = { viewModel.dismissForm() },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.supplier_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showCreateForm() }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_supplier_cd))
                    }
                },
            )
        },
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.suppliers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(64.dp), tint = AppColors.TextTertiary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.no_suppliers), style = MaterialTheme.typography.titleMedium, color = AppColors.TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.showCreateForm() }, shape = RoundedCornerShape(8.dp)) {
                        Text(stringResource(R.string.add_supplier_btn))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.suppliers) { supplier ->
                    SupplierItem(
                        supplier = supplier,
                        onClick = { viewModel.showEditForm(supplier) },
                        onDelete = { viewModel.delete(supplier) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SupplierItem(
    supplier: Supplier,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_supplier_title)) },
            text = { Text(stringResource(R.string.delete_confirm_msg, supplier.name)) },
            confirmButton = { TextButton(onClick = { showDeleteConfirm = false; onDelete() }) { Text(stringResource(R.string.delete), color = AppColors.Error) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) } },
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
            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = AppColors.Accent, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(supplier.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                if (!supplier.contactPerson.isNullOrBlank()) {
                    Text("LH: ${supplier.contactPerson}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
                if (!supplier.phone.isNullOrBlank()) {
                    Text(supplier.phone, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = AppColors.TextTertiary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SupplierFormDialog(
    editing: Supplier?,
    onSave: (String, String?, String?, String?, String?, String?, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var contact by remember { mutableStateOf(editing?.contactPerson ?: "") }
    var phone by remember { mutableStateOf(editing?.phone ?: "") }
    var email by remember { mutableStateOf(editing?.email ?: "") }
    var address by remember { mutableStateOf(editing?.address ?: "") }
    var taxCode by remember { mutableStateOf(editing?.taxCode ?: "") }
    var notes by remember { mutableStateOf(editing?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing != null) stringResource(R.string.edit_supplier) else stringResource(R.string.add_supplier_btn)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.supplier_name_required)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                }
                item {
                    OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text(stringResource(R.string.contact_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                }
                item {
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(stringResource(R.string.phone_number)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(8.dp))
                }
                item {
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(stringResource(R.string.email_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), shape = RoundedCornerShape(8.dp))
                }
                item {
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(stringResource(R.string.address_label)) }, modifier = Modifier.fillMaxWidth(), maxLines = 2, shape = RoundedCornerShape(8.dp))
                }
                item {
                    OutlinedTextField(value = taxCode, onValueChange = { taxCode = it }, label = { Text(stringResource(R.string.tax_code)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                }
                item {
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.notes)) }, modifier = Modifier.fillMaxWidth(), maxLines = 3, shape = RoundedCornerShape(8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onSave(name, contact.ifBlank { null }, phone.ifBlank { null }, email.ifBlank { null }, address.ifBlank { null }, taxCode.ifBlank { null }, notes.ifBlank { null })
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
