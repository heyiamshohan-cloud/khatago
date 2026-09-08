package com.shohan.khatago.ui.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.data.repository.DashboardRepository
import com.shohan.khatago.domain.model.AccountKind
import com.shohan.khatago.domain.model.DashboardSnapshot
import com.shohan.khatago.domain.model.InsightTone
import com.shohan.khatago.domain.model.LedgerEntry
import com.shohan.khatago.domain.model.MonthFlow
import com.shohan.khatago.domain.model.OutstandingBreakdown
import com.shohan.khatago.domain.model.UpcomingItem
import com.shohan.khatago.ui.components.KhataGoBarChart
import com.shohan.khatago.ui.components.KhataGoCard
import com.shohan.khatago.ui.components.KhataGoChartLegend
import com.shohan.khatago.ui.components.KhataGoDashboardHeader
import com.shohan.khatago.ui.components.KhataGoEmptyState
import com.shohan.khatago.ui.components.KhataGoQuickActionTile
import com.shohan.khatago.ui.components.KhataGoSectionHeader
import com.shohan.khatago.ui.components.KhataGoTransactionRow
import com.shohan.khatago.ui.components.KhataGoUpcomingRow
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkPrimary
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.KhataGoGreenSoft
import com.shohan.khatago.ui.theme.Negative
import com.shohan.khatago.ui.theme.OnHero
import com.shohan.khatago.ui.theme.OnHeroMuted
import com.shohan.khatago.ui.theme.Positive
import com.shohan.khatago.ui.theme.ShapeTokens
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceWhite
import com.shohan.khatago.ui.theme.Upcoming
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate

data class DashboardUiState(
    val greeting: String = "",
    val dateLabel: String = "",
    val snapshot: DashboardSnapshot = DashboardSnapshot(),
    val today: LocalDate = LocalDate.now()
)

class DashboardViewModel(repository: DashboardRepository) : ViewModel() {

