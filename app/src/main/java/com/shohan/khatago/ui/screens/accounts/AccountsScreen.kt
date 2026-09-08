package com.shohan.khatago.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.data.repository.EmiRepository
import com.shohan.khatago.data.repository.LoanRepository
import com.shohan.khatago.data.repository.PersonalRepository
import com.shohan.khatago.data.repository.ShopRepository
import com.shohan.khatago.domain.model.AccountKind
import com.shohan.khatago.domain.model.BorrowedAccount
import com.shohan.khatago.domain.model.DueState
import com.shohan.khatago.domain.model.EmiAccount
import com.shohan.khatago.domain.model.LentAccount
import com.shohan.khatago.domain.model.LoanAccount
import com.shohan.khatago.domain.model.OutstandingBreakdown
import com.shohan.khatago.domain.model.ShopAccount
import com.shohan.khatago.ui.components.KhataGoAccountRow
import com.shohan.khatago.ui.components.KhataGoCard
import com.shohan.khatago.ui.components.KhataGoChip
import com.shohan.khatago.ui.components.KhataGoEmptyState
import com.shohan.khatago.ui.components.KhataGoSegmentedControl
import com.shohan.khatago.ui.components.KhataGoTopBar
import com.shohan.khatago.ui.components.iconForKind
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkPrimary
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.InkTertiary
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.OnHero
import com.shohan.khatago.ui.theme.OutlineSoft
import com.shohan.khatago.ui.theme.ShapeTokens
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceSubtle
import com.shohan.khatago.ui.theme.SurfaceWhite
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohan.khatago.core.time.TodayProvider
import com.shohan.khatago.data.repository.combineAll

enum class PersonalTab { BORROWED, LENT }

data class AccountsUiState(
    val tab: AccountKind = AccountKind.SHOP_CREDIT,
    val personalTab: PersonalTab = PersonalTab.BORROWED,
    val query: String = "",
    val shops: List<ShopAccount> = emptyList(),
    val loans: List<LoanAccount> = emptyList(),
    val emis: List<EmiAccount> = emptyList(),
    val borrowed: List<BorrowedAccount> = emptyList(),
    val lent: List<LentAccount> = emptyList(),
    val outstanding: OutstandingBreakdown = OutstandingBreakdown(),
    val lentOutstanding: Long = 0L,
    val today: java.time.LocalDate = java.time.LocalDate.now()
)

