package com.minipos.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.minipos.core.theme.AppColors
import com.minipos.core.utils.ImageHelper
import java.io.File

/**
 * Image picker component for product images.
 * Supports camera capture and gallery selection.
 * Shows thumbnails with add/remove functionality.
 */
@Composable
fun ProductImagePicker(
    mainImagePath: String?,
    additionalImages: List<String>,
    onMainImageChanged: (String?) -> Unit,
    onAdditionalImagesChanged: (List<String>) -> Unit,
    productId: String,
    maxImages: Int = 5,
) {
    val context = LocalContext.current
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var isPickingMainImage by remember { mutableStateOf(true) }

    // Image viewer state
    var showImageViewer by remember { mutableStateOf(false) }
    var viewerInitialIndex by remember { mutableIntStateOf(0) }

    // Build the combined image list for the viewer
    val allImages = remember(mainImagePath, additionalImages) {
        buildList {
            mainImagePath?.let { add(it) }
            addAll(additionalImages)
        }
    }

    // Camera permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    // Camera capture
    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraFile != null) {
            val uri = Uri.fromFile(tempCameraFile!!)
            val index = if (isPickingMainImage) 0 else additionalImages.size + 1
            val savedPath = ImageHelper.processAndSaveImage(context, uri, productId, index)
            if (savedPath != null) {
                if (isPickingMainImage) {
                    onMainImageChanged(savedPath)
                } else {
                    onAdditionalImagesChanged(additionalImages + savedPath)
                }
            }
            tempCameraFile?.delete()
        }
    }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val index = if (isPickingMainImage) 0 else additionalImages.size + 1
            val savedPath = ImageHelper.processAndSaveImage(context, it, productId, index)
            if (savedPath != null) {
                if (isPickingMainImage) {
                    onMainImageChanged(savedPath)
                } else {
                    onAdditionalImagesChanged(additionalImages + savedPath)
                }
            }
        }
    }

    fun launchCamera() {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        val file = ImageHelper.createTempImageFile(context)
        tempCameraFile = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraLauncher.launch(uri)
    }

    fun launchGallery() {
        galleryLauncher.launch("image/*")
    }

    // Source selection dialog
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            icon = { Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = AppColors.Primary) },
            title = { Text("Chọn ảnh") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Camera option
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImageSourceDialog = false
                                launchCamera()
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = AppColors.SurfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = AppColors.Primary,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Chụp ảnh", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Sử dụng camera để chụp",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextSecondary,
                                )
                            }
                        }
                    }

                    // Gallery option
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImageSourceDialog = false
                                launchGallery()
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = AppColors.SurfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = AppColors.Secondary,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Chọn từ thư viện", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Chọn ảnh có sẵn trong điện thoại",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextSecondary,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageSourceDialog = false }) { Text("Đóng") }
            },
        )
    }

    // Image viewer fullscreen dialog
    if (showImageViewer && allImages.isNotEmpty()) {
        Dialog(
            onDismissRequest = { showImageViewer = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            ImageViewerScreen(
                images = allImages,
                initialIndex = viewerInitialIndex,
                onClose = { showImageViewer = false },
            )
        }
    }

    Column {
        Text(
            "Ảnh sản phẩm",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Main image
            item {
                ImageThumbnail(
                    imagePath = mainImagePath,
                    label = "Ảnh đại diện",
                    isMain = true,
                    onClick = {
                        if (mainImagePath != null) {
                            viewerInitialIndex = 0
                            showImageViewer = true
                        } else {
                            isPickingMainImage = true
                            showImageSourceDialog = true
                        }
                    },
                    onRemove = if (mainImagePath != null) {
                        {
                            ImageHelper.deleteImage(mainImagePath)
                            onMainImageChanged(null)
                        }
                    } else null,
                )
            }

            // Additional images
            itemsIndexed(additionalImages) { index, path ->
                ImageThumbnail(
                    imagePath = path,
                    label = "Ảnh ${index + 2}",
                    isMain = false,
                    onClick = {
                        // Open viewer at the correct index (offset by 1 if main image exists)
                        viewerInitialIndex = if (mainImagePath != null) index + 1 else index
                        showImageViewer = true
                    },
                    onRemove = {
                        ImageHelper.deleteImage(path)
                        onAdditionalImagesChanged(additionalImages.toMutableList().apply { removeAt(index) })
                    },
                )
            }

            // Add button (if under max)
            val totalImages = (if (mainImagePath != null) 1 else 0) + additionalImages.size
            if (totalImages < maxImages) {
                item {
                    AddImageButton(
                        onClick = {
                            isPickingMainImage = mainImagePath == null
                            showImageSourceDialog = true
                        },
                        remaining = maxImages - totalImages,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageThumbnail(
    imagePath: String?,
    label: String,
    isMain: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier.size(80.dp),
    ) {
        if (imagePath != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(imagePath))
                    .crossfade(true)
                    .build(),
                contentDescription = label,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = if (isMain) 2.dp else 1.dp,
                        color = if (isMain) AppColors.Primary else AppColors.Border,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable(onClick = onClick),
                contentScale = ContentScale.Crop,
            )

            // Remove button
            if (onRemove != null) {
                Surface(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .clickable { onRemove() },
                    shape = CircleShape,
                    color = AppColors.Error,
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Xóa",
                        modifier = Modifier.size(14.dp).padding(2.dp),
                        tint = Color.White,
                    )
                }
            }

            // Main badge
            if (isMain) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(2.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = AppColors.Primary.copy(alpha = 0.9f),
                ) {
                    Text(
                        "Chính",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }
        } else {
            // Empty placeholder
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(8.dp),
                color = AppColors.SurfaceVariant,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = "Thêm ảnh",
                        modifier = Modifier.size(24.dp),
                        tint = AppColors.TextTertiary,
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextTertiary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddImageButton(
    onClick: () -> Unit,
    remaining: Int,
) {
    Surface(
        modifier = Modifier
            .size(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = AppColors.SurfaceVariant,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Thêm ảnh",
                modifier = Modifier.size(28.dp),
                tint = AppColors.Primary,
            )
            Text(
                "+$remaining",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary,
            )
        }
    }
}
