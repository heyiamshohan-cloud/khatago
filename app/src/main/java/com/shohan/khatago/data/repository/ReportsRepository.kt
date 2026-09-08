package com.shohan.khatago.data.repository

import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.core.time.TodayProvider
import com.shohan.khatago.data.local.db.rows.CategoryTotalRow
import com.shohan.khatago.data.local.db.rows.MonthTotalRow
import com.shohan.khatago.domain.model.AccountKind
import com.shohan.khatago.domain.model.BorrowedAccount
import com.shohan.khatago.domain.model.CategorySlice
import com.shohan.khatago.domain.model.DateRange
import com.shohan.khatago.domain.model.DebtSlice
import com.shohan.khatago.domain.model.DueState
import com.shohan.khatago.domain.model.EmiAccount
import com.shohan.khatago.domain.model.LedgerEntry
import com.shohan.khatago.domain.model.LoanAccount
import com.shohan.khatago.domain.model.LentAccount
import com.shohan.khatago.domain.model.MonthFlow
import com.shohan.khatago.domain.model.OutstandingBreakdown
import com.shohan.khatago.domain.model.ReceivableSummary
import com.shohan.khatago.domain.model.ReportData
import com.shohan.khatago.domain.model.ShopAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate

/** The report periods offered on the Reports screen. */
enum class ReportRangeKind(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom")
}

/**
 * Reports recompute every figure from stored rows for the selected period.
 * They use exactly the same accounting semantics as the dashboard:
 *
 *      income − expense = net cash flow
 *      payments are never counted as income
 *      borrowed money is never counted as income
 */
