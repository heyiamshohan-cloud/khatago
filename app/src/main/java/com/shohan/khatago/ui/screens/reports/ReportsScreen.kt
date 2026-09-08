package com.shohan.khatago.ui.screens.reports

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.data.repository.ReportRangeKind
import com.shohan.khatago.data.repository.ReportsRepository
import com.shohan.khatago.domain.model.DateRange
import com.shohan.khatago.domain.model.LedgerEntry
import com.shohan.khatago.domain.model.ReportData
import com.shohan.khatago.ui.components.KhataGoBarBreakdown
import com.shohan.khatago.ui.components.KhataGoBarChart
import com.shohan.khatago.ui.components.KhataGoCard
import com.shohan.khatago.ui.components.KhataGoChartLegend
import com.shohan.khatago.ui.components.KhataGoChip
import com.shohan.khatago.ui.components.KhataGoDonutChart
import com.shohan.khatago.ui.components.KhataGoEmptyState
import com.shohan.khatago.ui.components.KhataGoKeyValueRow
import com.shohan.khatago.ui.components.KhataGoSecondaryButton
import com.shohan.khatago.ui.components.KhataGoTopBar
import com.shohan.khatago.ui.components.KhataGoTransactionRow
import com.shohan.khatago.ui.components.DonutSlice
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkPrimary
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.Negative
import com.shohan.khatago.ui.theme.Positive
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceWhite
import com.shohan.khatago.ui.theme.Upcoming
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ReportsUiState(
    val kind: ReportRangeKind = ReportRangeKind.THIS_MONTH,
    val range: DateRange = DateRange(
        KhataGoTime.startOfMonth(KhataGoTime.today()),
        KhataGoTime.endOfMonth(KhataGoTime.today()),
        ""
    ),
    val report: ReportData = ReportData(range = DateRange(KhataGoTime.today(), KhataGoTime.today(), ""))
)

class ReportsViewModel(private val repository: ReportsRepository) : ViewModel() {

    private val kind = MutableStateFlow(ReportRangeKind.THIS_MONTH)
    private val customRange = MutableStateFlow<DateRange?>(null)

    val state: StateFlow<ReportsUiState> = kind.flatMapLatest { current ->
        val range = customRange.value?.takeIf { current == ReportRangeKind.CUSTOM }
            ?: repository.rangeFor(current)
        repository.observeReport(range).mapToState(current, range)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ReportsUiState()
    )

    fun selectRange(value: ReportRangeKind) {
        kind.value = value
    }

    fun setCustomRange(start: java.time.LocalDate, end: java.time.LocalDate) {
        customRange.value = DateRange(start, end, "Custom")
        kind.value = ReportRangeKind.CUSTOM
    }

    private fun kotlinx.coroutines.flow.Flow<ReportData>.mapToState(
        current: ReportRangeKind,
        range: DateRange
    ): kotlinx.coroutines.flow.Flow<ReportsUiState> =
        map { report -> ReportsUiState(current, range, report) }
}

/**
 * Reports: period filters, reconciled summary, analytics and the underlying
 * transaction list. Every figure matches the dashboard's accounting semantics.
 */
