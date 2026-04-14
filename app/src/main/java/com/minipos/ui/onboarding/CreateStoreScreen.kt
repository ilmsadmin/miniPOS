package com.minipos.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoreScreen(
    onStoreCreated: () -> Unit,
    onBack: () -> Unit,
    viewModel: CreateStoreViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onStoreCreated()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_store_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            StoreInfoStep(state, viewModel)

            state.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(error, color = AppColors.Error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StoreInfoStep(state: CreateStoreState, viewModel: CreateStoreViewModel) {
    var showPin by remember { mutableStateOf(false) }
    var showPinConfirm by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showPasswordConfirm by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = state.storeName,
        onValueChange = { viewModel.updateStoreName(it) },
        label = { Text(stringResource(R.string.store_name_required)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.storeCode,
        onValueChange = { viewModel.updateStoreCode(it.uppercase().take(8)) },
        label = { Text(stringResource(R.string.store_code_required)) },
        supportingText = { Text(stringResource(R.string.store_code_supporting)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.storeAddress,
        onValueChange = { viewModel.updateStoreAddress(it) },
        label = { Text(stringResource(R.string.address_label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.storePhone,
        onValueChange = { viewModel.updateStorePhone(it) },
        label = { Text(stringResource(R.string.phone_number)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(modifier = Modifier.height(24.dp))

    HorizontalDivider(color = AppColors.Divider)
    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.create_owner_account_section),
        style = MaterialTheme.typography.titleSmall,
        color = AppColors.TextSecondary,
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = state.ownerName,
        onValueChange = { viewModel.updateOwnerName(it) },
        label = { Text(stringResource(R.string.display_name_required)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = state.ownerPin,
        onValueChange = { if (it.all(Char::isDigit) && it.length <= 6) viewModel.updateOwnerPin(it) },
        label = { Text(stringResource(R.string.pin_label_required)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { showPin = !showPin }) {
                Icon(
                    if (showPin) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = null,
                )
            }
        },
        isError = state.ownerPin.isNotEmpty() && state.ownerPin.length < 4,
        supportingText = if (state.ownerPin.isNotEmpty() && state.ownerPin.length < 4) {
            { Text(stringResource(R.string.error_pin_length), color = AppColors.Error) }
        } else null,
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = state.ownerPinConfirm,
        onValueChange = { if (it.all(Char::isDigit) && it.length <= 6) viewModel.updateOwnerPinConfirm(it) },
        label = { Text(stringResource(R.string.pin_confirm_label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = if (showPinConfirm) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { showPinConfirm = !showPinConfirm }) {
                Icon(
                    if (showPinConfirm) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = null,
                )
            }
        },
        isError = state.ownerPinConfirm.isNotEmpty() && state.ownerPin != state.ownerPinConfirm,
        supportingText = if (state.ownerPinConfirm.isNotEmpty() && state.ownerPin != state.ownerPinConfirm) {
            { Text(stringResource(R.string.pin_mismatch_error), color = AppColors.Error) }
        } else null,
    )
    Spacer(modifier = Modifier.height(24.dp))

    HorizontalDivider(color = AppColors.Divider)
    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.owner_password_section),
        style = MaterialTheme.typography.titleSmall,
        color = AppColors.TextSecondary,
    )
    Text(
        text = stringResource(R.string.owner_password_section_hint),
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.TextTertiary,
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = state.ownerPassword,
        onValueChange = { viewModel.updateOwnerPassword(it) },
        label = { Text(stringResource(R.string.admin_password)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                    if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = null,
                )
            }
        },
        isError = state.ownerPassword.isNotBlank() && state.ownerPassword.length < 6,
        supportingText = if (state.ownerPassword.isNotBlank() && state.ownerPassword.length < 6) {
            { Text(stringResource(R.string.error_password_length), color = AppColors.Error) }
        } else {
            { Text(stringResource(R.string.admin_password_hint)) }
        },
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = state.ownerPasswordConfirm,
        onValueChange = { viewModel.updateOwnerPasswordConfirm(it) },
        label = { Text(stringResource(R.string.confirm_password)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (showPasswordConfirm) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { showPasswordConfirm = !showPasswordConfirm }) {
                Icon(
                    if (showPasswordConfirm) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = null,
                )
            }
        },
        isError = state.ownerPasswordConfirm.isNotEmpty() && state.ownerPassword != state.ownerPasswordConfirm,
        supportingText = if (state.ownerPasswordConfirm.isNotEmpty() && state.ownerPassword != state.ownerPasswordConfirm) {
            { Text(stringResource(R.string.error_password_mismatch), color = AppColors.Error) }
        } else null,
    )
    Spacer(modifier = Modifier.height(24.dp))

    // ── Data storage warning ──
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AppColors.Warning.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Rounded.WarningAmber,
            contentDescription = null,
            tint = AppColors.Warning,
            modifier = Modifier.size(24.dp),
        )
        Column {
            Text(
                text = stringResource(R.string.onboarding_data_warning_title),
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.Warning,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.onboarding_data_warning_message),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = { viewModel.createStore() },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        enabled = !state.isLoading && state.canCreate,
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(stringResource(R.string.create_store_btn), style = MaterialTheme.typography.titleMedium)
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}
