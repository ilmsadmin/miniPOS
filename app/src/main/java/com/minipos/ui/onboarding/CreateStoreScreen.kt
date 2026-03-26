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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
                title = { Text(if (state.step == 1) "Tạo cửa hàng (1/2)" else "Tạo tài khoản (2/2)") },
                navigationIcon = {
                    IconButton(onClick = { if (state.step == 2) viewModel.previousStep() else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
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
            // Step indicator
            LinearProgressIndicator(
                progress = state.step / 2f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = AppColors.Primary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.step == 1) {
                StoreInfoStep(state, viewModel)
            } else {
                OwnerAccountStep(state, viewModel)
            }

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
        label = { Text("Tên cửa hàng *") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.storeCode,
        onValueChange = { viewModel.updateStoreCode(it.uppercase().take(8)) },
        label = { Text("Mã cửa hàng *") },
        supportingText = { Text("4–8 ký tự, dùng để kết nối thiết bị") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.storeAddress,
        onValueChange = { viewModel.updateStoreAddress(it) },
        label = { Text("Địa chỉ") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.storePhone,
        onValueChange = { viewModel.updateStorePhone(it) },
        label = { Text("Số điện thoại") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = { viewModel.nextStep() },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        enabled = state.storeName.isNotBlank() && state.storeCode.length >= 4,
    ) {
        Text("Tiếp theo →", style = MaterialTheme.typography.titleMedium)
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun OwnerAccountStep(state: CreateStoreState, viewModel: CreateStoreViewModel) {
    OutlinedTextField(
        value = state.ownerName,
        onValueChange = { viewModel.updateOwnerName(it) },
        label = { Text("Tên hiển thị *") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.ownerPin,
        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) viewModel.updateOwnerPin(it) },
        label = { Text("PIN đăng nhập * (4–6 số)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = PasswordVisualTransformation(),
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.ownerPassword,
        onValueChange = { viewModel.updateOwnerPassword(it) },
        label = { Text("Mật khẩu quản trị *") },
        supportingText = { Text("Tối thiểu 6 ký tự, dùng cho thao tác nhạy cảm") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.confirmPassword,
        onValueChange = { viewModel.updateConfirmPassword(it) },
        label = { Text("Xác nhận mật khẩu *") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        shape = RoundedCornerShape(12.dp),
        isError = state.confirmPassword.isNotEmpty() && state.confirmPassword != state.ownerPassword,
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
            Text("🏪  Tạo cửa hàng", style = MaterialTheme.typography.titleMedium)
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}
