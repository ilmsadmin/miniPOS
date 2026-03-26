package com.minipos.core.barcode

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Generates EAN-13 barcode numbers and bitmap images.
 * EAN-13 is the standard barcode format used in retail worldwide.
 */
object BarcodeGenerator {

    // EAN-13 encoding tables
    private val L_PATTERNS = arrayOf(
        "0001101", "0011001", "0010011", "0111101", "0100011",
        "0110001", "0101111", "0111011", "0110111", "0001011"
    )
    private val G_PATTERNS = arrayOf(
        "0100111", "0110011", "0011011", "0100001", "0011101",
        "0111001", "0000101", "0010001", "0001001", "0010111"
    )
    private val R_PATTERNS = arrayOf(
        "1110010", "1100110", "1101100", "1000010", "1011100",
        "1001110", "1010000", "1000100", "1001000", "1110100"
    )

    // First digit encoding pattern (which L/G pattern to use for digits 2-7)
    private val FIRST_DIGIT_PATTERNS = arrayOf(
        "LLLLLL", "LLGLGG", "LLGGLG", "LLGGGL", "LGLLGG",
        "LGGLLG", "LGGGLL", "LGLGLG", "LGLGGL", "LGGLGL"
    )

    /**
     * Generate a unique EAN-13 barcode number.
     * Format: 200 (in-store prefix) + 9 digits from store/product + check digit
     * @param storeCode short numeric derived from store
     * @param sequence auto-incrementing product number
     */
    fun generateEan13(storeCode: String, sequence: Int): String {
        // Use prefix "200" for in-store use (GS1 standard for internal use: 200-299)
        val storeNum = storeCode.filter { it.isDigit() }.take(3).padStart(3, '0')
        val seqStr = sequence.toString().padStart(6, '0').takeLast(6)
        val base = "2${storeNum.take(2)}${seqStr}" // 12 digits without check digit

        val padded = base.padEnd(12, '0').take(12)
        val checkDigit = calculateEan13CheckDigit(padded)
        return padded + checkDigit
    }

    /**
     * Calculate the EAN-13 check digit for a 12-digit string.
     */
    fun calculateEan13CheckDigit(first12: String): Int {
        require(first12.length == 12) { "Need exactly 12 digits" }
        var sum = 0
        for (i in 0 until 12) {
            val digit = first12[i] - '0'
            sum += if (i % 2 == 0) digit else digit * 3
        }
        return (10 - (sum % 10)) % 10
    }

    /**
     * Validate an EAN-13 barcode (13 digits with valid check digit).
     */
    fun isValidEan13(barcode: String): Boolean {
        if (barcode.length != 13 || !barcode.all { it.isDigit() }) return false
        val checkDigit = calculateEan13CheckDigit(barcode.substring(0, 12))
        return checkDigit == (barcode[12] - '0')
    }

    /**
     * Encode an EAN-13 barcode as a binary string (1=black, 0=white).
     */
    private fun encodeEan13(barcode: String): String {
        require(barcode.length == 13)
        val firstDigit = barcode[0] - '0'
        val pattern = FIRST_DIGIT_PATTERNS[firstDigit]

        val sb = StringBuilder()
        // Start guard
        sb.append("101")

        // Left group (digits 2-7, indices 1-6)
        for (i in 0 until 6) {
            val digit = barcode[i + 1] - '0'
            if (pattern[i] == 'L') {
                sb.append(L_PATTERNS[digit])
            } else {
                sb.append(G_PATTERNS[digit])
            }
        }

        // Center guard
        sb.append("01010")

        // Right group (digits 8-13, indices 7-12)
        for (i in 7..12) {
            val digit = barcode[i] - '0'
            sb.append(R_PATTERNS[digit])
        }

        // End guard
        sb.append("101")

        return sb.toString()
    }

    /**
     * Generate a bitmap image of an EAN-13 barcode.
     * @param barcode 13-digit EAN-13 string
     * @param width desired bitmap width in pixels
     * @param height desired bitmap height in pixels
     * @param showText whether to render the number below the barcode
     */
    fun generateBarcodeBitmap(
        barcode: String,
        width: Int = 300,
        height: Int = 150,
        showText: Boolean = true,
    ): Bitmap {
        val binary = encodeEan13(barcode)
        val textHeight = if (showText) 28 else 0
        val barcodeHeight = height - textHeight
        val barWidth = width.toFloat() / binary.length

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val barPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = false
        }

