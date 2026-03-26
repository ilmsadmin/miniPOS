package com.minipos.core.receipt

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.core.utils.DateUtils
import com.minipos.domain.model.OrderDetail
import com.minipos.domain.model.Store

/**
 * Generates bitmap receipt images for thermal printer output.
 * Creates a monochrome bitmap suitable for 58mm/80mm thermal printers.
 */
object ReceiptBitmapGenerator {

    private const val RECEIPT_WIDTH_58MM = 384 // pixels for 58mm paper at 203dpi
    private const val RECEIPT_WIDTH_80MM = 576 // pixels for 80mm paper at 203dpi
    private const val LINE_HEIGHT = 24
    private const val FONT_SIZE_NORMAL = 18f
    private const val FONT_SIZE_SMALL = 14f
    private const val FONT_SIZE_LARGE = 22f
    private const val PADDING = 8

    /**
     * Generate receipt bitmap for thermal printing.
     * @param paperWidth Use RECEIPT_WIDTH_58MM or RECEIPT_WIDTH_80MM
     */
    fun generate(
        store: Store,
        detail: OrderDetail,
        paperWidth: Int = RECEIPT_WIDTH_58MM,
    ): Bitmap {
        val order = detail.order
        val items = detail.items
        val payments = detail.payments

        // First pass: calculate height
        val lines = mutableListOf<ReceiptLine>()

        // Header
        lines.add(ReceiptLine(store.name.uppercase(), Style.BOLD_CENTER, FONT_SIZE_LARGE))
        if (!store.address.isNullOrBlank()) {
            lines.add(ReceiptLine(store.address, Style.CENTER, FONT_SIZE_SMALL))
        }
        if (!store.phone.isNullOrBlank()) {
            lines.add(ReceiptLine("ĐT: ${store.phone}", Style.CENTER, FONT_SIZE_SMALL))
        }
        lines.add(ReceiptLine.DOUBLE_DIVIDER)
        lines.add(ReceiptLine("HÓA ĐƠN BÁN HÀNG", Style.BOLD_CENTER, FONT_SIZE_LARGE))
        lines.add(ReceiptLine.DOUBLE_DIVIDER)

        // Order info
        lines.add(ReceiptLine("Mã: ${order.orderCode}", Style.LEFT, FONT_SIZE_NORMAL))
        lines.add(ReceiptLine("Ngày: ${DateUtils.formatDateTime(order.createdAt)}", Style.LEFT, FONT_SIZE_NORMAL))
        if (!order.customerName.isNullOrBlank()) {
            lines.add(ReceiptLine("KH: ${order.customerName}", Style.LEFT, FONT_SIZE_NORMAL))
        }
        lines.add(ReceiptLine.DIVIDER)

        // Items
        for (item in items) {
            val qty = if (item.quantity == item.quantity.toLong().toDouble()) {
                item.quantity.toLong().toString()
            } else {
                item.quantity.toString()
            }
            lines.add(ReceiptLine(item.productName, Style.BOLD_LEFT, FONT_SIZE_NORMAL))
            if (!item.variantName.isNullOrBlank()) {
                lines.add(ReceiptLine("  (${item.variantName})", Style.LEFT, FONT_SIZE_SMALL))
            }
            lines.add(ReceiptLine(
                "  ${CurrencyFormatter.format(item.unitPrice)} x $qty",
                CurrencyFormatter.format(item.totalPrice),
                FONT_SIZE_NORMAL,
            ))
            if (item.discountAmount > 0) {
                lines.add(ReceiptLine(
                    "  Giảm giá:",
                    "-${CurrencyFormatter.format(item.discountAmount)}",
                    FONT_SIZE_SMALL,
                ))
            }
        }
        lines.add(ReceiptLine.DIVIDER)

        // Totals
        lines.add(ReceiptLine("Tạm tính:", CurrencyFormatter.format(order.subtotal), FONT_SIZE_NORMAL))
        if (order.discountAmount > 0) {
            val discountLabel = when (order.discountType) {
                "percent" -> "Giảm (${order.discountValue.toLong()}%):"
                else -> "Giảm giá:"
            }
            lines.add(ReceiptLine(discountLabel, "-${CurrencyFormatter.format(order.discountAmount)}", FONT_SIZE_NORMAL))
        }
        if (order.taxAmount > 0) {
            lines.add(ReceiptLine("Thuế:", CurrencyFormatter.format(order.taxAmount), FONT_SIZE_NORMAL))
        }
        lines.add(ReceiptLine.DOUBLE_DIVIDER)
        lines.add(ReceiptLine("TỔNG CỘNG:", CurrencyFormatter.format(order.totalAmount), FONT_SIZE_LARGE))
        lines.add(ReceiptLine.DOUBLE_DIVIDER)

        // Payments
        for (payment in payments) {
            lines.add(ReceiptLine(
                "${payment.method.displayName()}:",
                CurrencyFormatter.format(payment.amount),
                FONT_SIZE_NORMAL,
            ))
            if (payment.changeAmount > 0) {
                lines.add(ReceiptLine(
                    "  Tiền thừa:",
                    CurrencyFormatter.format(payment.changeAmount),
                    FONT_SIZE_SMALL,
                ))
            }
        }
        if (payments.isNotEmpty()) {
            lines.add(ReceiptLine.DIVIDER)
        }

        // Notes
        if (!order.notes.isNullOrBlank()) {
            lines.add(ReceiptLine("Ghi chú: ${order.notes}", Style.LEFT, FONT_SIZE_SMALL))
            lines.add(ReceiptLine.DIVIDER)
        }

        // Footer
        lines.add(ReceiptLine(store.settings.receiptHeader, Style.CENTER, FONT_SIZE_NORMAL))
        lines.add(ReceiptLine(store.settings.receiptFooter, Style.CENTER, FONT_SIZE_SMALL))
        lines.add(ReceiptLine("", Style.LEFT, FONT_SIZE_NORMAL)) // blank line at end

        // Calculate total height
        val totalHeight = lines.sumOf { it.heightPx } + PADDING * 2

        // Create bitmap
        val bitmap = Bitmap.createBitmap(paperWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
        }

        var y = PADDING.toFloat()
        for (line in lines) {
            y = drawLine(canvas, paint, line, y, paperWidth)
        }

        return bitmap
    }

