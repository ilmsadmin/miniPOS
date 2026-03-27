package com.minipos.core.receipt

import android.content.Context
import android.graphics.pdf.PdfDocument
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
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                // Use a delay to ensure content is fully rendered
                view?.postDelayed({
                    renderWebViewToPdf(context, webView, fileName, deferred)
                }, 300)
            }
        }

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        deferred.await()
    }

    /**
     * Generate PDF with better rendering quality.
     * Uses postDelayed with longer delay for complex receipts.
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
            // Set initial layout size so WebView can calculate content properly
            val displayMetrics = context.resources.displayMetrics
            val widthPx = (595 * displayMetrics.density).toInt()
            measure(
                android.view.View.MeasureSpec.makeMeasureSpec(widthPx, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
            )
            layout(0, 0, widthPx, 1)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.postDelayed({
                    renderWebViewToPdf(context, webView, fileName, deferred)
                }, 600) // Slightly longer delay for reliable rendering
            }
        }

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        deferred.await()
    }

    private fun renderWebViewToPdf(
        context: Context,
        webView: WebView,
        fileName: String,
        deferred: CompletableDeferred<File>,
    ) {
        try {
            val pdfWidthPx = 595 // A4 width in points at 72dpi

            // Measure with exact width, unspecified height to get content height
            webView.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(pdfWidthPx, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
            )

            // Use the larger of measuredHeight and contentHeight * density
            val density = context.resources.displayMetrics.density
            val contentBasedHeight = (webView.contentHeight * density).toInt()
            val measuredHeight = webView.measuredHeight
            val pdfHeightPx = maxOf(contentBasedHeight, measuredHeight, 400)

            webView.layout(0, 0, pdfWidthPx, pdfHeightPx)

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pdfWidthPx, pdfHeightPx, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            // Scale canvas if needed
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
