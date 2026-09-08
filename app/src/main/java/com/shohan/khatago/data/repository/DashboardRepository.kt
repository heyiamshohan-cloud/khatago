package com.shohan.khatago.data.repository

import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.core.time.TodayProvider
import com.shohan.khatago.data.local.db.dao.EmiDao
import com.shohan.khatago.data.local.db.dao.LoanDao
import com.shohan.khatago.data.local.db.dao.PersonalDao
import com.shohan.khatago.data.local.db.dao.ShopDao
import com.shohan.khatago.data.local.db.rows.ShopDueRow
import com.shohan.khatago.data.local.db.dao.UpcomingEmiRow
import com.shohan.khatago.data.local.db.dao.UpcomingLoanRow
import com.shohan.khatago.data.local.db.dao.UpcomingPersonalRow
import com.shohan.khatago.domain.finance.AllocationEngine
import com.shohan.khatago.domain.finance.DueEngine
import com.shohan.khatago.domain.finance.InsightEngine
import com.shohan.khatago.domain.model.AccountKind
import com.shohan.khatago.domain.model.DashboardSnapshot
import com.shohan.khatago.domain.model.DueState
import com.shohan.khatago.domain.model.LedgerEntry
import com.shohan.khatago.domain.model.MonthFlow
import com.shohan.khatago.domain.model.OutstandingBreakdown
import com.shohan.khatago.domain.model.ReceivableSummary
import com.shohan.khatago.domain.model.TodaySnapshot
import com.shohan.khatago.domain.model.UpcomingItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * The dashboard is a pure projection of the database: every number below is
 * recomputed from stored rows whenever anything changes. Nothing is cached,
 * hardcoded or sampled.
 */
