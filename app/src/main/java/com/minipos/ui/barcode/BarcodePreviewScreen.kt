package com.minipos.ui.barcode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.ui.components.MiniPosTokens
import com.minipos.ui.components.MiniPosBottomSheet
import com.minipos.ui.components.MiniPosGradientButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodePreviewScreen(
    onBack: () -> Unit,
    viewModel: BarcodeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showShareSheet by remember { mutableStateOf(false) }

    val handleBack = {
        viewModel.dismissPreview()
        onBack()
    }

    // Show messages
    LaunchedEffect(state.message, state.error) {
        val msg = state.error ?: state.message
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    // Nếu không có bitmap thì quay về
    val bitmap = state.previewBitmap
    if (bitmap == null) {
        LaunchedEffect(Unit) {
            viewModel.dismissPreview()
            onBack()
        }
        return
    }

    // ── Share format picker ──
    if (showShareSheet) {
        MiniPosBottomSheet(
            visible = true,
            title = stringResource(R.string.share_format_title),
            onDismiss = { showShareSheet = false },
        ) {
            // PDF
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showShareSheet = false
                        viewModel.shareAsPdf(context)
                    }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(AppColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column {
                    Text(
                        stringResource(R.string.share_pdf_label),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        stringResource(R.string.share_pdf_desc),
                        fontSize = 13.sp,
                        color = AppColors.TextSecondary,
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = AppColors.Divider,
            )
            // Image
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showShareSheet = false
                        viewModel.shareAsImage(context)
                    }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(AppColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column {
                    Text(
                        stringResource(R.string.share_image_label),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        stringResource(R.string.share_image_desc),
                        fontSize = 13.sp,
                        color = AppColors.TextSecondary,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    // Printer picker
    if (state.showPrinterPicker) {
        PrinterPickerDialog(
            devices = state.availablePrinters,
            onSelect = { viewModel.printViaBluetooth(context, it) },
            onDismiss = { viewModel.dismissPrinterPicker() },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.barcode_preview),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background,
                    titleContentColor = AppColors.TextPrimary,
                    navigationIconContentColor = AppColors.TextPrimary,
                ),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Surface),
            ) {
                HorizontalDivider(color = AppColors.Divider)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Share
                    OutlinedButton(
                        onClick = { showShareSheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(MiniPosTokens.RadiusMd),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary),
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = AppColors.Primary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.share),
                            color = AppColors.Primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    // Print
                    MiniPosGradientButton(
                        text = if (state.isPrinting) stringResource(R.string.printing)
                               else stringResource(R.string.barcode_action_print),
                        onClick = { viewModel.showPrinterPicker(context) },
                        enabled = !state.isPrinting,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        icon = Icons.Default.Print,
                    )
                }
            }
        },
        containerColor = AppColors.Background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(MiniPosTokens.RadiusMd),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Barcode labels preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(8.dp),
                        contentScale = ContentScale.FillWidth,
                    )
                }
            }
        }
    }
}
