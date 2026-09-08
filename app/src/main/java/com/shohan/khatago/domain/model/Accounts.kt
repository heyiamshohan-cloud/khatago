package com.shohan.khatago.domain.model

import com.shohan.khatago.core.money.Money
import java.time.LocalDate

/** What a person owes and has received back, per account family. */
data class OutstandingBreakdown(
    val shopCredit: Long = 0L,
    val loans: Long = 0L,
    val emi: Long = 0L,
    val borrowed: Long = 0L
) {
    val total: Long
        get() = Money.safeAdd(Money.safeAdd(shopCredit, loans), Money.safeAdd(emi, borrowed))
}

/** Money the user is owed by other people — tracked separately from what they owe. */
data class ReceivableSummary(
    val lent: Long = 0L,
    val received: Long = 0L,
    val outstanding: Long = 0L
)

// --------------------------------------------------------------------- accounts

data class ShopAccount(
    val id: Long,
    val name: String,
    val ownerName: String,
    val phone: String,
    val totalCredit: Long,
    val totalPaid: Long,
    val remaining: Long,
    val creditCount: Int,
    val lastActivity: LocalDate?
)

data class LoanAccount(
    val id: Long,
    val name: String,
    val institution: String,
    val principalAmount: Long,
    val totalPayable: Long,
    val scheduledTotal: Long,
    val paidAmount: Long,
    val remaining: Long,
    val installmentAmount: Long,
    val installmentCount: Int,
    val remainingCount: Int,
    val overdueCount: Int,
    val nextDueDate: LocalDate?,
    val dateTaken: LocalDate,
    val maturityDate: LocalDate,
    val frequency: Frequency,
    val progressPercent: Int,
    val dueState: DueState,
    val archived: Boolean
)

data class EmiAccount(
    val id: Long,
    val productName: String,
    val seller: String,
    val totalPrice: Long,
    val downPayment: Long,
    val financedAmount: Long,
    val totalPayable: Long,
    val scheduledTotal: Long,
    val paidAmount: Long,
    val remaining: Long,
    val installmentAmount: Long,
    val installmentCount: Int,
    val remainingCount: Int,
    val overdueCount: Int,
    val nextDueDate: LocalDate?,
    val purchaseDate: LocalDate,
    val maturityDate: LocalDate,
    val frequency: Frequency,
    val progressPercent: Int,
    val dueState: DueState,
    val archived: Boolean
)

data class BorrowedAccount(
    val id: Long,
    val personId: Long,
    val personName: String,
    val relationship: String,
    val amount: Long,
    val paidAmount: Long,
    val remaining: Long,
    val borrowedDate: LocalDate,
    val expectedReturnDate: LocalDate?,
    val notes: String,
    val progressPercent: Int,
    val dueState: DueState,
    val archived: Boolean
)

data class LentAccount(
    val id: Long,
    val personId: Long,
    val personName: String,
    val relationship: String,
    val amount: Long,
    val receivedAmount: Long,
    val remaining: Long,
    val lentDate: LocalDate,
    val expectedReturnDate: LocalDate?,
    val notes: String,
    val progressPercent: Int,
    val dueState: DueState,
    val archived: Boolean
)

// ------------------------------------------------------------------ instalments

data class InstallmentView(
    val id: Long,
    val number: Int,
    val dueDate: LocalDate,
    val scheduledAmount: Long,
    val paidAmount: Long,
    val remaining: Long,
    val status: InstallmentStatus
)

// ---------------------------------------------------------------------- ledger

data class LedgerEntry(
    val id: Long,
    val timestamp: Long,
    val date: LocalDate,
    val amount: Long,
    val type: TransactionType,
    val category: String,
    val relatedType: RelatedType,
    val relatedId: Long,
    val description: String,
    val notes: String
) {
    /** Sign shown in the activity timeline. Never used for totals. */
    val signedAmount: Long
        get() = when {
            type.countsAsIncome -> amount
            type == TransactionType.PERSONAL_RETURN -> amount
            type == TransactionType.EXPENSE -> -amount
            type.isCashOut -> -amount
            else -> amount
        }
}

enum class RelatedType { SHOP, LOAN, EMI, PERSON, INCOME, EXPENSE, NONE }

// ------------------------------------------------------------------- dashboard

data class UpcomingItem(
    val kind: AccountKind,
    val refId: Long,
    val title: String,
    val subtitle: String,
    val dueDate: LocalDate,
    val remaining: Long,
    val dueState: DueState
)

data class MonthFlow(
    val period: Int,
    val label: String,
    val income: Long,
    val expense: Long,
    val payments: Long
) {
    val net: Long get() = income - expense
}

data class TodaySnapshot(
    val income: Long = 0L,
    val expense: Long = 0L,
    val payments: Long = 0L,
    val dueToday: Long = 0L,
    val dueTodayCount: Int = 0
)

data class DashboardSnapshot(
    val userName: String = "",
    val hasAnyData: Boolean = false,
    val outstanding: OutstandingBreakdown = OutstandingBreakdown(),
    val receivable: ReceivableSummary = ReceivableSummary(),
    val overdueCount: Int = 0,
    val overdueTotal: Long = 0L,
    val today: TodaySnapshot = TodaySnapshot(),
    val upcoming: List<UpcomingItem> = emptyList(),
    val overview: List<MonthFlow> = emptyList(),
    val recentTransactions: List<LedgerEntry> = emptyList(),
    val insights: List<Insight> = emptyList()
)

// --------------------------------------------------------------------- reports

data class DateRange(val start: LocalDate, val end: LocalDate, val label: String)

data class CategorySlice(val name: String, val total: Long)

data class DebtSlice(val kind: AccountKind, val outstanding: Long)

data class ReportData(
    val range: DateRange,
    val income: Long = 0L,
    val expense: Long = 0L,
    val payments: Long = 0L,
    val netCashFlow: Long = 0L,
    val outstanding: OutstandingBreakdown = OutstandingBreakdown(),
    val receivable: ReceivableSummary = ReceivableSummary(),
    val incomeByCategory: List<CategorySlice> = emptyList(),
    val expenseByCategory: List<CategorySlice> = emptyList(),
    val monthly: List<MonthFlow> = emptyList(),
    val debtDistribution: List<DebtSlice> = emptyList(),
    val transactions: List<LedgerEntry> = emptyList(),
    val overdueCount: Int = 0,
    val overdueTotal: Long = 0L,
    val upcomingCount: Int = 0,
    val upcomingTotal: Long = 0L
) {
    val hasData: Boolean
        get() = income != 0L || expense != 0L || payments != 0L || transactions.isNotEmpty()
}

// -------------------------------------------------------------------- insights

enum class InsightTone { POSITIVE, NEGATIVE, NEUTRAL }

data class Insight(
    val id: String,
    val title: String,
    val message: String,
    val tone: InsightTone
)