class AccountsViewModel(
    shopRepository: ShopRepository,
    loanRepository: LoanRepository,
    emiRepository: EmiRepository,
    personalRepository: PersonalRepository,
    todayProvider: TodayProvider
) : ViewModel() {

    private val tab = kotlinx.coroutines.flow.MutableStateFlow(AccountKind.SHOP_CREDIT)
    private val personalTab = kotlinx.coroutines.flow.MutableStateFlow(PersonalTab.BORROWED)
    private val query = kotlinx.coroutines.flow.MutableStateFlow("")

    val state: StateFlow<AccountsUiState> = todayProvider.epochDay
        .flatMapLatest { todayEpochDay ->
            combineAll(
                listOf(
                    tab,
                    personalTab,
                    query,
                    shopRepository.observeAccounts(),
                    loanRepository.observeAccounts(todayEpochDay),
                    emiRepository.observeAccounts(todayEpochDay),
                    personalRepository.observeBorrowed(),
                    personalRepository.observeLent()
                )
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val shops = values[3] as List<ShopAccount>
                @Suppress("UNCHECKED_CAST")
                val loans = values[4] as List<LoanAccount>
                @Suppress("UNCHECKED_CAST")
                val emis = values[5] as List<EmiAccount>
                @Suppress("UNCHECKED_CAST")
                val borrowed = values[6] as List<BorrowedAccount>
                @Suppress("UNCHECKED_CAST")
                val lent = values[7] as List<LentAccount>

                AccountsUiState(
                    tab = values[0] as AccountKind,
                    personalTab = values[1] as PersonalTab,
                    query = values[2] as String,
                    shops = shops,
                    loans = loans,
                    emis = emis,
                    borrowed = borrowed,
                    lent = lent,
                    outstanding = OutstandingBreakdown(
                        shopCredit = shops.sumOf { it.remaining },
                        loans = loans.sumOf { it.remaining },
                        emi = emis.sumOf { it.remaining },
                        borrowed = borrowed.sumOf { it.remaining }
                    ),
                    lentOutstanding = lent.sumOf { it.remaining },
                    today = java.time.LocalDate.ofEpochDay(todayEpochDay)
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

    fun selectTab(kind: AccountKind) {
        tab.value = kind
    }

    fun selectPersonalTab(value: PersonalTab) {
        personalTab.value = value
    }

    fun setQuery(value: String) {
        query.value = value
    }
}

/**
 * Accounts: everything the user owes (and is owed) in one place, with the total
 * outstanding up front and a contextual search for each tab.
 */
@Composable
fun AccountsScreen(
    uiState: AccountsUiState,
    onSelectTab: (AccountKind) -> Unit,
    onSelectPersonalTab: (PersonalTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onAddAccount: (AccountKind) -> Unit,
    onOpenShop: (Long) -> Unit,
    onOpenLoan: (Long) -> Unit,
    onOpenEmi: (Long) -> Unit,
    onOpenBorrowed: (Long) -> Unit,
    onOpenLent: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val query = uiState.query.trim()

    Scaffold(
        containerColor = CanvasWhite,
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = { onAddAccount(uiState.tab) },
                containerColor = KhataGoGreen,
                contentColor = OnHero,
                shape = RoundedCornerShape(ShapeTokens.Medium)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add account")
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
                KhataGoTopBar(
                    title = "Accounts",
                    subtitle = "Everything you owe, in one place",
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            item {
                OutstandingSummary(
                    outstanding = uiState.outstanding,
                    lentOutstanding = uiState.lentOutstanding,
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
                    AccountKind.entries.forEach { kind ->
                        KhataGoChip(
                            text = kind.label,
                            selected = uiState.tab == kind,
                            onClick = { onSelectTab(kind) }
                        )
                    }
                }
            }

            item {
                AccountsSearchField(
                    query = uiState.query,
                    placeholder = searchPlaceholder(uiState.tab, uiState.personalTab),
                    onQueryChange = onQueryChange,
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            if (uiState.tab == AccountKind.PERSONAL_DEBT) {
                item {
                    KhataGoSegmentedControl(
                        options = PersonalTab.entries,
                        selected = uiState.personalTab,
                        onSelected = onSelectPersonalTab,
                        label = { if (it == PersonalTab.BORROWED) "Borrowed" else "Lent" },
                        modifier = Modifier.padding(horizontal = Spacing.Gutter)
                    )
                }
            }

            when (uiState.tab) {
                AccountKind.SHOP_CREDIT -> {
                    val items = uiState.shops.filter {
                        query.isBlank() || it.name.contains(query, true) || it.ownerName.contains(query, true)
                    }
                    if (items.isEmpty()) {
                        item { AccountsEmptyState(message = "No shops yet", hint = "Add your first shop to start tracking credit purchases.") }
                    } else {
                        items(items, key = { it.id }) { shop ->
                            KhataGoAccountRow(
                                title = shop.name,
                                subtitle = shop.ownerName.ifBlank {
                                    if (shop.creditCount == 1) "1 purchase" else "${shop.creditCount} purchases"
                                },
                                remaining = shop.remaining,
                                total = shop.totalCredit,
                                kind = AccountKind.SHOP_CREDIT,
                                dueState = if (shop.remaining > 0L) DueState.SCHEDULED else DueState.SETTLED,
                                nextDueLabel = shop.lastActivity?.let { "Last · ${KhataGoTime.formatShortDate(it)}" },
                                onClick = { onOpenShop(shop.id) },
                                modifier = Modifier.padding(horizontal = Spacing.Gutter)
                            )
                        }
                    }
                }

                AccountKind.LOAN -> {
                    val items = uiState.loans.filter {
                        query.isBlank() || it.name.contains(query, true) || it.institution.contains(query, true)
                    }
                    if (items.isEmpty()) {
                        item { AccountsEmptyState(message = "No loans yet", hint = "Add a loan to track its installment schedule.") }
                    } else {
                        items(items, key = { it.id }) { loan ->
                            KhataGoAccountRow(
                                title = loan.name,
                                subtitle = loan.institution.ifBlank {
                                    "${loan.remainingCount} of ${loan.installmentCount} left"
                                },
                                remaining = loan.remaining,
                                total = loan.totalPayable,
                                kind = AccountKind.LOAN,
                                dueState = loan.dueState,
                                nextDueLabel = loan.nextDueDate?.let { "Next · ${KhataGoTime.formatShortDate(it)}" },
                                onClick = { onOpenLoan(loan.id) },
                                modifier = Modifier.padding(horizontal = Spacing.Gutter)
                            )
                        }
                    }
                }

                AccountKind.EMI -> {
                    val items = uiState.emis.filter {
                        query.isBlank() || it.productName.contains(query, true) || it.seller.contains(query, true)
                    }
                    if (items.isEmpty()) {
                        item { AccountsEmptyState(message = "No products yet", hint = "Add a product bought on installments to track your EMI.") }
                    } else {
                        items(items, key = { it.id }) { emi ->
                            KhataGoAccountRow(
                                title = emi.productName,
                                subtitle = emi.seller.ifBlank { "${emi.remainingCount} of ${emi.installmentCount} left" },
                                remaining = emi.remaining,
                                total = emi.totalPayable,
                                kind = AccountKind.EMI,
                                dueState = emi.dueState,
                                nextDueLabel = emi.nextDueDate?.let { "Next · ${KhataGoTime.formatShortDate(it)}" },
                                onClick = { onOpenEmi(emi.id) },
                                modifier = Modifier.padding(horizontal = Spacing.Gutter)
                            )
                        }
                    }
                }

                AccountKind.PERSONAL_DEBT -> {
                    if (uiState.personalTab == PersonalTab.BORROWED) {
                        val items = uiState.borrowed.filter {
                            query.isBlank() || it.personName.contains(query, true) ||
                                it.relationship.contains(query, true)
                        }
                        if (items.isEmpty()) {
                            item { AccountsEmptyState(message = "No borrowed money", hint = "Add money you borrowed to track repayments.") }
                        } else {
                            items(items, key = { it.id }) { debt ->
                                KhataGoAccountRow(
                                    title = debt.personName,
                                    subtitle = debt.relationship.ifBlank { "Borrowed" },
                                    remaining = debt.remaining,
                                    total = debt.amount,
                                    kind = AccountKind.PERSONAL_DEBT,
                                    dueState = debt.dueState,
                                    nextDueLabel = debt.expectedReturnDate?.let {
                                        "Due · ${KhataGoTime.formatShortDate(it)}"
                                    },
                                    onClick = { onOpenBorrowed(debt.id) },
                                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                                )
                            }
                        }
                    } else {
                        val items = uiState.lent.filter {
                            query.isBlank() || it.personName.contains(query, true) ||
                                it.relationship.contains(query, true)
                        }
                        if (items.isEmpty()) {
                            item { AccountsEmptyState(message = "No money lent", hint = "Add money you lent to track what you get back.") }
                        } else {
                            items(items, key = { it.id }) { lent ->
                                KhataGoAccountRow(
                                    title = lent.personName,
                                    subtitle = lent.relationship.ifBlank { "Lent" },
                                    remaining = lent.remaining,
                                    total = lent.amount,
                                    kind = AccountKind.PERSONAL_DEBT,
                                    dueState = lent.dueState,
                                    nextDueLabel = lent.expectedReturnDate?.let {
                                        "Expected · ${KhataGoTime.formatShortDate(it)}"
                                    },
                                    onClick = { onOpenLent(lent.id) },
                                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun searchPlaceholder(tab: AccountKind, personalTab: PersonalTab): String = when (tab) {
    AccountKind.SHOP_CREDIT -> "Search shops..."
    AccountKind.LOAN -> "Search loans..."
    AccountKind.EMI -> "Search products..."
    AccountKind.PERSONAL_DEBT -> if (personalTab == PersonalTab.BORROWED) "Search people..." else "Search people..."
}

@Composable
private fun OutstandingSummary(
    outstanding: OutstandingBreakdown,
    lentOutstanding: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(KhataGoGreen, RoundedCornerShape(ShapeTokens.Hero))
            .padding(Spacing.XXL)
    ) {
        Text(
            text = "Total Outstanding",
            style = MaterialTheme.typography.labelMedium,
            color = com.shohan.khatago.ui.theme.OnHeroMuted
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = Money.format(outstanding.total),
            style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
            color = OnHero
        )
        Spacer(Modifier.height(Spacing.L))
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryMetric("Shop Credit", outstanding.shopCredit, Modifier.weight(1f))
            SummaryMetric("Loans", outstanding.loans, Modifier.weight(1f))
        }
        Spacer(Modifier.height(Spacing.M))
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryMetric("EMI", outstanding.emi, Modifier.weight(1f))
            SummaryMetric("Personal Debt", outstanding.borrowed, Modifier.weight(1f))
        }
        if (lentOutstanding > 0L) {
            Spacer(Modifier.height(Spacing.M))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.16f))
            )
            Spacer(Modifier.height(Spacing.M))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Money lent out, still due",
                    style = MaterialTheme.typography.bodySmall,
                    color = com.shohan.khatago.ui.theme.OnHeroMuted
                )
                Text(
                    text = Money.format(lentOutstanding),
                    style = MaterialTheme.typography.titleSmall,
                    color = OnHero
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, amount: Long, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = com.shohan.khatago.ui.theme.OnHeroMuted,
            maxLines = 1
        )
        Text(
            text = Money.format(amount),
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            color = OnHero,
            maxLines = 1
        )
    }
}

@Composable
private fun AccountsSearchField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = InkTertiary) },
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

@Composable
private fun AccountsEmptyState(message: String, hint: String) {
    KhataGoCard(
        containerColor = SurfaceWhite,
        modifier = Modifier.padding(horizontal = Spacing.Gutter)
    ) {
        KhataGoEmptyState(title = message, message = hint)
    }
}
