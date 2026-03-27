package com.minipos.core.receipt

import android.content.Context
import com.minipos.R
import com.minipos.core.utils.CurrencyFormatter
import com.minipos.core.utils.DateUtils
import com.minipos.domain.model.OrderDetail
import com.minipos.domain.model.Store

/**
 * Generates receipt content in various formats (plain text, HTML).
 */
object ReceiptGenerator {

    private const val LINE_WIDTH = 32 // Characters for thermal printer (58mm)
    private const val LINE_SEPARATOR = "--------------------------------"
    private const val DOUBLE_LINE = "================================"

    /**
     * Generate plain text receipt for thermal printing or sharing as text.
     */
    fun generateTextReceipt(context: Context, store: Store, detail: OrderDetail): String {
        val order = detail.order
        val items = detail.items
        val payments = detail.payments

        return buildString {
            // Header
            appendLine(center(store.name.uppercase()))
            if (!store.address.isNullOrBlank()) {
                appendLine(center(store.address))
            }
            if (!store.phone.isNullOrBlank()) {
                appendLine(center(context.getString(R.string.receipt_phone_prefix, store.phone)))
            }
            // Custom receipt header (below store info)
            if (store.settings.receiptHeader.isNotBlank()) {
                appendLine(center(store.settings.receiptHeader))
            }
            appendLine(DOUBLE_LINE)
            appendLine(center(context.getString(R.string.receipt_title)))
            appendLine(DOUBLE_LINE)

            // Order info
            appendLine(context.getString(R.string.receipt_code, order.orderCode))
            appendLine(context.getString(R.string.receipt_date, DateUtils.formatDateTime(order.createdAt)))
            if (!order.customerName.isNullOrBlank()) {
                appendLine(context.getString(R.string.receipt_customer_prefix, order.customerName))
            }
            appendLine(LINE_SEPARATOR)

            // Items
            for (item in items) {
                val qty = if (item.quantity == item.quantity.toLong().toDouble()) {
                    item.quantity.toLong().toString()
                } else {
                    item.quantity.toString()
                }
                appendLine(item.productName)
                if (!item.variantName.isNullOrBlank()) {
                    appendLine("  (${item.variantName})")
                }
                val priceStr = "${CurrencyFormatter.format(item.unitPrice)} x $qty"
                val totalStr = CurrencyFormatter.format(item.totalPrice)
                appendLine(leftRight("  $priceStr", totalStr))
                if (item.discountAmount > 0) {
                    appendLine(leftRight("  ${context.getString(R.string.receipt_discount_label)}", "-${CurrencyFormatter.format(item.discountAmount)}"))
                }
            }
            appendLine(LINE_SEPARATOR)

            // Totals
            appendLine(leftRight(context.getString(R.string.receipt_subtotal), CurrencyFormatter.format(order.subtotal)))
            if (order.discountAmount > 0) {
                val discountLabel = when (order.discountType) {
                    "percent" -> context.getString(R.string.receipt_discount_percent_label, order.discountValue.toLong().toInt())
                    else -> context.getString(R.string.receipt_discount_label)
                }
                appendLine(leftRight(discountLabel, "-${CurrencyFormatter.format(order.discountAmount)}"))
            }
            if (order.taxAmount > 0) {
                appendLine(leftRight(context.getString(R.string.receipt_tax), CurrencyFormatter.format(order.taxAmount)))
            }
            appendLine(DOUBLE_LINE)
            appendLine(leftRight(context.getString(R.string.receipt_grand_total), CurrencyFormatter.format(order.totalAmount)))
            appendLine(DOUBLE_LINE)

            // Payments
            if (payments.isNotEmpty()) {
                for (payment in payments) {
                    appendLine(leftRight(payment.method.displayName(context) + ":", CurrencyFormatter.format(payment.amount)))
                    if ((payment.receivedAmount ?: 0.0) > payment.amount) {
                        appendLine(leftRight("  ${context.getString(R.string.receipt_received)}", CurrencyFormatter.format(payment.receivedAmount ?: 0.0)))
                    }
                    if (payment.changeAmount > 0) {
                        appendLine(leftRight("  ${context.getString(R.string.receipt_change)}", CurrencyFormatter.format(payment.changeAmount)))
                    }
                }
                appendLine(LINE_SEPARATOR)
            }

            // Notes
            if (!order.notes.isNullOrBlank()) {
                appendLine(context.getString(R.string.receipt_notes, order.notes))
                appendLine(LINE_SEPARATOR)
            }

            // Footer
            if (store.settings.receiptFooter.isNotBlank()) {
                appendLine(center(store.settings.receiptFooter))
            }
            appendLine()
        }
    }

