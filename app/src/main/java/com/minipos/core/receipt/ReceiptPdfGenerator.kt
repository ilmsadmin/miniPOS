package com.minipos.core.receipt

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.core.utils.DateUtils
import com.minipos.domain.model.OrderDetail
import com.minipos.domain.model.Store
import java.io.File
import java.io.FileOutputStream

/**
 * Generates PDF receipts using Android PdfDocument canvas drawing.
 * This approach is reliable and does not require WebView or a UI context.
 */
object ReceiptPdfGenerator {

    // A4 dimensions in points at 72dpi
    private const val PAGE_WIDTH = 595
    private const val MARGIN = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2

    // Text sizes
    private const val SIZE_TITLE = 18f
    private const val SIZE_STORE_NAME = 16f
    private const val SIZE_BODY = 12f
    private const val SIZE_SMALL = 10f

    // Line heights
    private const val LINE_NORMAL = 18f
    private const val LINE_SMALL = 15f
    private const val LINE_HEADING = 24f

    /**
     * Generate a PDF file from order detail.
     * This is a synchronous, blocking call — run on IO dispatcher if needed.
     */
    fun generatePdf(context: Context, store: Store, detail: OrderDetail, fileName: String): File {
        val order = detail.order
        val items = detail.items
        val payments = detail.payments

        // Estimate content height: header + items + summary + footer
        val estimatedLines = 20 + items.size * 3 + payments.size * 2 + 10
        val estimatedHeight = (estimatedLines * LINE_NORMAL + 300).toInt().coerceAtLeast(842)

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, estimatedHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val normalPaint = Paint().apply {
            textSize = SIZE_BODY
            color = android.graphics.Color.BLACK
            isAntiAlias = true
        }
        val boldPaint = Paint(normalPaint).apply {
            typeface = Typeface.DEFAULT_BOLD
        }
        val smallPaint = Paint(normalPaint).apply {
            textSize = SIZE_SMALL
            color = android.graphics.Color.DKGRAY
        }
        val titlePaint = Paint(boldPaint).apply {
            textSize = SIZE_TITLE
            textAlign = Paint.Align.CENTER
        }
        val storeNamePaint = Paint(boldPaint).apply {
            textSize = SIZE_STORE_NAME
            textAlign = Paint.Align.CENTER
        }
        val centerSmallPaint = Paint(smallPaint).apply {
            textAlign = Paint.Align.CENTER
        }
        val rightPaint = Paint(normalPaint).apply {
            textAlign = Paint.Align.RIGHT
        }
        val rightBoldPaint = Paint(boldPaint).apply {
            textAlign = Paint.Align.RIGHT
        }

        var y = MARGIN + 20f
        val cx = PAGE_WIDTH / 2f
        val rightX = MARGIN + CONTENT_WIDTH

        // ---- Store header ----
        canvas.drawText(store.name.uppercase(), cx, y, storeNamePaint)
        y += LINE_HEADING

        if (!store.address.isNullOrBlank()) {
            canvas.drawText(store.address, cx, y, centerSmallPaint)
            y += LINE_SMALL
        }
        if (!store.phone.isNullOrBlank()) {
            canvas.drawText("Tel: ${store.phone}", cx, y, centerSmallPaint)
            y += LINE_SMALL
        }
        if (store.settings.receiptHeader.isNotBlank()) {
            canvas.drawText(store.settings.receiptHeader, cx, y, centerSmallPaint)
            y += LINE_SMALL
        }

        y += 6f
        y += 8f

        // ---- Receipt title ----
        canvas.drawText("HÓA ĐƠN BÁN HÀNG", cx, y, titlePaint)
        y += LINE_HEADING

        y += 12f

        // ---- Order info ----
        canvas.drawText("Mã đơn: ", MARGIN, y, boldPaint)
        canvas.drawText(order.orderCode, MARGIN + boldPaint.measureText("Mã đơn: "), y, normalPaint)
        y += LINE_NORMAL

        canvas.drawText("Ngày: ", MARGIN, y, boldPaint)
        canvas.drawText(DateUtils.formatDateTime(order.createdAt), MARGIN + boldPaint.measureText("Ngày: "), y, normalPaint)
        y += LINE_NORMAL

        if (!order.customerName.isNullOrBlank()) {
            canvas.drawText("Khách hàng: ", MARGIN, y, boldPaint)
            canvas.drawText(order.customerName, MARGIN + boldPaint.measureText("Khách hàng: "), y, normalPaint)
            y += LINE_NORMAL
        }

        y += 4f
        y += 10f

        // ---- Items header ----
        canvas.drawText("Sản phẩm", MARGIN, y, boldPaint)
        canvas.drawText("Thành tiền", rightX, y, rightBoldPaint)
        y += LINE_NORMAL
        y += 8f

        // ---- Items ----
        for (item in items) {
            val qty = if (item.quantity == item.quantity.toLong().toDouble()) {
                item.quantity.toLong().toString()
            } else {
                item.quantity.toString()
            }

            // Product name (may need wrapping)
            val nameText = item.productName + if (!item.variantName.isNullOrBlank()) " (${item.variantName})" else ""
            canvas.drawText(nameText, MARGIN, y, boldPaint)
            canvas.drawText(CurrencyFormatter.format(item.totalPrice), rightX, y, rightPaint)
            y += LINE_NORMAL

            val priceDetail = "${CurrencyFormatter.format(item.unitPrice)} × $qty"
            canvas.drawText(priceDetail, MARGIN + 8f, y, smallPaint)
            y += LINE_SMALL

            if (item.discountAmount > 0) {
                val discPaint = Paint(smallPaint).apply { color = android.graphics.Color.RED }
                canvas.drawText("  Giảm: -${CurrencyFormatter.format(item.discountAmount)}", MARGIN + 8f, y, discPaint)
                y += LINE_SMALL
            }
        }

        y += 4f
        y += 10f

        // ---- Summary ----
        fun drawLabelValue(canvas: Canvas, label: String, value: String, y: Float, lPaint: Paint = normalPaint, rPaint: Paint = normalPaint): Float {
            canvas.drawText(label, MARGIN, y, lPaint)
            canvas.drawText(value, rightX, y, if (rPaint.textAlign == Paint.Align.RIGHT) rPaint else Paint(rPaint).apply { textAlign = Paint.Align.RIGHT })
            return y + LINE_NORMAL
        }

        y = drawLabelValue(canvas, "Tạm tính:", CurrencyFormatter.format(order.subtotal), y)

        if (order.discountAmount > 0) {
            val discountLabel = when (order.discountType) {
                "percent" -> "Giảm giá (${order.discountValue.toLong()}%):"
                else -> "Giảm giá:"
            }
            val discPaint = Paint(rightPaint).apply { color = android.graphics.Color.RED }
            y = drawLabelValue(canvas, discountLabel, "-${CurrencyFormatter.format(order.discountAmount)}", y, normalPaint, discPaint)
        }

        if (order.taxAmount > 0) {
            y = drawLabelValue(canvas, "Thuế:", CurrencyFormatter.format(order.taxAmount), y)
        }

        y += 10f

        // Grand total
        val bigTotalPaint = Paint(boldPaint).apply { textSize = SIZE_TITLE }
        val bigTotalRightPaint = Paint(bigTotalPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("TỔNG CỘNG:", MARGIN, y, bigTotalPaint)
        canvas.drawText(CurrencyFormatter.format(order.totalAmount), rightX, y, bigTotalRightPaint)
        y += LINE_HEADING

        y += 10f

        // ---- Payments ----
        if (payments.isNotEmpty()) {
            for (payment in payments) {
                val methodName = payment.method.displayName(context)
                y = drawLabelValue(canvas, "$methodName:", CurrencyFormatter.format(payment.amount), y, boldPaint, rightPaint)
                if ((payment.receivedAmount ?: 0.0) > payment.amount) {
                    y = drawLabelValue(canvas, "  Tiền nhận:", CurrencyFormatter.format(payment.receivedAmount ?: 0.0), y, smallPaint, rightPaint)
                }
                if (payment.changeAmount > 0) {
                    y = drawLabelValue(canvas, "  Tiền thừa:", CurrencyFormatter.format(payment.changeAmount), y, smallPaint, rightPaint)
                }
            }
            y += 4f
            y += 10f
        }

        // ---- Notes ----
        if (!order.notes.isNullOrBlank()) {
            canvas.drawText("Ghi chú: ${order.notes}", MARGIN, y, smallPaint)
            y += LINE_SMALL
            y += 10f
        }

        // ---- Footer ----
        if (store.settings.receiptFooter.isNotBlank()) {
            canvas.drawText(store.settings.receiptFooter, cx, y, centerSmallPaint)
            y += LINE_SMALL
        }

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val outputFile = File(outputDir, "$fileName.pdf")
        FileOutputStream(outputFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        return outputFile
    }

    /**
     * Clean up old cached receipt files.
     */
    fun cleanupCache(context: Context) {
        val dir = File(context.cacheDir, "receipts")
        if (dir.exists()) {
            val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
            dir.listFiles()?.forEach { file ->
                if (file.lastModified() < oneDayAgo) file.delete()
            }
        }
    }
}
