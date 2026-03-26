package com.minipos.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.minipos.core.theme.AppColors
import java.util.concurrent.Executors

/**
 * Full-screen barcode/QR scanner using CameraX + ML Kit.
 * Can be used standalone or embedded in other screens.
 */
@Composable
fun BarcodeScannerScreen(
    onBarcodeScanned: (String, String) -> Unit, // (value, format)
    onClose: () -> Unit,
    title: String = "Quét mã vạch / QR",
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var scannedValue by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    var flashEnabled by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (!hasCameraPermission) {
            // Permission not granted
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.White,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Cần quyền truy cập Camera",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Vui lòng cấp quyền Camera để quét mã vạch",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Cấp quyền")
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onClose) {
                    Text("Đóng", color = Color.White)
                }
            }
        } else {
            // Camera preview
            val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

            DisposableEffect(Unit) {
                onDispose { cameraExecutor.shutdown() }
            }

            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val barcodeScanner = BarcodeScanning.getClient()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            if (!isScanning) {
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            @androidx.annotation.OptIn(ExperimentalGetImage::class)
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees,
                                )

                                barcodeScanner.process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            val rawValue = barcode.rawValue ?: continue
                                            val format = when (barcode.format) {
                                                Barcode.FORMAT_QR_CODE -> "QR"
                                                Barcode.FORMAT_EAN_13 -> "EAN-13"
                                                Barcode.FORMAT_EAN_8 -> "EAN-8"
                                                Barcode.FORMAT_UPC_A -> "UPC-A"
                                                Barcode.FORMAT_UPC_E -> "UPC-E"
                                                Barcode.FORMAT_CODE_128 -> "CODE-128"
                                                Barcode.FORMAT_CODE_39 -> "CODE-39"
                                                Barcode.FORMAT_CODE_93 -> "CODE-93"
                                                Barcode.FORMAT_ITF -> "ITF"
                                                Barcode.FORMAT_CODABAR -> "CODABAR"
                                                Barcode.FORMAT_DATA_MATRIX -> "DATA-MATRIX"
                                                Barcode.FORMAT_PDF417 -> "PDF-417"
                                                else -> "UNKNOWN"
                                            }
                                            isScanning = false
                                            scannedValue = rawValue
                                            onBarcodeScanned(rawValue, format)
                                            break
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis,
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Overlay UI
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.White)
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = {
                        flashEnabled = !flashEnabled
                        camera?.cameraControl?.enableTorch(flashEnabled)
                    }) {
                        Icon(
                            if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Đèn flash",
                            tint = if (flashEnabled) AppColors.Accent else Color.White,
                        )
                    }
                }

                // Scan frame
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    // Scanning frame
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .border(
                                width = 3.dp,
                                color = if (scannedValue != null) AppColors.Secondary else Color.White,
                                shape = RoundedCornerShape(16.dp),
                            )
                            .clip(RoundedCornerShape(16.dp)),
                    )

                    // Guide text
                    Text(
                        if (scannedValue != null) "Đã quét thành công!" else "Đưa mã vạch vào khung hình",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                // Bottom area
                AnimatedVisibility(
                    visible = scannedValue != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = AppColors.Surface,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AppColors.Secondary,
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                scannedValue ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        scannedValue = null
                                        isScanning = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text("Quét lại")
                                }
                                Button(
                                    onClick = onClose,
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text("Xong")
                                }
                            }
                        }
                    }
                }

                // Manual input hint
                if (scannedValue == null) {
                    Spacer(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .height(32.dp),
                    )
                }
            }
        }
    }
}