    private fun drawLine(canvas: Canvas, paint: Paint, line: ReceiptLine, startY: Float, width: Int): Float {
        paint.textSize = line.fontSize
        paint.typeface = when (line.style) {
            Style.BOLD_LEFT, Style.BOLD_CENTER -> Typeface.DEFAULT_BOLD
            else -> Typeface.DEFAULT
        }

        when {
            line.isDivider -> {
                paint.strokeWidth = 1f
                val y = startY + line.heightPx / 2f
                canvas.drawLine(PADDING.toFloat(), y, (width - PADDING).toFloat(), y, paint)
            }
            line.isDoubleDivider -> {
                paint.strokeWidth = 2f
                val y = startY + line.heightPx / 2f
                canvas.drawLine(PADDING.toFloat(), y - 2, (width - PADDING).toFloat(), y - 2, paint)
                canvas.drawLine(PADDING.toFloat(), y + 2, (width - PADDING).toFloat(), y + 2, paint)
            }
            line.rightText != null -> {
                // Left-right layout
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(line.text, PADDING.toFloat(), startY + line.fontSize, paint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(line.rightText, (width - PADDING).toFloat(), startY + line.fontSize, paint)
            }
            else -> {
                when (line.style) {
                    Style.CENTER, Style.BOLD_CENTER -> {
                        paint.textAlign = Paint.Align.CENTER
                        canvas.drawText(line.text, width / 2f, startY + line.fontSize, paint)
                    }
                    else -> {
                        paint.textAlign = Paint.Align.LEFT
                        canvas.drawText(line.text, PADDING.toFloat(), startY + line.fontSize, paint)
                    }
                }
            }
        }

        return startY + line.heightPx
    }

    // ---- Data classes ----

    private data class ReceiptLine(
        val text: String,
        val style: Style = Style.LEFT,
        val fontSize: Float = FONT_SIZE_NORMAL,
        val rightText: String? = null,
        val isDivider: Boolean = false,
        val isDoubleDivider: Boolean = false,
    ) {
        constructor(left: String, right: String, fontSize: Float) : this(
            text = left, rightText = right, fontSize = fontSize,
        )

        val heightPx: Int
            get() = when {
                isDivider || isDoubleDivider -> 12
                else -> (fontSize * 1.4f).toInt()
            }

        companion object {
            val DIVIDER = ReceiptLine("", isDivider = true)
            val DOUBLE_DIVIDER = ReceiptLine("", isDoubleDivider = true)
        }
    }

    private enum class Style {
        LEFT, CENTER, BOLD_LEFT, BOLD_CENTER
    }
}
