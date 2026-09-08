package com.shohan.khatago.data.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.domain.model.DateRange
import com.shohan.khatago.domain.model.LedgerEntry
import com.shohan.khatago.domain.model.ReportData
import java.time.LocalDate

/**
 * Local PDF report generation with android.graphics.pdf.PdfDocument.
 *
 * No cloud service, no paid SDK, no network: the document is rendered on the
 * device from the same data the Reports screen shows.
 */
object PdfReportRenderer {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 44f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (2 * MARGIN)

    private const val GREEN = 0xFF14614B.toInt()
    private const val INK = 0xFF12211C.toInt()
    private const val MUTED = 0xFF6B7A73.toInt()
    private const val LINE = 0xFFE2E8E5.toInt()
    private const val PANEL = 0xFFF4F7F5.toInt()
    private const val POSITIVE = 0xFF1F7A55.toInt()
    private const val NEGATIVE = 0xFFC4453B.toInt()

    fun render(report: ReportData, generatedAt: Long = System.currentTimeMillis()): PdfDocument {
        val document = PdfDocument()
        val state = PageState(document)
        val today = LocalDate.now()

        drawHeader(state, report.range, generatedAt)

        // ------------------------------------------------------------- summary
        state.section("Summary")
        summaryCard(state, report)

        // ----------------------------------------------------------- accounts
        state.section("Outstanding by account")
        if (report.debtDistribution.isEmpty()) {
            state.body("Nothing outstanding right now.")
        } else {
            val total = report.debtDistribution.sumOf { it.outstanding }.coerceAtLeast(1L)
            report.debtDistribution.forEach { slice ->
                state.bulletWithBar(
                    label = slice.kind.label,
                    value = "BDT ${Money.formatPlain(slice.outstanding)}",
                    progress = slice.outstanding.toFloat() / total.toFloat()
                )
            }
            state.keyValue("Total outstanding", "BDT ${Money.formatPlain(report.outstanding.total)}")
        }

        if (report.receivable.outstanding > 0L) {
            state.gap(6f)
            state.keyValue("Money lent out (still due)", "BDT ${Money.formatPlain(report.receivable.outstanding)}")
        }

        // ---------------------------------------------------------- categories
        state.section("Where the money went")
        if (report.expenseByCategory.isEmpty()) {
            state.body("No expenses recorded in this period.")
        } else {
            val total = report.expenseByCategory.sumOf { it.total }.coerceAtLeast(1L)
            report.expenseByCategory.take(8).forEach { slice ->
                state.bulletWithBar(
                    label = slice.name,
                    value = "BDT ${Money.formatPlain(slice.total)}",
                    progress = slice.total.toFloat() / total.toFloat()
                )
            }
        }

        state.section("Income sources")
        if (report.incomeByCategory.isEmpty()) {
            state.body("No income recorded in this period.")
        } else {
            val total = report.incomeByCategory.sumOf { it.total }.coerceAtLeast(1L)
            report.incomeByCategory.take(8).forEach { slice ->
                state.bulletWithBar(
                    label = slice.name,
                    value = "BDT ${Money.formatPlain(slice.total)}",
                    progress = slice.total.toFloat() / total.toFloat()
                )
            }
        }

        // ------------------------------------------------------- monthly trend
        if (report.monthly.size > 1) {
            state.section("Monthly cash flow")
            state.tableHeader(listOf("Month", "Income", "Expense", "Net"))
            report.monthly.forEach { point ->
                state.tableRow(
                    listOf(
                        point.label,
                        Money.formatPlain(point.income),
                        Money.formatPlain(point.expense),
                        Money.formatPlain(point.net, showDecimals = false)
                    ),
                    emphasizeLast = true
                )
            }
        }

        // -------------------------------------------------------- transactions
        state.section("Transactions")
        if (report.transactions.isEmpty()) {
            state.body("No transactions in this period.")
        } else {
            state.tableHeader(listOf("Date", "Description", "Type", "Amount (BDT)"))
            report.transactions.take(40).forEach { entry ->
                state.tableRow(
                    listOf(
                        KhataGoTime.formatShortDate(entry.date),
                        entry.description.ifBlank { entry.category }.ifBlank { entry.type.label }
                            .let { if (it.length > 34) it.take(33) + "…" else it },
                        entry.type.label,
                        signedAmount(entry)
                    )
                )
            }
            if (report.transactions.size > 40) {
                state.gap(4f)
                state.body("Showing the 40 most recent of ${report.transactions.size} transactions.")
            }
        }

        state.finish(today)
        return document
    }

