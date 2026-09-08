package com.shohan.khatago.data.local.db.rows

/**
 * Read models returned directly by Room queries.
 *
 * Aggregates are computed in SQL (indexed subqueries) so the dashboard and the
 * Accounts screen stay fast even with tens of thousands of rows.
 */

data class ShopAccountRow(
    val id: Long,
    val name: String,
    val ownerName: String,
    val phone: String,
    val totalCredit: Long,
    val totalPaid: Long,
    val creditCount: Int,
    val lastActivityEpochDay: Long?
)

data class ShopCreditRow(
    val id: Long,
    val shopId: Long,
    val dateEpochDay: Long,
    val dueDateEpochDay: Long?,
    val totalAmount: Long,
    val notes: String,
    val itemCount: Int
)

data class LoanAccountRow(
    val id: Long,
    val name: String,
    val institution: String,
    val principalAmount: Long,
    val totalPayable: Long,
    val installmentAmount: Long,
    val installmentCount: Int,
    val frequency: String,
    val dateTakenEpochDay: Long,
    val firstDueDateEpochDay: Long,
    val maturityDateEpochDay: Long,
    val scheduledTotal: Long,
    val paidAmount: Long,
    val remainingAmount: Long,
    val remainingCount: Int,
    val overdueCount: Int,
    val nextDueDateEpochDay: Long?,
    val archived: Boolean
)

data class EmiAccountRow(
    val id: Long,
    val productName: String,
    val seller: String,
    val totalPrice: Long,
    val downPayment: Long,
    val totalPayable: Long,
    val installmentAmount: Long,
    val installmentCount: Int,
    val frequency: String,
    val purchaseDateEpochDay: Long,
    val firstDueDateEpochDay: Long,
    val maturityDateEpochDay: Long,
    val scheduledTotal: Long,
    val paidAmount: Long,
    val remainingAmount: Long,
    val remainingCount: Int,
    val overdueCount: Int,
    val nextDueDateEpochDay: Long?,
    val archived: Boolean
)

data class BorrowedRow(
    val id: Long,
    val personId: Long,
    val personName: String,
    val relationship: String,
    val amount: Long,
    val borrowedDateEpochDay: Long,
    val expectedReturnDateEpochDay: Long?,
    val notes: String,
    val paidAmount: Long,
    val archived: Boolean
)

data class LentRow(
    val id: Long,
    val personId: Long,
    val personName: String,
    val relationship: String,
    val amount: Long,
    val lentDateEpochDay: Long,
    val expectedReturnDateEpochDay: Long?,
    val notes: String,
    val receivedAmount: Long,
    val archived: Boolean
)

data class CategoryTotalRow(
    val name: String,
    val total: Long
)

data class MonthTotalRow(
    /** `year * 100 + month`, e.g. 202609. */
    val period: Int,
    val total: Long
)

/**
 * A shop credit that carries a promise-to-pay date, together with everything
 * needed to work out how much of it is still due.
 *
 * Shop payments settle the oldest purchases first (FIFO), so:
 *      covered  = clamp(shopPaid − olderCreditTotal, 0, creditTotal)
 *      remaining = creditTotal − covered
 */
data class ShopDueRow(
    val id: Long,
    val shopId: Long,
    val shopName: String,
    val dateEpochDay: Long,
    val dueDateEpochDay: Long,
    val totalAmount: Long,
    /** Total of this credit plus every older credit at the same shop. */
    val cumulativeCredit: Long,
    /** Everything ever paid to the shop. */
    val shopPaid: Long
)

data class UpcomingObligationRow(
    /** LOAN, EMI, BORROWED, LENT, SHOP */
    val kind: String,
    val refId: Long,
    val title: String,
    val subtitle: String,
    val dueDateEpochDay: Long,
    val amount: Long,
    val remaining: Long
)
