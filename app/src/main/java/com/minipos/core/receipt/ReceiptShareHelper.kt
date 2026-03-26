package com.minipos.core.receipt

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.minipos.domain.model.OrderDetail
import com.minipos.domain.model.Store
import java.io.File

/**
 * Helper to share receipts via Android share sheet.
 * Supports sharing as PDF file or plain text.
 */
object ReceiptShareHelper {

    /**
     * Share receipt as a PDF file.
     */
    fun sharePdf(context: Context, pdfFile: File, orderCode: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile,
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Hóa đơn $orderCode")
            putExtra(Intent.EXTRA_TEXT, "Hóa đơn $orderCode từ miniPOS")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Chia sẻ hóa đơn")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Share receipt as plain text (e.g., via messaging apps).
     */
    fun shareText(context: Context, store: Store, detail: OrderDetail) {
        val text = ReceiptGenerator.generateTextReceipt(store, detail)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Hóa đơn ${detail.order.orderCode}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(shareIntent, "Chia sẻ hóa đơn")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Share receipt with options: the user picks PDF or text.
     * For simplicity, we default to PDF sharing.
     */
    suspend fun shareReceipt(
        context: Context,
        store: Store,
        detail: OrderDetail,
        asPdf: Boolean = true,
    ) {
        if (asPdf) {
            val html = ReceiptGenerator.generateHtmlReceipt(store, detail)
            val fileName = "receipt_${detail.order.orderCode.replace("-", "_")}"
            val pdfFile = ReceiptPdfGenerator.generatePdfSimple(context, html, fileName)
            sharePdf(context, pdfFile, detail.order.orderCode)
        } else {
            shareText(context, store, detail)
        }
    }
}