class ReportsRepository(
    private val shopRepository: ShopRepository,
    private val loanRepository: LoanRepository,
    private val emiRepository: EmiRepository,
    private val personalRepository: PersonalRepository,
    private val ledgerRepository: LedgerRepository,
    private val todayProvider: TodayProvider
) {

    fun rangeFor(kind: ReportRangeKind, today: LocalDate = KhataGoTime.today()): DateRange = when (kind) {
        ReportRangeKind.TODAY -> DateRange(today, today, "Today")
        ReportRangeKind.THIS_WEEK -> DateRange(
            KhataGoTime.startOfWeek(today),
            KhataGoTime.endOfWeek(today),
            "This Week"
        )
        ReportRangeKind.THIS_MONTH -> DateRange(
            KhataGoTime.startOfMonth(today),
            KhataGoTime.endOfMonth(today),
            KhataGoTime.formatMonth(today)
        )
        ReportRangeKind.LAST_MONTH -> {
            val (start, end) = KhataGoTime.lastMonthRange(today)
            DateRange(start, end, KhataGoTime.formatMonth(start))
        }
        ReportRangeKind.THIS_YEAR -> DateRange(
            KhataGoTime.startOfYear(today),
            KhataGoTime.endOfYear(today),
            "${today.year}"
        )
        ReportRangeKind.CUSTOM -> DateRange(today, today, "Custom")
    }

    fun observeReport(range: DateRange): Flow<ReportData> =
        todayProvider.epochDay.flatMapLatest { todayEpochDay ->
            val today = LocalDate.ofEpochDay(todayEpochDay)
            val startDay = range.start.toEpochDay()
            val endDay = range.end.toEpochDay()
            val fromPeriod = periodOf(range.start)
            val toPeriod = periodOf(range.end)

            combineAll(
                listOf(
                    ledgerRepository.observeIncomeTotalFlow(startDay, endDay),
                    ledgerRepository.observeExpenseTotalFlow(startDay, endDay),
                    ledgerRepository.observePaymentsTotalFlow(startDay, endDay),
                    ledgerRepository.observeIncomeByCategory(startDay, endDay),
                    ledgerRepository.observeExpenseByCategory(startDay, endDay),
                    ledgerRepository.observeIncomeByMonth(fromPeriod, toPeriod),
                    ledgerRepository.observeExpenseByMonth(fromPeriod, toPeriod),
                    ledgerRepository.observePaymentsByMonth(fromPeriod, toPeriod),
                    ledgerRepository.observeTransactionsBetween(startDay, endDay, 500),
                    shopRepository.observeAccounts(),
                    loanRepository.observeAccounts(todayEpochDay),
                    emiRepository.observeAccounts(todayEpochDay),
                    personalRepository.observeBorrowed(),
                    personalRepository.observeLent()
                )
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val income = values[0] as Long
                @Suppress("UNCHECKED_CAST")
                val expense = values[1] as Long
                @Suppress("UNCHECKED_CAST")
                val payments = values[2] as Long
                val incomeCategories = values[3] as List<CategoryTotalRow>
                val expenseCategories = values[4] as List<CategoryTotalRow>
                val incomeMonths = values[5] as List<MonthTotalRow>
                val expenseMonths = values[6] as List<MonthTotalRow>
                val paymentMonths = values[7] as List<MonthTotalRow>
                val transactions = values[8] as List<LedgerEntry>
                val shops = values[9] as List<ShopAccount>
                val loans = values[10] as List<LoanAccount>
                val emis = values[11] as List<EmiAccount>
                val borrowed = values[12] as List<BorrowedAccount>
                val lent = values[13] as List<LentAccount>

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

                val overdueAccounts = borrowed.filter { it.dueState == DueState.OVERDUE } +
                    loans.filter { it.overdueCount > 0 } +
                    emis.filter { it.overdueCount > 0 }

                ReportData(
                    range = range,
                    income = income,
                    expense = expense,
                    payments = payments,
                    netCashFlow = income - expense,
                    outstanding = outstanding,
                    receivable = receivable,
                    incomeByCategory = incomeCategories.map { CategorySlice(it.name, it.total) },
                    expenseByCategory = expenseCategories.map { CategorySlice(it.name, it.total) },
                    monthly = buildMonthly(incomeMonths, expenseMonths, paymentMonths, fromPeriod, toPeriod),
                    debtDistribution = listOf(
                        DebtSlice(AccountKind.SHOP_CREDIT, outstanding.shopCredit),
                        DebtSlice(AccountKind.LOAN, outstanding.loans),
                        DebtSlice(AccountKind.EMI, outstanding.emi),
                        DebtSlice(AccountKind.PERSONAL_DEBT, outstanding.borrowed)
                    ).filter { it.outstanding > 0L },
                    transactions = transactions,
                    overdueCount = overdueAccounts.size,
                    overdueTotal = overdueAccounts.sumOf {
                        when (it) {
                            is BorrowedAccount -> it.remaining
                            is LoanAccount -> it.remaining
                            is EmiAccount -> it.remaining
                            else -> 0L
                        }
                    },
                    upcomingCount = loans.count { it.dueState == DueState.DUE_SOON } +
                        emis.count { it.dueState == DueState.DUE_SOON } +
                        borrowed.count { it.dueState == DueState.DUE_SOON },
                    upcomingTotal = (loans.sumOf { if (it.dueState == DueState.DUE_SOON) it.remaining else 0L } +
                        emis.sumOf { if (it.dueState == DueState.DUE_SOON) it.remaining else 0L } +
                        borrowed.sumOf { if (it.dueState == DueState.DUE_SOON) it.remaining else 0L })
                )
            }
        }

    private fun buildMonthly(
        income: List<MonthTotalRow>,
        expense: List<MonthTotalRow>,
        payments: List<MonthTotalRow>,
        fromPeriod: Int,
        toPeriod: Int
    ): List<MonthFlow> {
        val incomeMap = income.associateBy { it.period }
        val expenseMap = expense.associateBy { it.period }
        val paymentMap = payments.associateBy { it.period }
        return periodsInRange(fromPeriod, toPeriod).map { period ->
            MonthFlow(
                period = period,
                label = KhataGoTime.formatMonth(
                    LocalDate.of(period / 100, (period % 100).coerceIn(1, 12), 1)
                ),
                income = incomeMap[period]?.total ?: 0L,
                expense = expenseMap[period]?.total ?: 0L,
                payments = paymentMap[period]?.total ?: 0L
            )
        }
    }

    private fun periodOf(date: LocalDate): Int = date.year * 100 + date.monthValue

    private fun periodsInRange(from: Int, to: Int): List<Int> {
        val result = ArrayList<Int>()
        var year = from / 100
        var month = from % 100
        val endYear = to / 100
        val endMonth = to % 100
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
}
