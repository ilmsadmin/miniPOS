package com.minipos.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Helper for capturing, resizing, and managing product images.
 * Images are stored in app-private internal storage to avoid permission issues.
 * All images are resized to small dimensions suitable for POS use.
 */
object ImageHelper {

    private const val IMAGE_DIR = "product_images"
    private const val MAX_IMAGE_WIDTH = 480
    private const val MAX_IMAGE_HEIGHT = 480
    private const val JPEG_QUALITY = 70

    /**
     * Get the directory for storing product images.
     */
    fun getImageDir(context: Context): File {
        val dir = File(context.filesDir, IMAGE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Create a temporary file for camera capture.
     */
    fun createTempImageFile(context: Context): File {
        val dir = File(context.cacheDir, "camera_temp")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "capture_${UUID.randomUUID()}.jpg")
    }

    /**
     * Process and save an image from a URI (gallery pick or camera capture).
     * Returns the internal file path of the saved image, or null on failure.
     */
    fun processAndSaveImage(context: Context, sourceUri: Uri, productId: String, index: Int = 0): String? {
        return try {
            val bitmap = decodeBitmapFromUri(context, sourceUri) ?: return null
            val resized = resizeBitmap(bitmap, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT)
            val rotated = correctRotation(context, sourceUri, resized)
            val fileName = "${productId}_${index}_${System.currentTimeMillis()}.jpg"
            saveBitmap(context, rotated, fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Process and save a bitmap directly (from camera preview/capture).
     */
    fun processAndSaveBitmap(context: Context, bitmap: Bitmap, productId: String, index: Int = 0): String? {
        return try {
            val resized = resizeBitmap(bitmap, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT)
            val fileName = "${productId}_${index}_${System.currentTimeMillis()}.jpg"
            saveBitmap(context, resized, fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Save a bitmap to internal storage.
     * Returns the absolute file path.
     */
    private fun saveBitmap(context: Context, bitmap: Bitmap, fileName: String): String {
        val dir = getImageDir(context)
        val file = File(dir, fileName)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
        }
        return file.absolutePath
    }

    /**
     * Decode bitmap from URI with memory-efficient sampling.
     */
    private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        // First pass: get dimensions
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        // Calculate sample size
        options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT)
        options.inJustDecodeBounds = false

        // Second pass: decode with sample size
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    /**
     * Calculate optimal inSampleSize for memory-efficient decoding.
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Resize bitmap to fit within max dimensions while maintaining aspect ratio.
     */
    private fun resizeBitmap(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxWidth && height <= maxHeight) return source

        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }

    /**
     * Correct image rotation based on EXIF data.
     */
    private fun correctRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
            inputStream.close()

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }

    /**
     * Delete a product image file.
     */
    fun deleteImage(path: String): Boolean {
        return try {
            File(path).delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete all images for a product.
     */
    fun deleteProductImages(context: Context, productId: String) {
        val dir = getImageDir(context)
        dir.listFiles()?.forEach { file ->
            if (file.name.startsWith("${productId}_")) {
                file.delete()
            }
        }
    }

    /**
     * Get all image paths for a product.
     */
    fun getProductImagePaths(context: Context, productId: String): List<String> {
        val dir = getImageDir(context)
        return dir.listFiles()
            ?.filter { it.name.startsWith("${productId}_") }
            ?.sortedBy { it.name }
            ?.map { it.absolutePath }
            ?: emptyList()
    }

    /**
     * Get file for a given path; null if doesn't exist.
     */
    fun getImageFile(path: String): File? {
        val file = File(path)
        return if (file.exists()) file else null
    }

    /**
     * Clean up orphaned temp files older than 1 hour.
     */
    fun cleanupTempFiles(context: Context) {
        val dir = File(context.cacheDir, "camera_temp")
        if (!dir.exists()) return
        val oneHourAgo = System.currentTimeMillis() - 60 * 60 * 1000
        dir.listFiles()?.forEach { file ->
            if (file.lastModified() < oneHourAgo) {
                file.delete()
            }
        }
    }
}
