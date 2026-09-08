package com.shohan.khatago.ui.screens.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.data.repository.LedgerRepository
import com.shohan.khatago.data.repository.combineAll
import com.shohan.khatago.domain.model.LedgerEntry
import com.shohan.khatago.domain.model.TransactionType
import com.shohan.khatago.ui.components.KhataGoCard
import com.shohan.khatago.ui.components.KhataGoChip
import com.shohan.khatago.ui.components.KhataGoEmptyState
import com.shohan.khatago.ui.components.KhataGoTopBar
import com.shohan.khatago.ui.components.KhataGoTransactionRow
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkPrimary
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.InkTertiary
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.Negative
import com.shohan.khatago.ui.theme.OutlineSoft
import com.shohan.khatago.ui.theme.Positive
import com.shohan.khatago.ui.theme.ShapeTokens
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceWhite
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

enum class TransactionsFilter(val label: String) { INCOME("Income"), EXPENSE("Expense"), ALL("All") }

data class TransactionsUiState(
    val filter: TransactionsFilter = TransactionsFilter.ALL,
    val query: String = "",
    val entries: List<LedgerEntry> = emptyList(),
    val incomeTotal: Long = 0L,
    val expenseTotal: Long = 0L,
    val paymentTotal: Long = 0L
)

class TransactionsViewModel(private val ledgerRepository: LedgerRepository) : ViewModel() {

    private val filter = MutableStateFlow(TransactionsFilter.ALL)
    private val query = MutableStateFlow("")

    val state: StateFlow<TransactionsUiState> =
        filter.flatMapLatest { currentFilter ->
            combineAll(
                listOf(
                    query,
                    ledgerRepository.observeTransactionsFiltered(
                        startDay = 0L,
                        endDay = 10_000_000L,
                        type = when (currentFilter) {
                            TransactionsFilter.INCOME -> TransactionType.INCOME
                            TransactionsFilter.EXPENSE -> TransactionType.EXPENSE
                            TransactionsFilter.ALL -> null
                        },
                        limit = 300
                    )
                )
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val all = values[1] as List<LedgerEntry>
                val text = (values[0] as String).trim()
                val filtered = if (text.isBlank()) {
                    all
                } else {
                    all.filter {
                        it.description.contains(text, ignoreCase = true) ||
                            it.notes.contains(text, ignoreCase = true) ||
                            it.category.contains(text, ignoreCase = true)
                    }
                }
                TransactionsUiState(
                    filter = currentFilter,
                    query = values[0] as String,
                    entries = filtered,
                    incomeTotal = filtered.filter { it.type.countsAsIncome }.sumOf { it.amount },
                    expenseTotal = filtered.filter { it.type.countsAsExpense }.sumOf { it.amount },
                    paymentTotal = filtered.filter { it.type.countsAsPayment }.sumOf { it.amount }
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun selectFilter(value: TransactionsFilter) {
        filter.value = value
    }

    fun setQuery(value: String) {
        query.value = value
    }
}

/**
 * Transactions: the unified activity history. Income and expense are visually
 * distinct, and every row is real data from the central ledger.
 */
@Composable
fun TransactionsScreen(
    uiState: TransactionsUiState,
    onSelectFilter: (TransactionsFilter) -> Unit,
    onQueryChange: (String) -> Unit,
    onOpenEntry: (LedgerEntry) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    title = "Transactions",
                    subtitle = if (uiState.entries.isEmpty()) "Nothing recorded yet" else
                        "${uiState.entries.size} shown",
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.Gutter),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.S)
                ) {
                    TransactionsFilter.entries.forEach { option ->
                        KhataGoChip(
                            text = option.label,
                            selected = uiState.filter == option,
                            onClick = { onSelectFilter(option) }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.Gutter),
                    placeholder = {
                        Text(
                            "Search source or note...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkTertiary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(ShapeTokens.Medium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KhataGoGreen,
                        unfocusedBorderColor = OutlineSoft,
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite,
                        cursorColor = KhataGoGreen
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = InkTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }

            if (uiState.entries.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.Gutter),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.CardGap)
                    ) {
                        TotalsCard(
                            label = "Income",
                            amount = uiState.incomeTotal,
                            color = Positive,
                            modifier = Modifier.weight(1f)
                        )
                        TotalsCard(
                            label = "Expense",
                            amount = uiState.expenseTotal,
                            color = Negative,
                            modifier = Modifier.weight(1f)
                        )
                        TotalsCard(
                            label = "Payments",
                            amount = uiState.paymentTotal,
                            color = KhataGoGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (uiState.entries.isEmpty()) {
                item {
                    KhataGoCard(
                        containerColor = SurfaceWhite,
                        modifier = Modifier.padding(horizontal = Spacing.Gutter)
                    ) {
                        KhataGoEmptyState(
                            title = "No transactions yet",
                            message = "Add income or expenses to start building your history."
                        )
                    }
                }
            } else {
                val grouped = uiState.entries.groupBy { it.date }
                grouped.keys.sortedDescending().forEach { date ->
                    item {
                        Text(
                            text = KhataGoTime.formatTimelineHeader(date),
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSecondary,
                            modifier = Modifier.padding(
                                start = Spacing.Gutter + Spacing.XS,
                                top = Spacing.S,
                                bottom = Spacing.XS
                            )
                        )
                    }
                    item {
                        KhataGoCard(
                            containerColor = SurfaceWhite,
                            modifier = Modifier.padding(horizontal = Spacing.Gutter)
                        ) {
                            Column {
                                grouped[date]?.forEachIndexed { index, entry ->
                                    KhataGoTransactionRow(entry = entry, onClick = { onOpenEntry(entry) })
                                    if (index < (grouped[date]?.size ?: 1) - 1) {
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
private fun TotalsCard(
    label: String,
    amount: Long,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    KhataGoCard(modifier = modifier, containerColor = SurfaceWhite) {
        Column(modifier = Modifier.padding(horizontal = Spacing.L, vertical = Spacing.M)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = InkSecondary)
            Text(
                text = Money.format(amount),
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                color = color,
                maxLines = 1
            )
        }
    }
}
