package com.minipos.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
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
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.ownerName,
        onValueChange = { viewModel.updateOwnerName(it) },
        label = { Text(stringResource(R.string.display_name_required)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(modifier = Modifier.height(32.dp))

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
