package com.minipos.core.barcode

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.minipos.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Handles saving barcode label bitmaps as PDF and sharing them.
 */
object BarcodePrintHelper {

    /**
     * Save a combined barcode labels bitmap as a PDF file.
     * The PDF uses the bitmap width as page width, suitable for thermal printers.
     */
    suspend fun saveLabelsPdf(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "barcodes",
    ): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "barcodes")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val file = File(cacheDir, "${fileName}.pdf")

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        document.finishPage(page)

        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        file
    }

    /**
     * Save a combined bitmap as a PNG image file.
     */
    suspend fun saveLabelsImage(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "barcodes",
    ): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "barcodes")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val file = File(cacheDir, "${fileName}.png")
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        file
    }

    /**
     * Share barcode labels as a PDF file via Android share sheet.
     */
    fun sharePdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile,
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_barcode_subject))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_barcode_chooser))
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Share barcode labels as a PNG image via Android share sheet.
     */
    fun shareImage(context: Context, imageFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile,
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_barcode_subject))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_barcode_chooser))
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Open a print intent to print barcode labels as image.
     */
    fun printViaSystemDialog(context: Context, imageFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile,
        )
        val printIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(printIntent, context.getString(R.string.print_barcode_chooser))
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
