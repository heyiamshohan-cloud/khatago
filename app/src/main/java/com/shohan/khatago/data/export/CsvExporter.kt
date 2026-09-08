package com.shohan.khatago.data.export

import com.shohan.khatago.core.money.Money
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.domain.model.LedgerEntry
import com.shohan.khatago.domain.model.TransactionType

/**
 * CSV export of real transaction data.
 *
 * Output is UTF-8 with a byte order mark so Excel opens ৳ amounts correctly, and
 * every field is escaped for commas, quotes and newlines.
 */
object CsvExporter {

    // Built from an escape so this source file holds no literal byte order mark.
    private const val BOM = "\uFEFF"

    private val HEADERS = listOf(
        "Date",
        "Time",
        "Type",
        "Category",
        "Description",
        "Amount (BDT)",
        "Amount",
        "Related Account",
        "Status",
        "Notes"
    )

    fun build(entries: List<LedgerEntry>): String {
        val builder = StringBuilder()
        builder.append(BOM)
        builder.appendLine(HEADERS.joinToString(",") { escape(it) })

        entries.forEach { entry ->
            builder.appendLine(
                listOf(
                    KhataGoTime.formatDate(entry.date),
                    KhataGoTime.formatTime(entry.timestamp),
                    entry.type.label,
                    entry.category,
                    entry.description,
                    Money.toDecimal(entry.amount).toPlainString(),
                    Money.format(entry.amount),
                    entry.relatedType.name,
                    statusFor(entry.type),
                    entry.notes
                ).joinToString(",") { escape(it) }
            )
        }
        return builder.toString()
    }

    private fun statusFor(type: TransactionType): String = when (type) {
        TransactionType.INCOME -> "Income"
        TransactionType.EXPENSE -> "Expense"
        TransactionType.SHOP_CREDIT,
        TransactionType.LOAN,
        TransactionType.EMI_PURCHASE,
        TransactionType.PERSONAL_BORROWING -> "Obligation"
        TransactionType.PERSONAL_LENDING -> "Lent"
        else -> "Payment"
    }

    /** RFC 4180 escaping: quotes doubled, the field wrapped in quotes when needed. */
    internal fun escape(value: String): String {
        val needsEscaping = value.contains(',') || value.contains('"') ||
            value.contains('\n') || value.contains('\r')
        val escaped = value.replace("\"", "\"\"")
        return if (needsEscaping) "\"$escaped\"" else escaped
    }
}
