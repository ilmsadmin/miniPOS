package com.minipos.ui.pos

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.core.theme.AppColors
import com.minipos.domain.model.Customer
import com.minipos.R

import com.minipos.ui.components.*

@Composable
fun PosStep3Screen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    viewModel: PosStep3ViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val cart by viewModel.cartHolder.cart.collectAsState()

    Scaffold(
        containerColor = AppColors.Background,
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = AppColors.Surface,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (cart.customer != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                                .background(AppColors.SecondaryContainer)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = AppColors.Secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(cart.customer!!.name, fontWeight = FontWeight.Medium)
                                    if (!cart.customer!!.phone.isNullOrBlank()) {
                                        Text(cart.customer!!.phone!!, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                                    }
                                }
                            }
                            IconButton(onClick = { viewModel.selectCustomer(null) }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.deselect), tint = AppColors.TextSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    MiniPosGradientButton(
                        text = if (cart.customer == null) stringResource(R.string.skip_walk_in) else stringResource(R.string.next_step),
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MiniPosTopBar(
                title = stringResource(R.string.step3_title),
                onBack = onBack,
            )

            if (state.showCreateForm) {
                QuickCreateCustomerCard(
                    onCancel = { viewModel.toggleCreateForm() },
                    onCreate = { name, phone -> viewModel.quickCreateCustomer(name, phone) },
                )
            } else {
                MiniPosSearchBar(
                    value = state.searchQuery,
                    onValueChange = { viewModel.search(it) },
                    placeholder = stringResource(R.string.search_customer),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    trailingIcon = {
                        IconButton(onClick = { viewModel.toggleCreateForm() }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = stringResource(R.string.add_new_customer), tint = AppColors.Accent)
                        }
                    },
                )
            }

            // Recent customers header
            if (state.searchQuery.isBlank() && state.recentCustomers.isNotEmpty()) {
                Text(
                    stringResource(R.string.recent_customers),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextSecondary,
                )
            }

            val customers = if (state.searchQuery.isNotBlank()) state.searchResults else state.recentCustomers

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(customers) { customer ->
                    CustomerRow(
                        customer = customer,
                        isSelected = cart.customer?.id == customer.id,
                        onClick = { viewModel.selectCustomer(customer) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerRow(
    customer: Customer,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiniPosTokens.RadiusSm))
            .background(if (isSelected) AppColors.PrimaryContainer else AppColors.Surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isSelected) Icons.Default.CheckCircle else Icons.Default.Person,
            contentDescription = null,
            tint = if (isSelected) AppColors.Primary else AppColors.TextTertiary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(customer.name, fontWeight = FontWeight.Medium)
            if (!customer.phone.isNullOrBlank()) {
                Text(customer.phone, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            }
        }
        Text(
            stringResource(R.string.visit_count_format, customer.visitCount),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextTertiary,
        )
    }
}

@Composable
private fun QuickCreateCustomerCard(
    onCancel: () -> Unit,
    onCreate: (String, String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.add_new_customer_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.customer_name_required)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(stringResource(R.string.phone_number)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(8.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { if (name.isNotBlank()) onCreate(name, phone.ifBlank { null }) },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(8.dp),
                ) { Text(stringResource(R.string.add_btn)) }
            }
        }
    }
}