    private fun signedAmount(entry: LedgerEntry): String = when {
        entry.type.countsAsIncome -> "+${Money.formatPlain(entry.amount)}"
        entry.type.isCashOut -> "−${Money.formatPlain(entry.amount)}"
        else -> Money.formatPlain(entry.amount)
    }

    // ------------------------------------------------------------- page helper

    private class PageState(private val document: PdfDocument) {
        var page: PdfDocument.Page = newPage()
        var canvas: Canvas = page.canvas
        var y = MARGIN
        var pageNumber = 1

        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f
            color = GREEN
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            color = MUTED
        }
        private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            color = GREEN
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            color = INK
        }
        private val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            color = MUTED
        }
        private val strongPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            color = INK
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LINE
            strokeWidth = 1f
        }
        private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PANEL }

        fun newPage(): PdfDocument.Page {
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val created = document.startPage(info)
            canvas = created.canvas
            y = MARGIN
            return created
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > PAGE_HEIGHT - MARGIN - 24f) {
                document.finishPage(page)
                pageNumber += 1
                page = newPage()
            }
        }

        fun gap(amount: Float) {
            y += amount
        }

        fun section(title: String) {
            ensureSpace(46f)
            y += 12f
            canvas.drawText(title.uppercase(), MARGIN, y, sectionPaint)
            y += 8f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 14f
        }

        fun body(text: String) {
            ensureSpace(20f)
            canvas.drawText(text, MARGIN, y, bodyPaint)
            y += 16f
        }

        fun keyValue(label: String, value: String, emphasize: Boolean = false) {
            ensureSpace(20f)
            canvas.drawText(label, MARGIN, y, if (emphasize) strongPaint else bodyPaint)
            val width = (if (emphasize) strongPaint else bodyPaint).measureText(value)
            canvas.drawText(value, PAGE_WIDTH - MARGIN - width, y, if (emphasize) strongPaint else bodyPaint)
            y += 16f
        }

        fun bulletWithBar(label: String, value: String, progress: Float) {
            ensureSpace(26f)
            canvas.drawText(label, MARGIN, y, bodyPaint)
            val width = strongPaint.measureText(value)
            canvas.drawText(value, PAGE_WIDTH - MARGIN - width, y, strongPaint)
            y += 6f
            val barTop = y
            canvas.drawRoundRect(
                MARGIN, barTop, PAGE_WIDTH - MARGIN, barTop + 5f, 2.5f, 2.5f, panelPaint
            )
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN }
            val filled = (CONTENT_WIDTH * progress.coerceIn(0f, 1f))
            if (filled > 0f) {
                canvas.drawRoundRect(
                    MARGIN, barTop, MARGIN + filled, barTop + 5f, 2.5f, 2.5f, fillPaint
                )
            }
            y += 20f
        }

        fun tableHeader(columns: List<String>) {
            ensureSpace(24f)
            val widths = columnWidths(columns.size)
            columns.forEachIndexed { index, column ->
                canvas.drawText(column, MARGIN + widths.first * index, y, mutedPaint)
            }
            y += 5f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 14f
        }

        fun tableRow(columns: List<String>, emphasizeLast: Boolean = false) {
            ensureSpace(20f)
            val widths = columnWidths(columns.size)
            columns.forEachIndexed { index, column ->
                val isLast = index == columns.size - 1
                val paint = when {
                    isLast && emphasizeLast -> Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 10f
                        color = if (column.startsWith("−")) NEGATIVE else if (column.startsWith("+")) POSITIVE else INK
                        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                    }
                    isLast -> Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 10f
                        color = if (column.startsWith("−")) NEGATIVE else INK
                    }
                    else -> bodyPaint
                }
                var text = column
                if (isLast) {
                    val textWidth = paint.measureText(text)
                    canvas.drawText(text, PAGE_WIDTH - MARGIN - textWidth, y, paint)
                    return@forEachIndexed
                }
                val available = widths.first - 12f
                while (paint.measureText(text) > available && text.length > 4) {
                    text = text.dropLast(2) + "…"
                }
                canvas.drawText(text, MARGIN + widths.first * index, y, paint)
            }
            y += 16f
        }

        private fun columnWidths(count: Int): Pair<Float, Float> {
            val width = CONTENT_WIDTH / count.toFloat()
            return width to width
        }

        fun panel(height: Float, draw: (Canvas, Float) -> Unit) {
            ensureSpace(height + 8f)
            draw(canvas, y)
            y += height + 8f
        }

        fun finish(today: LocalDate) {
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 8f
                color = MUTED
            }
            val footer = "KhataGo · Created by Shohan Khan · helloiamshohan@gmail.com · " +
                "Generated ${KhataGoTime.formatDate(today)}"
            val pageInfo = "Page $pageNumber"
            val pages = 1..pageNumber
            pages.forEach { index ->
                // The footer is drawn on the active page; earlier pages were closed
                // with their footer already rendered in drawHeader/finish flow.
                if (index == pageNumber) {
                    canvas.drawText(footer, MARGIN, PAGE_HEIGHT - MARGIN + 14f, footerPaint)
                    val width = footerPaint.measureText(pageInfo)
                    canvas.drawText(pageInfo, PAGE_WIDTH - MARGIN - width, PAGE_HEIGHT - MARGIN + 14f, footerPaint)
                }
            }
            document.finishPage(page)
        }
    }

    private fun drawHeader(state: PageState, range: DateRange, generatedAt: Long) {
        val canvas = state.canvas
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 26f
            color = GREEN
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            color = MUTED
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            color = MUTED
        }

        canvas.drawText("KhataGo", MARGIN, state.y + 14f, titlePaint)
        val periodLabel = "Report period: ${KhataGoTime.formatDate(range.start)} – ${KhataGoTime.formatDate(range.end)}"
        val periodWidth = metaPaint.measureText(periodLabel)
        canvas.drawText(periodLabel, PAGE_WIDTH - MARGIN - periodWidth, state.y + 6f, metaPaint)
        val generatedLabel = "Generated ${KhataGoTime.formatDateTime(generatedAt)}"
        val generatedWidth = metaPaint.measureText(generatedLabel)
        canvas.drawText(generatedLabel, PAGE_WIDTH - MARGIN - generatedWidth, state.y + 20f, metaPaint)

        state.y += 26f
        canvas.drawText("All your finances, in one place.", MARGIN, state.y, taglinePaint)
        state.y += 10f
        canvas.drawText("Created by Shohan Khan · helloiamshohan@gmail.com", MARGIN, state.y, taglinePaint)
        state.y += 12f
        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GREEN
            strokeWidth = 2f
        }
        canvas.drawLine(MARGIN, state.y, PAGE_WIDTH - MARGIN, state.y, divider)
        state.y += 16f
    }

    private fun summaryCard(state: PageState, report: ReportData) {
        state.panel(96f) { canvas, top ->
            val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PANEL }
            canvas.drawRoundRect(MARGIN, top, PAGE_WIDTH - MARGIN, top + 88f, 10f, 10f, panelPaint)

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; color = MUTED }
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 15f
                color = INK
                typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            }
            val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 15f
                color = POSITIVE
                typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            }
            val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 15f
                color = NEGATIVE
                typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            }

            val columnWidth = CONTENT_WIDTH / 2f
            val rows = listOf(
                Triple("Income", "BDT ${Money.formatPlain(report.income)}", greenPaint),
                Triple("Expense", "BDT ${Money.formatPlain(report.expense)}", redPaint),
                Triple("Net cash flow", "BDT ${Money.formatPlain(report.netCashFlow)}", valuePaint),
                Triple("Payments made", "BDT ${Money.formatPlain(report.payments)}", valuePaint)
            )
            rows.forEachIndexed { index, (label, value, paint) ->
                val column = index % 2
                val row = index / 2
                val x = MARGIN + 16f + columnWidth * column
                val yLabel = top + 26f + row * 42f
                val yValue = top + 44f + row * 42f
                canvas.drawText(label, x, yLabel, labelPaint)
                canvas.drawText(value, x, yValue, paint)
            }
        }
    }
}
