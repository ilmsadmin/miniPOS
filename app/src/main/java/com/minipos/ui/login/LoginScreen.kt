package com.minipos.ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.core.theme.AppColors
import com.minipos.domain.model.User

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = state.storeName,
            style = MaterialTheme.typography.headlineMedium,
            color = AppColors.Primary,
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (state.selectedUser == null) {
            Text("Chọn tài khoản", style = MaterialTheme.typography.titleMedium, color = AppColors.TextSecondary)
            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.users) { user ->
                    UserCard(user = user, onClick = { viewModel.selectUser(user) })
                }
            }
        } else {
            // PIN entry
            val user = state.selectedUser!!

            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape),
                tint = AppColors.Primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(user.displayName, style = MaterialTheme.typography.titleLarge)
            Text(user.role.name, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = state.pin,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) viewModel.updatePin(it) },
                label = { Text("Nhập PIN") },
                modifier = Modifier.width(200.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                isError = state.error != null,
                supportingText = state.error?.let { { Text(it, color = AppColors.Error) } },
                textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.login() },
                modifier = Modifier
                    .width(200.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = state.pin.length >= 4 && !state.isLoading,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Đăng nhập")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { viewModel.clearSelection() }) {
                Text("← Chọn tài khoản khác")
            }
        }
    }
}

@Composable
private fun UserCard(user: User, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = AppColors.Primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                user.displayName,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}
