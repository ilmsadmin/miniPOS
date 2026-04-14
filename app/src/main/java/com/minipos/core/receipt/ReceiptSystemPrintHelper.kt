package com.minipos.core.receipt

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.minipos.domain.model.OrderDetail
import com.minipos.domain.model.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Helper to print receipts via Android's built-in Print Framework.
 * Uses the system "Print" dialog which supports WiFi / Bluetooth / Cloud printers
 * that are already configured on the device — no manual Bluetooth management needed.
 */
object ReceiptSystemPrintHelper {

    /**
     * Generate a PDF from the order detail and open the system print dialog.
     * The user can then choose any printer registered on their device.
     */
    suspend fun printViaSystemDialog(
        context: Context,
        store: Store,
        detail: OrderDetail,
    ) {
        val fileName = "receipt_${detail.order.orderCode.replace("-", "_")}"

        // Generate the PDF file on IO thread
        val pdfFile = withContext(Dispatchers.IO) {
            ReceiptPdfGenerator.generatePdf(context, store, detail, fileName)
        }

        // Get the PrintManager service
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager

        val jobName = "ViPOS - ${detail.order.orderCode}"

        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?,
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()

                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?,
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    return
                }

                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        // Open the system print dialog
        printManager.print(jobName, adapter, PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        )
    }
}
