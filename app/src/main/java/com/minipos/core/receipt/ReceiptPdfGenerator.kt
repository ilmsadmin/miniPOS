package com.minipos.core.receipt

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Generates PDF receipts from HTML content using WebView rendering.
 */
object ReceiptPdfGenerator {

    /**
     * Generate a PDF file from HTML receipt content.
     * Must be called from a coroutine context.
     * Returns the File path of the generated PDF.
     */
    suspend fun generatePdf(
        context: Context,
        html: String,
        fileName: String,
    ): File = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<File>()

        val webView = WebView(context).apply {
            settings.javaScriptEnabled = false
            settings.allowFileAccess = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                // Create print adapter and generate PDF
                createPdfFromWebView(context, webView, fileName, deferred)
            }
        }

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)

        deferred.await()
    }

    private fun createPdfFromWebView(
        context: Context,
        webView: WebView,
        fileName: String,
        deferred: CompletableDeferred<File>,
    ) {
        try {
            // For simpler approach, create PDF using PdfDocument from measured WebView
            val width = (80 * 72 / 25.4).toInt() // 80mm in points (72 dpi)
            val contentHeight = webView.contentHeight
            val scale = width.toFloat() / webView.width.coerceAtLeast(1)
            val pdfHeight = ((contentHeight * scale) + 40).toInt().coerceAtLeast(200)

            webView.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(pdfHeight, android.view.View.MeasureSpec.EXACTLY),
            )
            webView.layout(0, 0, width, pdfHeight)

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(width, pdfHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            webView.draw(page.canvas)
            pdfDocument.finishPage(page)

            val outputDir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val outputFile = File(outputDir, "$fileName.pdf")

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()

            deferred.complete(outputFile)
        } catch (e: Exception) {
            deferred.completeExceptionally(e)
        }
    }

    /**
     * Generate PDF using Android Print Framework for high-quality output.
     * This uses PrintedPdfDocument for better rendering.
     */
    suspend fun generatePdfSimple(
        context: Context,
        html: String,
        fileName: String,
    ): File = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<File>()

        val webView = WebView(context).apply {
            settings.javaScriptEnabled = false
            settings.allowFileAccess = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.postDelayed({
                    try {
                        // Measure and layout the WebView
                        val widthPx = 595 // A4 width in 72dpi points ~= 210mm
                        @Suppress("DEPRECATION")
                        val measuredHeight = (webView.contentHeight * webView.scale).toInt()
                            .coerceAtLeast(400)

                        webView.measure(
                            android.view.View.MeasureSpec.makeMeasureSpec(widthPx, android.view.View.MeasureSpec.EXACTLY),
                            android.view.View.MeasureSpec.makeMeasureSpec(measuredHeight, android.view.View.MeasureSpec.UNSPECIFIED),
                        )
                        webView.layout(0, 0, widthPx, measuredHeight)

                        val document = PdfDocument()
                        val pageInfo = PdfDocument.PageInfo.Builder(widthPx, measuredHeight, 1).create()
                        val page = document.startPage(pageInfo)
                        webView.draw(page.canvas)
                        document.finishPage(page)

                        val outputDir = File(context.cacheDir, "receipts").apply { mkdirs() }
                        val outputFile = File(outputDir, "$fileName.pdf")
                        FileOutputStream(outputFile).use { document.writeTo(it) }
                        document.close()

                        deferred.complete(outputFile)
                    } catch (e: Exception) {
                        deferred.completeExceptionally(e)
                    }
                }, 500) // Small delay to ensure rendering is complete
            }
        }

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        deferred.await()
    }

    /**
     * Clean up old cached receipt files.
     */
    fun cleanupCache(context: Context) {
        val dir = File(context.cacheDir, "receipts")
        if (dir.exists()) {
            val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
            dir.listFiles()?.forEach { file ->
                if (file.lastModified() < oneDayAgo) {
                    file.delete()
                }
            }
        }
    }
}
