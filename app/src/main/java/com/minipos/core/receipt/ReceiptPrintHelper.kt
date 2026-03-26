package com.minipos.core.receipt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import com.minipos.domain.model.OrderDetail
import com.minipos.domain.model.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

/**
 * Helper for printing receipts via Bluetooth thermal printer (ESC/POS protocol).
 * Supports common 58mm and 80mm thermal printers.
 */
object ReceiptPrintHelper {

    // Standard Serial Port Service UUID for Bluetooth printers
    private val PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // ESC/POS Commands
    private val ESC_INIT = byteArrayOf(0x1B, 0x40) // Initialize printer
    private val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01) // Center alignment
    private val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00) // Left alignment
    private val ESC_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02) // Right alignment
    private val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01) // Bold on
    private val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00) // Bold off
    private val ESC_DOUBLE_HEIGHT = byteArrayOf(0x1B, 0x21, 0x10) // Double height
    private val ESC_NORMAL_SIZE = byteArrayOf(0x1B, 0x21, 0x00) // Normal size
    private val ESC_CUT = byteArrayOf(0x1D, 0x56, 0x00) // Full cut
    private val ESC_PARTIAL_CUT = byteArrayOf(0x1D, 0x56, 0x01) // Partial cut
    private val ESC_FEED = byteArrayOf(0x1B, 0x64, 0x04) // Feed 4 lines
    private val LF = byteArrayOf(0x0A) // Line feed

    /**
     * Get list of paired Bluetooth devices that might be printers.
     */
    @SuppressLint("MissingPermission")
    fun getPairedPrinters(context: Context): List<BluetoothDevice> {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return try {
            adapter.bondedDevices
                ?.filter { device ->
                    // Filter for likely printer devices
                    val name = device.name?.lowercase() ?: ""
                    name.contains("print") ||
                    name.contains("pos") ||
                    name.contains("thermal") ||
                    name.contains("receipt") ||
                    name.contains("pt-") ||
                    name.contains("rpp") ||
                    name.contains("spp") ||
                    // Major device class for printers / imaging
                    device.bluetoothClass?.majorDeviceClass == 0x0600
                }
                ?.toList()
                ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    /**
     * Get all paired Bluetooth devices (let user pick the printer).
     */
    @SuppressLint("MissingPermission")
    fun getAllPairedDevices(context: Context): List<BluetoothDevice> {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return try {
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    /**
     * Print a receipt to a Bluetooth thermal printer using ESC/POS text commands.
     */
    @SuppressLint("MissingPermission")
    suspend fun printReceipt(
        device: BluetoothDevice,
        store: Store,
        detail: OrderDetail,
    ): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(PRINTER_UUID)
            socket.connect()
            val output = socket.outputStream

            // Generate and send text receipt via ESC/POS
            sendTextReceipt(output, store, detail)

            // Feed and cut
            output.write(ESC_FEED)
            output.write(ESC_PARTIAL_CUT)
            output.flush()

            kotlin.Result.success(Unit)
        } catch (e: IOException) {
            kotlin.Result.failure(Exception("Lỗi kết nối máy in: ${e.message}"))
        } catch (e: SecurityException) {
            kotlin.Result.failure(Exception("Không có quyền Bluetooth: ${e.message}"))
        } catch (e: Exception) {
            kotlin.Result.failure(Exception("Lỗi in hóa đơn: ${e.message}"))
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Print a bitmap to a Bluetooth thermal printer.
     */
    @SuppressLint("MissingPermission")
    suspend fun printBitmap(
        device: BluetoothDevice,
        bitmap: Bitmap,
    ): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(PRINTER_UUID)
            socket.connect()
            val output = socket.outputStream

            output.write(ESC_INIT)
            output.write(ESC_ALIGN_CENTER)

            // Convert bitmap to ESC/POS raster format
            sendBitmap(output, bitmap)

            output.write(ESC_FEED)
            output.write(ESC_PARTIAL_CUT)
            output.flush()

            kotlin.Result.success(Unit)
        } catch (e: IOException) {
            kotlin.Result.failure(Exception("Lỗi kết nối máy in: ${e.message}"))
        } catch (e: Exception) {
            kotlin.Result.failure(Exception("Lỗi in hóa đơn: ${e.message}"))
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    private fun sendTextReceipt(output: OutputStream, store: Store, detail: OrderDetail) {
        val textReceipt = ReceiptGenerator.generateTextReceipt(store, detail)

        output.write(ESC_INIT)

        // Try to use Vietnamese encoding; fall back to UTF-8
        val bytes = try {
            textReceipt.toByteArray(charset("UTF-8"))
        } catch (e: Exception) {
            textReceipt.toByteArray()
        }

        output.write(bytes)
    }

    /**
     * Send bitmap data to printer in ESC/POS raster bit-image mode.
     */
    private fun sendBitmap(output: OutputStream, source: Bitmap) {
        // Convert to monochrome
        val width = source.width
        val height = source.height
        val bytesPerRow = (width + 7) / 8

        // Threshold for black/white conversion
        val threshold = 128

        for (y in 0 until height) {
            // GS v 0 command for raster bit image
            output.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))
            output.write(byteArrayOf(
                (bytesPerRow and 0xFF).toByte(),
                ((bytesPerRow shr 8) and 0xFF).toByte(),
                0x01,
                0x00,
            ))

            for (byteIndex in 0 until bytesPerRow) {
                var byte = 0
                for (bit in 0 until 8) {
                    val x = byteIndex * 8 + bit
                    if (x < width) {
                        val pixel = source.getPixel(x, y)
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF
                        val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                        if (luminance < threshold) {
                            byte = byte or (0x80 shr bit)
                        }
                    }
                }
                output.write(byte)
            }
        }
    }

    /**
     * Check if Bluetooth is available and enabled.
     */
    fun isBluetoothAvailable(context: Context): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter?.isEnabled == true
    }
}