        // Draw bars
        for (i in binary.indices) {
            if (binary[i] == '1') {
                val left = i * barWidth
                val right = left + barWidth
                canvas.drawRect(left, 0f, right, barcodeHeight.toFloat(), barPaint)
            }
        }

        // Draw text
        if (showText) {
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 18f
                typeface = Typeface.MONOSPACE
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val textY = height.toFloat() - 4f
            // Format: first digit  left group  right group
            val formatted = "${barcode[0]}  ${barcode.substring(1, 7)}  ${barcode.substring(7, 13)}"
            canvas.drawText(formatted, width / 2f, textY, textPaint)
        }

        return bitmap
    }

    /**
     * Generate a label bitmap with barcode and product name.
     * Suitable for printing on label paper or thermal printer.
     * @param barcode the EAN-13 barcode string
     * @param productName product display name
     * @param sku product SKU
     * @param labelWidth width in pixels (default 384 for 58mm thermal)
     */
    fun generateLabelBitmap(
        barcode: String,
        productName: String,
        sku: String = "",
        labelWidth: Int = 384,
    ): Bitmap {
        val barcodeHeight = 80
        val padding = 8
        val lineHeight = 22

        // Calculate height
        var totalHeight = padding // top padding
        totalHeight += lineHeight // product name
        if (sku.isNotBlank()) totalHeight += lineHeight - 4 // sku line
        totalHeight += 4 // spacing
        totalHeight += barcodeHeight // barcode
        totalHeight += lineHeight // barcode number text
        totalHeight += padding // bottom padding

        val bitmap = Bitmap.createBitmap(labelWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val namePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val skuPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 13f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val barcodePaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = false
        }
        val barcodeTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val centerX = labelWidth / 2f
        var y = padding.toFloat()

        // Product name (truncate if too long)
        val displayName = if (productName.length > 30) productName.take(28) + "…" else productName
        y += lineHeight
        canvas.drawText(displayName, centerX, y, namePaint)

        // SKU
        if (sku.isNotBlank()) {
            y += lineHeight - 4
            canvas.drawText("SKU: $sku", centerX, y, skuPaint)
        }

        y += 8 // spacing

        // Draw barcode
        val binary = encodeEan13(barcode)
        val barcodeAreaWidth = labelWidth - padding * 6
        val barWidth = barcodeAreaWidth.toFloat() / binary.length
        val barcodeLeft = (labelWidth - barcodeAreaWidth) / 2f

        for (i in binary.indices) {
            if (binary[i] == '1') {
                val left = barcodeLeft + i * barWidth
                val right = left + barWidth
                canvas.drawRect(left, y, right, y + barcodeHeight, barcodePaint)
            }
        }
        y += barcodeHeight

        // Barcode number
        y += lineHeight - 4
        val formatted = "${barcode[0]} ${barcode.substring(1, 7)} ${barcode.substring(7, 13)}"
        canvas.drawText(formatted, centerX, y, barcodeTextPaint)

        return bitmap
    }

    /**
     * Generate a combined bitmap containing multiple barcode labels.
     * Suitable for batch printing on thermal printers.
     * @param labels list of individual label bitmaps
     * @param labelWidth width of each label
     * @param spacing vertical spacing between labels in pixels
     */
    fun combineLabelBitmaps(
        labels: List<Bitmap>,
        labelWidth: Int = 384,
        spacing: Int = 16,
    ): Bitmap {
        if (labels.isEmpty()) return Bitmap.createBitmap(labelWidth, 1, Bitmap.Config.ARGB_8888)

        val totalHeight = labels.sumOf { it.height } + spacing * (labels.size - 1) + spacing * 2
        val bitmap = Bitmap.createBitmap(labelWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // Dashed line paint for separators
        val dashPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 4f), 0f)
        }

        var y = spacing.toFloat()
        labels.forEachIndexed { index, label ->
            canvas.drawBitmap(label, (labelWidth - label.width) / 2f, y, null)
            y += label.height
            if (index < labels.size - 1) {
                y += spacing / 2f
                canvas.drawLine(8f, y, labelWidth - 8f, y, dashPaint)
                y += spacing / 2f
            }
        }

        return bitmap
    }
}