    /**
     * Generate HTML receipt for PDF generation with proper styling.
     */
    fun generateHtmlReceipt(context: Context, store: Store, detail: OrderDetail): String {
        val order = detail.order
        val items = detail.items
        val payments = detail.payments

        return buildString {
            append("""
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  @page { margin: 8mm; size: 80mm auto; }
  body { font-family: 'Roboto', sans-serif; font-size: 12px; color: #333; margin: 0; padding: 0; width: 100%; }
  .receipt { max-width: 72mm; margin: 0 auto; padding: 4mm; }
  .center { text-align: center; }
  .bold { font-weight: bold; }
  .store-name { font-size: 16px; font-weight: bold; text-transform: uppercase; margin-bottom: 2px; }
  .store-info { font-size: 11px; color: #666; }
  .title { font-size: 14px; font-weight: bold; margin: 8px 0; text-transform: uppercase; letter-spacing: 1px; }
  .divider { border: none; border-top: 1px dashed #999; margin: 6px 0; }
  .double-divider { border: none; border-top: 2px solid #333; margin: 6px 0; }
  .order-info { font-size: 11px; margin: 4px 0; }
  .items-table { width: 100%; border-collapse: collapse; margin: 4px 0; }
  .items-table td { padding: 2px 0; vertical-align: top; font-size: 12px; }
  .items-table .item-name { font-weight: 500; }
  .items-table .item-detail { font-size: 11px; color: #666; padding-left: 8px; }
  .items-table .item-total { text-align: right; font-weight: 500; white-space: nowrap; }
  .items-table .item-discount { font-size: 10px; color: #e53935; padding-left: 8px; }
  .summary-table { width: 100%; margin: 4px 0; }
  .summary-table td { padding: 2px 0; font-size: 12px; }
  .summary-table .label { }
  .summary-table .value { text-align: right; font-weight: 500; }
  .grand-total { font-size: 16px; font-weight: bold; }
  .payment-info { font-size: 11px; }
  .footer { font-size: 11px; color: #666; margin-top: 8px; }
  .qr-placeholder { margin: 8px 0; }
</style>
</head>
<body>
<div class="receipt">
""")

            // Store header
            append("""<div class="center"><div class="store-name">${escapeHtml(store.name)}</div>""")
            if (!store.address.isNullOrBlank()) {
                append("""<div class="store-info">${escapeHtml(store.address)}</div>""")
            }
            if (!store.phone.isNullOrBlank()) {
                append("""<div class="store-info">${escapeHtml(context.getString(R.string.receipt_phone_prefix, store.phone))}</div>""")
            }
            // Custom receipt header
            if (store.settings.receiptHeader.isNotBlank()) {
                append("""<div class="store-info" style="margin-top:4px;font-style:italic">${escapeHtml(store.settings.receiptHeader)}</div>""")
            }
            append("</div>")

            // Title
            append("""<hr class="double-divider"><div class="center title">${escapeHtml(context.getString(R.string.receipt_html_title))}</div><hr class="double-divider">""")

            // Order info
            append("""<div class="order-info"><strong>${escapeHtml(context.getString(R.string.receipt_code, ""))}</strong> ${escapeHtml(order.orderCode)}</div>""")
            append("""<div class="order-info"><strong>${escapeHtml(context.getString(R.string.receipt_date, ""))}</strong> ${DateUtils.formatDateTime(order.createdAt)}</div>""")
            if (!order.customerName.isNullOrBlank()) {
                append("""<div class="order-info"><strong>${escapeHtml(context.getString(R.string.receipt_customer_prefix, ""))}</strong> ${escapeHtml(order.customerName)}</div>""")
            }

            // Items
            append("""<hr class="divider"><table class="items-table">""")
            for (item in items) {
                val qty = if (item.quantity == item.quantity.toLong().toDouble()) {
                    item.quantity.toLong().toString()
                } else {
                    item.quantity.toString()
                }
                append("<tr>")
                append("""<td colspan="2" class="item-name">${escapeHtml(item.productName)}""")
                if (!item.variantName.isNullOrBlank()) {
                    append(""" <span style="font-size:10px;color:#666">(${escapeHtml(item.variantName)})</span>""")
                }
                append("</td></tr>")
                append("""<tr><td class="item-detail">${CurrencyFormatter.format(item.unitPrice)} x $qty</td>""")
                append("""<td class="item-total">${CurrencyFormatter.format(item.totalPrice)}</td></tr>""")
                if (item.discountAmount > 0) {
                    append("""<tr><td class="item-discount">${context.getString(R.string.receipt_discount_label)} -${CurrencyFormatter.format(item.discountAmount)}</td><td></td></tr>""")
                }
            }
            append("</table>")

            // Summary
            append("""<hr class="divider"><table class="summary-table">""")
            append("""<tr><td class="label">${context.getString(R.string.receipt_subtotal)}</td><td class="value">${CurrencyFormatter.format(order.subtotal)}</td></tr>""")
            if (order.discountAmount > 0) {
                val discountLabel = when (order.discountType) {
                    "percent" -> context.getString(R.string.receipt_html_discount_percent_label, order.discountValue.toLong().toInt())
                    else -> context.getString(R.string.receipt_html_discount_label)
                }
                append("""<tr><td class="label">$discountLabel</td><td class="value" style="color:#e53935">-${CurrencyFormatter.format(order.discountAmount)}</td></tr>""")
            }
            if (order.taxAmount > 0) {
                append("""<tr><td class="label">${context.getString(R.string.receipt_tax)}</td><td class="value">${CurrencyFormatter.format(order.taxAmount)}</td></tr>""")
            }
            append("</table>")

            // Grand total
            append("""<hr class="double-divider"><table class="summary-table">""")
            append("""<tr><td class="label grand-total">${context.getString(R.string.receipt_grand_total)}</td><td class="value grand-total">${CurrencyFormatter.format(order.totalAmount)}</td></tr>""")
            append("""</table><hr class="double-divider">""")

            // Payments
            if (payments.isNotEmpty()) {
                append("""<div class="payment-info">""")
                for (payment in payments) {
                    append("""<div><strong>${payment.method.displayName(context)}:</strong> ${CurrencyFormatter.format(payment.amount)}</div>""")
                    if ((payment.receivedAmount ?: 0.0) > payment.amount) {
                        append("""<div style="padding-left:8px">${context.getString(R.string.receipt_received)} ${CurrencyFormatter.format(payment.receivedAmount ?: 0.0)}</div>""")
                    }
                    if (payment.changeAmount > 0) {
                        append("""<div style="padding-left:8px">${context.getString(R.string.receipt_change)} ${CurrencyFormatter.format(payment.changeAmount)}</div>""")
                    }
                }
                append("""</div><hr class="divider">""")
            }

            // Notes
            if (!order.notes.isNullOrBlank()) {
                append("""<div class="order-info"><strong>${context.getString(R.string.receipt_notes, "")}</strong> ${escapeHtml(order.notes)}</div><hr class="divider">""")
            }

            // Footer
            if (store.settings.receiptFooter.isNotBlank()) {
                append("""<div class="center footer">""")
                append("<div>${escapeHtml(store.settings.receiptFooter)}</div>")
                append("</div>")
            }

            append("</div></body></html>")
        }
    }

    // ---- Text formatting helpers ----

    private fun center(text: String): String {
        if (text.length >= LINE_WIDTH) return text
        val padding = (LINE_WIDTH - text.length) / 2
        return " ".repeat(padding) + text
    }

    private fun leftRight(left: String, right: String): String {
        val space = LINE_WIDTH - left.length - right.length
        return if (space > 0) {
            left + " ".repeat(space) + right
        } else {
            "$left $right"
        }
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