@Composable
fun ReportsScreen(
    uiState: ReportsUiState,
    onSelectRange: (ReportRangeKind) -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onOpenEntry: (LedgerEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val report = uiState.report

    Scaffold(containerColor = CanvasWhite) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.XXXL),
            verticalArrangement = Arrangement.spacedBy(Spacing.CardGap)
        ) {
            item {
                KhataGoTopBar(
                    title = "Reports",
                    subtitle = "${KhataGoTime.formatDate(uiState.range.start)} – ${KhataGoTime.formatDate(uiState.range.end)}",
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.Gutter),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.S)
                ) {
                    ReportRangeKind.entries.forEach { option ->
                        KhataGoChip(
                            text = option.label,
                            selected = uiState.kind == option,
                            onClick = { onSelectRange(option) }
                        )
                    }
                }
            }

            item {
                SummarySection(report, modifier = Modifier.padding(horizontal = Spacing.Gutter))
            }

            item {
                OutstandingSection(report, modifier = Modifier.padding(horizontal = Spacing.Gutter))
            }

            if (report.monthly.size > 1) {
                item {
                    Column(modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                        Text(
                            text = "Cash Flow",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )
                        Spacer(Modifier.height(Spacing.XS))
                        KhataGoCard(containerColor = SurfaceWhite) {
                            Column(modifier = Modifier.padding(Spacing.CardInner)) {
                                KhataGoChartLegend(
                                    items = listOf(
                                        "Income" to Positive,
                                        "Expense" to Negative
                                    )
                                )
                                Spacer(Modifier.height(Spacing.M))
                                KhataGoBarChart(data = report.monthly)
                            }
                        }
                    }
                }
            }

            if (report.expenseByCategory.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                        Text(
                            text = "Where the money went",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )
                        Spacer(Modifier.height(Spacing.XS))
                        KhataGoCard(containerColor = SurfaceWhite) {
                            Column(modifier = Modifier.padding(Spacing.CardInner)) {
                                KhataGoBarBreakdown(
                                    slices = report.expenseByCategory.mapIndexed { index, slice ->
                                        DonutSlice(
                                            label = slice.name,
                                            value = slice.total,
                                            color = categoryColor(index)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    Text(
                        text = "Export",
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary
                    )
                    Spacer(Modifier.height(Spacing.XS))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.CardGap)) {
                        KhataGoSecondaryButton(
                            text = "CSV",
                            onClick = onExportCsv,
                            icon = Icons.Outlined.TableChart,
                            modifier = Modifier.weight(1f)
                        )
                        KhataGoSecondaryButton(
                            text = "PDF",
                            onClick = onExportPdf,
                            icon = Icons.Outlined.PictureAsPdf,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary,
                        modifier = Modifier.padding(bottom = Spacing.XS)
                    )
                    if (report.transactions.isEmpty()) {
                        KhataGoCard(containerColor = SurfaceWhite) {
                            KhataGoEmptyState(
                                title = "No data for this period",
                                message = "Try another date range or add a transaction."
                            )
                        }
                    } else {
                        KhataGoCard(containerColor = SurfaceWhite) {
                            Column {
                                report.transactions.take(20).forEachIndexed { index, entry ->
                                    KhataGoTransactionRow(entry = entry, onClick = { onOpenEntry(entry) })
                                    if (index < minOf(report.transactions.size, 20) - 1) {
                                        com.shohan.khatago.ui.components.KhataGoDivider(
                                            modifier = Modifier.padding(horizontal = Spacing.L)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummarySection(report: ReportData, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Summary",
            style = MaterialTheme.typography.titleLarge,
            color = InkPrimary
        )
        Spacer(Modifier.height(Spacing.XS))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.CardGap)) {
            MetricCard(
                label = "Income",
                amount = report.income,
                color = Positive,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Expense",
                amount = report.expense,
                color = Negative,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(Spacing.CardGap))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.CardGap)) {
            MetricCard(
                label = "Net Cash Flow",
                amount = report.netCashFlow,
                color = if (report.netCashFlow < 0L) Negative else Positive,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Payments",
                amount = report.payments,
                color = KhataGoGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    amount: Long,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    KhataGoCard(modifier = modifier, containerColor = SurfaceWhite) {
        Column(modifier = Modifier.padding(Spacing.CardInner)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = InkSecondary)
            Spacer(Modifier.height(6.dp))
            Text(
                text = Money.format(amount),
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun OutstandingSection(report: ReportData, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Outstanding",
            style = MaterialTheme.typography.titleLarge,
            color = InkPrimary
        )
        Spacer(Modifier.height(Spacing.XS))
        KhataGoCard(containerColor = SurfaceWhite) {
            Column(
                modifier = Modifier.padding(Spacing.CardInner),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (report.debtDistribution.isEmpty()) {
                    KhataGoEmptyState(
                        title = "Nothing outstanding",
                        message = "You have no open balances right now."
                    )
                } else {
                    KhataGoDonutChart(
                        slices = report.debtDistribution.mapIndexed { index, slice ->
                            DonutSlice(slice.kind.label, slice.outstanding, categoryColor(index))
                        },
                        centerLabel = "Total due",
                        centerValue = Money.format(report.outstanding.total)
                    )
                    Spacer(Modifier.height(Spacing.L))
                    report.debtDistribution.forEachIndexed { index, slice ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .width(8.dp)
                                        .height(8.dp)
                                        .padding(end = 0.dp)
                                        .then(
                                            Modifier.background(
                                                categoryColor(index),
                                                androidx.compose.foundation.shape.CircleShape
                                            )
                                        )
                                )
                                Spacer(Modifier.width(Spacing.S))
                                Text(
                                    text = slice.kind.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkPrimary
                                )
                            }
                            Text(
                                text = Money.format(slice.outstanding),
                                style = MaterialTheme.typography.titleSmall,
                                color = InkPrimary
                            )
                        }
                    }
                }
                if (report.overdueCount > 0) {
                    Spacer(Modifier.height(Spacing.M))
                    KhataGoKeyValueRow(
                        label = "Overdue",
                        value = "${report.overdueCount} · ${Money.format(report.overdueTotal)}",
                        valueColor = Negative
                    )
                }
                if (report.receivable.outstanding > 0L) {
                    KhataGoKeyValueRow(
                        label = "Money lent out, still due",
                        value = Money.format(report.receivable.outstanding),
                        valueColor = Upcoming
                    )
                }
            }
        }
    }
}

private fun categoryColor(index: Int): androidx.compose.ui.graphics.Color = when (index % 6) {
    0 -> KhataGoGreen
    1 -> Upcoming
    2 -> Positive
    3 -> Negative
    4 -> com.shohan.khatago.ui.theme.Info
    else -> com.shohan.khatago.ui.theme.KhataGoGreenMuted
}