class DashboardRepository(
    private val shopDao: ShopDao,
    private val loanDao: LoanDao,
    private val emiDao: EmiDao,
    private val personalDao: PersonalDao,
    private val shopRepository: ShopRepository,
    private val loanRepository: LoanRepository,
    private val emiRepository: EmiRepository,
    private val personalRepository: PersonalRepository,
    private val ledgerRepository: LedgerRepository,
    private val todayProvider: TodayProvider
) {

    /** How far ahead the "Upcoming payments" list looks. */
    private val upcomingHorizonDays = 45L
    private val overviewMonths = 6

    fun observeDashboard(): Flow<DashboardSnapshot> =
        todayProvider.epochDay.flatMapLatest { todayEpochDay ->
            val today = LocalDate.ofEpochDay(todayEpochDay)
            val horizon = today.plusDays(upcomingHorizonDays).toEpochDay()
            val monthStart = KhataGoTime.startOfMonth(today).toEpochDay()
            val monthEnd = KhataGoTime.endOfMonth(today).toEpochDay()
            val (lastMonthStart, lastMonthEnd) = KhataGoTime.lastMonthRange(today)
            val overviewRange = monthPeriods(today, overviewMonths)

            combineAll(
                listOf(
                    ledgerRepository.observeProfile(),
                    shopRepository.observeAccounts(),
                    loanRepository.observeAccounts(todayEpochDay),
                    emiRepository.observeAccounts(todayEpochDay),
                    personalRepository.observeBorrowed(),
                    personalRepository.observeLent(),
                    shopDao.observeCreditsWithDueDate(),
                    loanDao.observeUpcomingInstallments(horizon, OBLIGATION_LIMIT),
                    emiDao.observeUpcomingInstallments(horizon, OBLIGATION_LIMIT),
                    personalDao.observeUpcomingBorrowed(horizon, OBLIGATION_LIMIT),
                    personalDao.observeUpcomingLent(horizon, OBLIGATION_LIMIT),
                    ledgerRepository.observeTodaySnapshot(todayEpochDay),
                    ledgerRepository.observeRecentTransactions(RECENT_LIMIT),
                    ledgerRepository.observeIncomeByMonth(overviewRange.first, overviewRange.second),
                    ledgerRepository.observeExpenseByMonth(overviewRange.first, overviewRange.second),
                    ledgerRepository.observePaymentsByMonth(overviewRange.first, overviewRange.second),
                    ledgerRepository.observeIncomeTotalFlow(monthStart, monthEnd),
                    ledgerRepository.observeExpenseTotalFlow(monthStart, monthEnd),
                    ledgerRepository.observeIncomeTotalFlow(lastMonthStart, lastMonthEnd),
                    ledgerRepository.observeExpenseTotalFlow(lastMonthStart, lastMonthEnd)
                )
            ) { values ->
                val profile = values[0] as com.shohan.khatago.data.local.db.entity.UserProfileEntity?
                val shops = values[1] as List<com.shohan.khatago.domain.model.ShopAccount>
                val loans = values[2] as List<com.shohan.khatago.domain.model.LoanAccount>
                val emis = values[3] as List<com.shohan.khatago.domain.model.EmiAccount>
                val borrowed = values[4] as List<com.shohan.khatago.domain.model.BorrowedAccount>
                val lent = values[5] as List<com.shohan.khatago.domain.model.LentAccount>
                val shopDue = values[6] as List<ShopDueRow>
                val loanUpcoming = values[7] as List<UpcomingLoanRow>
                val emiUpcoming = values[8] as List<UpcomingEmiRow>
                val borrowedUpcoming = values[9] as List<UpcomingPersonalRow>
                val lentUpcoming = values[10] as List<UpcomingPersonalRow>
                val todaySnapshot = values[11] as TodaySnapshot
                val recent = values[12] as List<LedgerEntry>
                val incomeMonths = values[13] as List<com.shohan.khatago.data.local.db.rows.MonthTotalRow>
                val expenseMonths = values[14] as List<com.shohan.khatago.data.local.db.rows.MonthTotalRow>
                val paymentMonths = values[15] as List<com.shohan.khatago.data.local.db.rows.MonthTotalRow>
                val monthIncome = values[16] as Long
                val monthExpense = values[17] as Long
                val lastMonthIncome = values[18] as Long
                val lastMonthExpense = values[19] as Long

                val outstanding = OutstandingBreakdown(
                    shopCredit = shops.sumOf { it.remaining },
                    loans = loans.sumOf { it.remaining },
                    emi = emis.sumOf { it.remaining },
                    borrowed = borrowed.sumOf { it.remaining }
                )
                val receivable = ReceivableSummary(
                    lent = lent.sumOf { it.amount },
                    received = lent.sumOf { it.receivedAmount },
                    outstanding = lent.sumOf { it.remaining }
                )

                val obligations = buildObligations(
                    today = today,
                    shopDue = shopDue,
                    loanUpcoming = loanUpcoming,
                    emiUpcoming = emiUpcoming,
                    borrowedUpcoming = borrowedUpcoming,
                    lentUpcoming = lentUpcoming
                )

                val overdue = obligations.filter { it.dueState == DueState.OVERDUE }
                val dueToday = obligations.filter { it.dueState == DueState.DUE_TODAY }
                val upcoming = obligations
                    .filter { it.dueState == DueState.DUE_SOON || it.dueState == DueState.SCHEDULED }
                    .sortedWith(compareBy({ it.dueDate.toEpochDay() }, { it.kind.name }))
                    .take(6)

                val overview = buildOverview(incomeMonths, expenseMonths, paymentMonths, overviewRange)

                val largest = listOf(
                    "Shop credit" to outstanding.shopCredit,
                    "Loans" to outstanding.loans,
                    "EMI" to outstanding.emi,
                    "Personal debt" to outstanding.borrowed
                ).filter { it.second > 0L }.maxByOrNull { it.second }

                val insights = InsightEngine.generate(
                    InsightEngine.Input(
                        monthIncome = monthIncome,
                        monthExpense = monthExpense,
                        lastMonthIncome = lastMonthIncome,
                        lastMonthExpense = lastMonthExpense,
                        overdueCount = overdue.size,
                        overdueTotal = overdue.sumOf { it.remaining },
                        upcomingCount = obligations.count {
                            it.dueState == DueState.DUE_SOON || it.dueState == DueState.DUE_TODAY
                        },
                        upcomingTotal = obligations
                            .filter { it.dueState == DueState.DUE_SOON || it.dueState == DueState.DUE_TODAY }
                            .sumOf { it.remaining },
                        largestOutstandingName = largest?.first,
                        largestOutstanding = largest?.second ?: 0L
                    )
                )

                val hasAnyData = shops.isNotEmpty() || loans.isNotEmpty() || emis.isNotEmpty() ||
                    borrowed.isNotEmpty() || lent.isNotEmpty() || recent.isNotEmpty() ||
                    outstanding.total > 0L

                DashboardSnapshot(
                    userName = profile?.name.orEmpty(),
                    hasAnyData = hasAnyData,
                    outstanding = outstanding,
                    receivable = receivable,
                    overdueCount = overdue.size,
                    overdueTotal = overdue.sumOf { it.remaining },
                    today = todaySnapshot.copy(
                        dueToday = dueToday.sumOf { it.remaining },
                        dueTodayCount = dueToday.size
                    ),
                    upcoming = upcoming,
                    overview = overview,
                    recentTransactions = recent,
                    insights = insights
                )
            }
        }

    private fun buildObligations(
        today: LocalDate,
        shopDue: List<ShopDueRow>,
        loanUpcoming: List<UpcomingLoanRow>,
        emiUpcoming: List<UpcomingEmiRow>,
        borrowedUpcoming: List<UpcomingPersonalRow>,
        lentUpcoming: List<UpcomingPersonalRow>
    ): List<UpcomingItem> {
        val items = ArrayList<UpcomingItem>()

        shopDue.forEach { row ->
            val remaining = AllocationEngine.creditRemaining(
                creditTotal = row.totalAmount,
                olderCreditTotal = row.cumulativeCredit - row.totalAmount,
                shopPaid = row.shopPaid
            )
            if (remaining > 0L) {
                val dueDate = LocalDate.ofEpochDay(row.dueDateEpochDay)
                items += UpcomingItem(
                    kind = AccountKind.SHOP_CREDIT,
                    refId = row.id,
                    title = row.shopName,
                    subtitle = "Shop Credit",
                    dueDate = dueDate,
                    remaining = remaining,
                    dueState = DueEngine.state(remaining, dueDate, today)
                )
            }
        }

        loanUpcoming.forEach { row ->
            val dueDate = LocalDate.ofEpochDay(row.dueDateEpochDay)
            items += UpcomingItem(
                kind = AccountKind.LOAN,
                refId = row.refId,
                title = row.title,
                subtitle = if (row.subtitle.isBlank()) "Loan" else row.subtitle,
                dueDate = dueDate,
                remaining = row.remainingAmount.coerceAtLeast(0L),
                dueState = DueEngine.state(row.remainingAmount.coerceAtLeast(0L), dueDate, today)
            )
        }

        emiUpcoming.forEach { row ->
            val dueDate = LocalDate.ofEpochDay(row.dueDateEpochDay)
            items += UpcomingItem(
                kind = AccountKind.EMI,
                refId = row.refId,
                title = row.title,
                subtitle = if (row.subtitle.isBlank()) "EMI" else row.subtitle,
                dueDate = dueDate,
                remaining = row.remainingAmount.coerceAtLeast(0L),
                dueState = DueEngine.state(row.remainingAmount.coerceAtLeast(0L), dueDate, today)
            )
        }

        borrowedUpcoming.forEach { row ->
            val dueDate = LocalDate.ofEpochDay(row.dueDateEpochDay)
            items += UpcomingItem(
                kind = AccountKind.PERSONAL_DEBT,
                refId = row.refId,
                title = row.title,
                subtitle = "Borrowed",
                dueDate = dueDate,
                remaining = row.remainingAmount.coerceAtLeast(0L),
                dueState = DueEngine.state(row.remainingAmount.coerceAtLeast(0L), dueDate, today)
            )
        }

        lentUpcoming.forEach { row ->
            val dueDate = LocalDate.ofEpochDay(row.dueDateEpochDay)
            items += UpcomingItem(
                kind = AccountKind.PERSONAL_DEBT,
                refId = row.refId,
                title = row.title,
                subtitle = "Lent",
                dueDate = dueDate,
                remaining = row.remainingAmount.coerceAtLeast(0L),
                dueState = DueEngine.state(row.remainingAmount.coerceAtLeast(0L), dueDate, today)
            )
        }

        return items.sortedWith(compareBy({ it.dueDate.toEpochDay() }, { it.kind.name }))
    }

    private fun buildOverview(
        income: List<com.shohan.khatago.data.local.db.rows.MonthTotalRow>,
        expense: List<com.shohan.khatago.data.local.db.rows.MonthTotalRow>,
        payments: List<com.shohan.khatago.data.local.db.rows.MonthTotalRow>,
        range: Pair<Int, Int>
    ): List<MonthFlow> {
        val incomeMap = income.associateBy { it.period }
        val expenseMap = expense.associateBy { it.period }
        val paymentMap = payments.associateBy { it.period }
        return periodsInRange(range).map { period ->
            MonthFlow(
                period = period,
                label = KhataGoTime.formatMonth(LocalDate.of(period / 100, (period % 100).coerceIn(1, 12), 1)),
                income = incomeMap[period]?.total ?: 0L,
                expense = expenseMap[period]?.total ?: 0L,
                payments = paymentMap[period]?.total ?: 0L
            )
        }
    }

    private fun monthPeriods(today: LocalDate, months: Int): Pair<Int, Int> {
        val end = today.year * 100 + today.monthValue
        var year = today.year
        var month = today.monthValue - (months - 1)
        while (month <= 0) {
            month += 12
            year -= 1
        }
        return (year * 100 + month) to end
    }

    private fun periodsInRange(range: Pair<Int, Int>): List<Int> {
        val result = ArrayList<Int>()
        var (year, month) = range.first / 100 to range.first % 100
        val (endYear, endMonth) = range.second / 100 to range.second % 100
        while (year * 100 + month <= endYear * 100 + endMonth) {
            result += year * 100 + month
            month += 1
            if (month > 12) {
                month = 1
                year += 1
            }
        }
        return result
    }

    private companion object {
        const val OBLIGATION_LIMIT = 100
        const val RECENT_LIMIT = 8
    }
}