    val state: StateFlow<DashboardUiState> = repository.observeDashboard()
        .map { snapshot ->
            DashboardUiState(
                greeting = if (snapshot.userName.isBlank()) {
                    KhataGoTime.greeting()
                } else {
                    "${KhataGoTime.greeting()}, ${snapshot.userName}"
                },
                dateLabel = KhataGoTime.formatFullDate(),
                snapshot = snapshot,
                today = KhataGoTime.today()
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}

/**
 * The flagship screen.
 *
 * Hierarchy: total position -> what needs attention now -> today -> trend ->
 * actions -> history. Every number is read from the database.
 */
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onSearch: () -> Unit,
    onProfile: () -> Unit,
    onQuickAction: (AccountKind) -> Unit,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onSeeAllUpcoming: () -> Unit,
    onSeeAllTransactions: () -> Unit,
    onOpenOverdue: () -> Unit,
    onOpenTransaction: (LedgerEntry) -> Unit,
    onOpenUpcoming: (UpcomingItem) -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snapshot = uiState.snapshot
    val today = uiState.today

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CanvasWhite,
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = onQuickAdd,
                containerColor = KhataGoGreen,
                contentColor = OnHero,
                shape = RoundedCornerShape(ShapeTokens.Medium)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.XXXL),
            verticalArrangement = Arrangement.spacedBy(Spacing.CardGap)
        ) {
            item {
                KhataGoDashboardHeader(
                    greeting = uiState.greeting,
                    dateLabel = uiState.dateLabel,
                    onSearch = onSearch,
                    onProfile = onProfile,
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            item {
                HeroBalanceCard(
                    outstanding = snapshot.outstanding,
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            if (snapshot.insights.isNotEmpty()) {
                item {
                    InsightCard(
                        insights = snapshot.insights,
                        modifier = Modifier.padding(horizontal = Spacing.Gutter)
                    )
                }
            }

            item {
                ObligationStrip(
                    overdueCount = snapshot.overdueCount,
                    overdueTotal = snapshot.overdueTotal,
                    dueTodayCount = snapshot.today.dueTodayCount,
                    dueTodayTotal = snapshot.today.dueToday,
                    onOpenOverdue = onOpenOverdue,
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            item {
                TodaySnapshotSection(
                    income = snapshot.today.income,
                    expense = snapshot.today.expense,
                    payments = snapshot.today.payments,
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            item {
                UpcomingSection(
                    items = snapshot.upcoming,
                    today = today,
                    onSeeAll = onSeeAllUpcoming,
                    onOpen = onOpenUpcoming,
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            item {
                MoneyOverviewCard(
                    overview = snapshot.overview,
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            item {
                QuickActionsSection(
                    onQuickAction = onQuickAction,
                    onAddIncome = onAddIncome,
                    onAddExpense = onAddExpense,
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            item {
                RecentTransactionsSection(
                    transactions = snapshot.recentTransactions,
                    onSeeAll = onSeeAllTransactions,
                    onOpen = onOpenTransaction,
                    onAddIncome = onAddIncome,
                    onAddExpense = onAddExpense,
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }
        }
    }
}

// ------------------------------------------------------------------ hero card

@Composable
private fun HeroBalanceCard(
    outstanding: OutstandingBreakdown,
    modifier: Modifier = Modifier
) {
    val total = outstanding.total
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ShapeTokens.Hero))
            .background(KhataGoGreen)
            .padding(Spacing.XXL)
    ) {
        Text(
            text = "Total Outstanding",
            style = MaterialTheme.typography.labelMedium,
            color = OnHeroMuted
        )
        Spacer(Modifier.height(6.dp))
        AnimatedAmount(
            amount = total,
            style = MaterialTheme.typography.displayMedium,
            color = OnHero
        )
        if (total == 0L) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = Spacing.XS)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = OnHero,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "You're all clear",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnHero
                )
            }
        }
        Spacer(Modifier.height(Spacing.L))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.16f))
        )
        Spacer(Modifier.height(Spacing.L))
        Row(modifier = Modifier.fillMaxWidth()) {
            HeroMetric(
                label = "Shop Credit",
                amount = outstanding.shopCredit,
                modifier = Modifier.weight(1f)
            )
            HeroMetric(
                label = "Loans",
                amount = outstanding.loans,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(Spacing.M))
        Row(modifier = Modifier.fillMaxWidth()) {
            HeroMetric(
                label = "EMI",
                amount = outstanding.emi,
                modifier = Modifier.weight(1f)
            )
            HeroMetric(
                label = "Personal Debt",
                amount = outstanding.borrowed,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HeroMetric(label: String, amount: Long, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = OnHeroMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = Money.format(amount),
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            color = OnHero,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Counts up to the stored value so the headline number feels alive. */
@Composable
private fun AnimatedAmount(
    amount: Long,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = tween(durationMillis = 550),
        label = "amount"
    )
    Text(
        text = Money.format(animated.toLong()),
        style = style,
        color = color,
        modifier = modifier
    )
}

// ------------------------------------------------------------- overdue strip

@Composable
private fun ObligationStrip(
    overdueCount: Int,
    overdueTotal: Long,
    dueTodayCount: Int,
    dueTodayTotal: Long,
    onOpenOverdue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.CardGap)
    ) {
        KhataGoCard(
            modifier = Modifier.weight(1f),
            onClick = onOpenOverdue,
            containerColor = if (overdueCount > 0) com.shohan.khatago.ui.theme.NegativeSoft else SurfaceWhite,
            borderColor = if (overdueCount > 0) Color.Transparent else com.shohan.khatago.ui.theme.OutlineSoft
        ) {
            Column(modifier = Modifier.padding(Spacing.CardInner)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (overdueCount > 0) Negative else Positive,
                                androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Spacer(Modifier.width(Spacing.XS))
                    Text(
                        text = "Overdue",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (overdueCount > 0) com.shohan.khatago.ui.theme.OnNegativeSoft else InkSecondary
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (overdueCount > 0) {
                        Money.format(overdueTotal)
                    } else {
                        "You're all clear"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                    color = if (overdueCount > 0) com.shohan.khatago.ui.theme.OnNegativeSoft else InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (overdueCount == 1) "1 payment" else "$overdueCount payments",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
        }

        KhataGoCard(modifier = Modifier.weight(1f), containerColor = SurfaceWhite) {
            Column(modifier = Modifier.padding(Spacing.CardInner)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (dueTodayCount > 0) Upcoming else Positive,
                                androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Spacer(Modifier.width(Spacing.XS))
                    Text(
                        text = "Due Today",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSecondary
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (dueTodayCount > 0) Money.format(dueTodayTotal) else "Nothing due",
                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                    color = InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (dueTodayCount == 1) "1 payment" else "$dueTodayCount payments",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
        }
    }
}

// ----------------------------------------------------------- today's snapshot

@Composable
private fun TodaySnapshotSection(
    income: Long,
    expense: Long,
    payments: Long,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        KhataGoSectionHeader(title = "Today", modifier = Modifier.padding(bottom = Spacing.XS))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.CardGap)
        ) {
            TodayPrimaryCard(
                label = "Income",
                amount = income,
                accent = Positive,
                modifier = Modifier.weight(1f)
            )
            TodayPrimaryCard(
                label = "Expense",
                amount = expense,
                accent = Negative,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(Spacing.CardGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.CardGap)
        ) {
            TodaySecondaryCard(
                label = "Payments",
                amount = payments,
                modifier = Modifier.weight(1f)
            )
            TodaySecondaryCard(
                label = "Net today",
                amount = income - expense,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TodayPrimaryCard(
    label: String,
    amount: Long,
    accent: Color,
    modifier: Modifier = Modifier
) {
    KhataGoCard(modifier = modifier, containerColor = SurfaceWhite) {
        Column(modifier = Modifier.padding(Spacing.CardInner)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = InkSecondary)
            Spacer(Modifier.height(6.dp))
            Text(
                text = Money.format(amount),
                style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                color = InkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(Spacing.S))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(accent, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun TodaySecondaryCard(
    label: String,
    amount: Long,
    modifier: Modifier = Modifier
) {
    KhataGoCard(modifier = modifier, containerColor = SurfaceWhite) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.CardInner, vertical = Spacing.M),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
            Text(
                text = Money.format(amount),
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                color = if (amount < 0L) Negative else InkPrimary
            )
        }
    }
}

// ---------------------------------------------------------------- upcoming

@Composable
private fun UpcomingSection(
    items: List<UpcomingItem>,
    today: LocalDate,
    onSeeAll: () -> Unit,
    onOpen: (UpcomingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        KhataGoSectionHeader(
            title = "Upcoming Payments",
            actionLabel = if (items.size > 4) "See all" else null,
            onAction = onSeeAll
        )
        if (items.isEmpty()) {
            KhataGoCard(containerColor = SurfaceWhite) {
                KhataGoEmptyState(
                    title = "No payments due soon",
                    message = "When a loan, EMI or repayment is due, it will appear here."
                )
            }
        } else {
            KhataGoCard(containerColor = SurfaceWhite) {
                Column {
                    items.take(4).forEachIndexed { index, item ->
                        KhataGoUpcomingRow(
                            dateLabel = KhataGoTime.formatRelativeDate(item.dueDate, today),
                            title = item.title,
                            subtitle = item.subtitle,
                            amount = item.remaining,
                            dueState = item.dueState,
                            onClick = { onOpen(item) }
                        )
                        if (index < minOf(items.size, 4) - 1) {
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

// ---------------------------------------------------------------- chart

@Composable
private fun MoneyOverviewCard(overview: List<MonthFlow>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        KhataGoSectionHeader(title = "Money Overview")
        KhataGoCard(containerColor = SurfaceWhite) {
            Column(modifier = Modifier.padding(Spacing.CardInner)) {
                if (overview.all { it.income == 0L && it.expense == 0L }) {
                    KhataGoEmptyState(
                        title = "Nothing to chart yet",
                        message = "Add income or expenses to start seeing your trends."
                    )
                } else {
                    KhataGoChartLegend(
                        items = listOf(
                            "Income" to Positive,
                            "Expense" to Negative
                        )
                    )
                    Spacer(Modifier.height(Spacing.M))
                    KhataGoBarChart(data = overview)
                    Spacer(Modifier.height(Spacing.M))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Income",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSecondary
                            )
                            Text(
                                Money.format(overview.sumOf { it.income }),
                                style = MaterialTheme.typography.titleSmall,
                                color = InkPrimary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Expense",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSecondary
                            )
                            Text(
                                Money.format(overview.sumOf { it.expense }),
                                style = MaterialTheme.typography.titleSmall,
                                color = InkPrimary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Net",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSecondary
                            )
                            Text(
                                Money.format(overview.sumOf { it.net }),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (overview.sumOf { it.net } < 0) Negative else Positive
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------- quick actions

private data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun QuickActionsSection(
    onQuickAction: (AccountKind) -> Unit,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = remember(onQuickAction, onAddIncome, onAddExpense) {
        listOf(
            QuickAction("Shop Credit", Icons.Outlined.Storefront) {
                onQuickAction(AccountKind.SHOP_CREDIT)
            },
            QuickAction("Loan", Icons.Outlined.AccountBalance) { onQuickAction(AccountKind.LOAN) },
            QuickAction("EMI", Icons.Outlined.CreditCard) { onQuickAction(AccountKind.EMI) },
            QuickAction("Personal Debt", Icons.Outlined.Person) {
                onQuickAction(AccountKind.PERSONAL_DEBT)
            },
            QuickAction("Income", Icons.Outlined.TrendingUp, onAddIncome),
            QuickAction("Expense", Icons.Outlined.TrendingDown, onAddExpense)
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        KhataGoSectionHeader(title = "Quick Actions")
        // Exactly six tiles: two columns, three rows, identical size.
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.CardGap)) {
            actions.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.CardGap)
                ) {
                    row.forEach { action ->
                        Box(modifier = Modifier.weight(1f)) {
                            KhataGoQuickActionTile(
                                label = action.label,
                                icon = action.icon,
                                onClick = action.onClick
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------- recent items

@Composable
private fun RecentTransactionsSection(
    transactions: List<LedgerEntry>,
    onSeeAll: () -> Unit,
    onOpen: (LedgerEntry) -> Unit,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        KhataGoSectionHeader(
            title = "Recent Activity",
            actionLabel = if (transactions.isNotEmpty()) "See all" else null,
            onAction = onSeeAll
        )
        if (transactions.isEmpty()) {
            KhataGoCard(containerColor = SurfaceWhite) {
                KhataGoEmptyState(
                    title = "No transactions yet",
                    message = "Add income or expenses to start building your history."
                )
            }
        } else {
            KhataGoCard(containerColor = SurfaceWhite) {
                Column {
                    transactions.take(5).forEachIndexed { index, entry ->
                        KhataGoTransactionRow(entry = entry, onClick = { onOpen(entry) })
                        if (index < minOf(transactions.size, 5) - 1) {
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

@Composable
private fun InsightCard(
    insights: List<com.shohan.khatago.domain.model.Insight>,
    modifier: Modifier = Modifier
) {
    KhataGoCard(modifier = modifier, containerColor = KhataGoGreenSoft, borderColor = Color.Transparent) {
        Column(modifier = Modifier.padding(Spacing.CardInner)) {
            insights.take(2).forEachIndexed { index, insight ->
                if (index > 0) Spacer(Modifier.height(Spacing.M))
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                when (insight.tone) {
                                    InsightTone.POSITIVE -> Positive
                                    InsightTone.NEGATIVE -> Negative
                                    InsightTone.NEUTRAL -> KhataGoGreen
                                },
                                androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Spacer(Modifier.width(Spacing.S))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = insight.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = InkPrimary
                        )
                        Text(
                            text = insight.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                }
            }
        }
    }
}
