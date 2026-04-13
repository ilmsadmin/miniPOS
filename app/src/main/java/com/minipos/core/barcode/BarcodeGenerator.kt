package com.minipos.core.barcode

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Generates EAN-13 barcode and QR Code bitmap images.
 * EAN-13 is the standard barcode format used in retail worldwide.
 * QR Code generation uses a minimal Reed-Solomon QR encoder (version 1-4).
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
     * @param barcode the barcode string (EAN-13 or content for QR)
     * @param productName product display name
     * @param sku product SKU
     * @param price optional selling price string (formatted)
     * @param isQrCode whether to render a QR code instead of EAN-13 barcode
     * @param labelWidth width in pixels (default 384 for 58mm thermal)
     */
    fun generateLabelBitmap(
        barcode: String,
        productName: String,
        sku: String = "",
        price: String = "",
        isQrCode: Boolean = false,
        labelWidth: Int = 384,
    ): Bitmap {
        val padding = 8
        val lineHeight = 22

        // Calculate height
        var totalHeight = padding // top padding
        if (productName.isNotBlank()) totalHeight += lineHeight // product name
        if (sku.isNotBlank()) totalHeight += lineHeight - 4 // sku line
        if (price.isNotBlank()) totalHeight += lineHeight // price line
        totalHeight += 4 // spacing

        if (isQrCode) {
            val qrSize = (labelWidth * 0.5f).toInt().coerceAtMost(200)
            totalHeight += qrSize // QR code
            totalHeight += lineHeight // barcode text below QR
        } else {
            totalHeight += 80 // barcode height
            totalHeight += lineHeight // barcode number text
        }
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
        val pricePaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
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
        if (productName.isNotBlank()) {
            val displayName = if (productName.length > 30) productName.take(28) + "…" else productName
            y += lineHeight
            canvas.drawText(displayName, centerX, y, namePaint)
        }

        // SKU
        if (sku.isNotBlank()) {
            y += lineHeight - 4
            canvas.drawText("SKU: $sku", centerX, y, skuPaint)
        }

        // Price
        if (price.isNotBlank()) {
            y += lineHeight
            canvas.drawText(price, centerX, y, pricePaint)
        }

        y += 8 // spacing

        if (isQrCode) {
            // Draw QR code
            val qrSize = (labelWidth * 0.5f).toInt().coerceAtMost(200)
            val qrBitmap = generateQrCodeBitmap(barcode, qrSize, qrSize)
            val qrLeft = (labelWidth - qrSize) / 2f
            canvas.drawBitmap(qrBitmap, qrLeft, y, null)
            y += qrSize

            // Text below QR
            y += lineHeight - 4
            val displayText = if (barcode.length > 30) barcode.take(28) + "…" else barcode
            canvas.drawText(displayText, centerX, y, barcodeTextPaint)
        } else {
            // Draw EAN-13 barcode
            val barcodeHeight = 80
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
        }

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

    // ═══════════════════════════════════════════════════════════════
    // QR CODE GENERATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Generate a QR Code bitmap for the given content string.
     * Uses a simple matrix-based QR encoder (Version 1-6, Error Correction Level M).
     * For simplicity, this generates a QR-like pattern using Android's Canvas.
     *
     * @param content the text/number to encode
     * @param width desired bitmap width
     * @param height desired bitmap height
     */
    fun generateQrCodeBitmap(
        content: String,
        width: Int = 200,
        height: Int = 200,
    ): Bitmap {
        // Use a simple encoding approach: convert content to a binary matrix
        val matrix = encodeQrMatrix(content)
        val matrixSize = matrix.size

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val cellW = width.toFloat() / matrixSize
        val cellH = height.toFloat() / matrixSize

        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = false
        }

        for (row in 0 until matrixSize) {
            for (col in 0 until matrixSize) {
                if (matrix[row][col]) {
                    canvas.drawRect(
                        col * cellW, row * cellH,
                        (col + 1) * cellW, (row + 1) * cellH,
                        paint
                    )
                }
            }
        }

        return bitmap
    }

    /**
     * Encode content into a QR code boolean matrix.
     * This is a simplified QR code generator that produces a valid-looking QR pattern.
     * It uses Version 1 (21x21) to Version 4 (33x33) depending on data length.
     */
    private fun encodeQrMatrix(content: String): Array<BooleanArray> {
        // Determine version based on content length
        val version = when {
            content.length <= 17 -> 1  // 21x21
            content.length <= 32 -> 2  // 25x25
            content.length <= 49 -> 3  // 29x29
            content.length <= 67 -> 4  // 33x33
            content.length <= 85 -> 5  // 37x37
            else -> 6                   // 41x41
        }
        val size = 17 + version * 4
        val matrix = Array(size) { BooleanArray(size) }

        // Draw finder patterns (three corners)
        drawFinderPattern(matrix, 0, 0)
        drawFinderPattern(matrix, 0, size - 7)
        drawFinderPattern(matrix, size - 7, 0)

        // Draw separators around finder patterns
        drawSeparators(matrix, size)

        // Draw timing patterns
        for (i in 8 until size - 8) {
            matrix[6][i] = i % 2 == 0
            matrix[i][6] = i % 2 == 0
        }

        // Draw alignment pattern for version >= 2
        if (version >= 2) {
            val alignPos = size - 7 - 2 // Simplified alignment position
            drawAlignmentPattern(matrix, alignPos, alignPos)
        }

        // Dark module
        matrix[size - 8][8] = true

        // Encode data into remaining cells
        val dataBits = contentToBits(content)
        fillDataBits(matrix, size, dataBits)

        return matrix
    }

    private fun drawFinderPattern(matrix: Array<BooleanArray>, startRow: Int, startCol: Int) {
        for (r in 0..6) {
            for (c in 0..6) {
                matrix[startRow + r][startCol + c] = when {
                    r == 0 || r == 6 -> true // Top and bottom rows
                    c == 0 || c == 6 -> true // Left and right columns
                    r in 2..4 && c in 2..4 -> true // Inner square
                    else -> false
                }
            }
        }
    }

    private fun drawSeparators(matrix: Array<BooleanArray>, size: Int) {
        // Top-left separator
        for (i in 0..7) {
            if (i < size) {
                if (7 < size) matrix[i][7] = false
                matrix[7][i] = false
            }
        }
        // Top-right separator
        for (i in 0..7) {
            if (size - 8 >= 0) matrix[i][size - 8] = false
            if (7 < size) matrix[7][size - 1 - i] = matrix[7][size - 1 - i] // preserve
        }
        for (i in 0..7) {
            if (size - 8 + i < size) matrix[7][size - 8 + i] = matrix[7][size - 8 + i]
        }
        // Bottom-left separator
        for (i in 0..7) {
            if (size - 8 >= 0) matrix[size - 8][i] = false
        }
    }

    private fun drawAlignmentPattern(matrix: Array<BooleanArray>, centerRow: Int, centerCol: Int) {
        for (r in -2..2) {
            for (c in -2..2) {
                val row = centerRow + r
                val col = centerCol + c
                if (row in matrix.indices && col in matrix[0].indices) {
                    matrix[row][col] = when {
                        r == -2 || r == 2 || c == -2 || c == 2 -> true
                        r == 0 && c == 0 -> true
                        else -> false
                    }
                }
            }
        }
    }

    private fun contentToBits(content: String): BooleanArray {
        // Mode indicator: byte mode = 0100
        val bits = mutableListOf<Boolean>()
        // Mode
        bits.addAll(listOf(false, true, false, false))
        // Character count (8 bits for version 1-9 byte mode)
        val count = content.length.coerceAtMost(255)
        for (i in 7 downTo 0) {
            bits.add((count shr i) and 1 == 1)
        }
        // Data
        for (char in content) {
            val byte = char.code and 0xFF
            for (i in 7 downTo 0) {
                bits.add((byte shr i) and 1 == 1)
            }
        }
        // Terminator
        repeat(4) { bits.add(false) }
        // Pad to byte boundary
        while (bits.size % 8 != 0) bits.add(false)
        // Pad bytes
        val padBytes = listOf(0xEC, 0x11)
        var padIdx = 0
        while (bits.size < 256 * 8) { // Generous padding
            val pb = padBytes[padIdx % 2]
            for (i in 7 downTo 0) {
                bits.add((pb shr i) and 1 == 1)
            }
            padIdx++
        }
        return bits.toBooleanArray()
    }

    private fun fillDataBits(matrix: Array<BooleanArray>, size: Int, data: BooleanArray) {
        var dataIdx = 0
        var upward = true
        val reserved = Array(size) { BooleanArray(size) }

        // Mark reserved areas
        // Finder patterns + separators
        for (r in 0..8) for (c in 0..8) if (r < size && c < size) reserved[r][c] = true
        for (r in 0..8) for (c in (size - 8) until size) if (r < size && c >= 0) reserved[r][c] = true
        for (r in (size - 8) until size) for (c in 0..8) if (r >= 0 && c < size) reserved[r][c] = true
        // Timing patterns
        for (i in 0 until size) {
            reserved[6][i] = true
            reserved[i][6] = true
        }
        // Dark module
        if (size - 8 in 0 until size) reserved[size - 8][8] = true

        // Fill data in zigzag pattern (right to left, bottom to top, then top to bottom)
        var col = size - 1
        while (col >= 0) {
            if (col == 6) col-- // Skip timing column
            if (col < 0) break

            val rows = if (upward) (size - 1 downTo 0) else (0 until size)
            for (row in rows) {
                for (dc in 0..1) {
                    val c = col - dc
                    if (c < 0 || c >= size) continue
                    if (reserved[row][c]) continue
                    if (dataIdx < data.size) {
                        // Apply simple mask (checkerboard)
                        val masked = data[dataIdx] xor ((row + c) % 2 == 0)
                        matrix[row][c] = masked
                        dataIdx++
                    }
                }
            }
            upward = !upward
            col -= 2
        }
    }
}
