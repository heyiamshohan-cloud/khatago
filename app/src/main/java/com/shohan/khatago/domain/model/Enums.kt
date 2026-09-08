package com.shohan.khatago.domain.model

/**
 * How often a schedule repeats. Used by both loans and EMIs.
 */
enum class Frequency(val label: String, val shortLabel: String) {
    WEEKLY("Weekly", "Wk"),
    MONTHLY("Monthly", "Mo");

    companion object {
        fun from(value: String?): Frequency =
            entries.firstOrNull { it.name == value } ?: MONTHLY
    }
}

/**
 * Status of a single installment. Always DERIVED from stored data
 * (scheduled amount, paid amount, due date, today) — never persisted.
 */
enum class InstallmentStatus(val label: String) {
    PAID("Paid"),
    PARTIALLY_PAID("Partially Paid"),
    OVERDUE("Overdue"),
    DUE_TODAY("Due Today"),
    UPCOMING("Upcoming")
}

/**
 * Where a due date sits relative to today, for any obligation.
 */
enum class DueState(val label: String) {
    SETTLED("Settled"),
    OVERDUE("Overdue"),
    DUE_TODAY("Due Today"),
    DUE_SOON("Due Soon"),
    SCHEDULED("Scheduled"),
    UNSCHEDULED("No due date")
}

/**
 * A single entry in the central transaction ledger.
 *
 * Debt creating entries (shop credit, loan taken, EMI purchase, borrowing,
 * lending) are recorded so the activity timeline is complete, but they are
 * deliberately NOT counted as income or expense — see FINANCIAL_LOGIC.md.
 */
enum class TransactionType(val label: String) {
    SHOP_CREDIT("Shop Credit"),
    SHOP_PAYMENT("Shop Payment"),
    LOAN("Loan"),
    LOAN_PAYMENT("Loan Payment"),
    EMI_PURCHASE("EMI Purchase"),
    EMI_PAYMENT("EMI Payment"),
    PERSONAL_BORROWING("Borrowed"),
    PERSONAL_REPAYMENT("Repayment"),
    PERSONAL_LENDING("Lent"),
    PERSONAL_RETURN("Received"),
    INCOME("Income"),
    EXPENSE("Expense");

    /** Money coming in to the user's pocket. */
    val isCashIn: Boolean
        get() = this == INCOME || this == PERSONAL_RETURN

    /** Money leaving the user's pocket. */
    val isCashOut: Boolean
        get() = this == EXPENSE || this == SHOP_PAYMENT || this == LOAN_PAYMENT ||
            this == EMI_PAYMENT || this == PERSONAL_REPAYMENT || this == PERSONAL_LENDING

    /** Counts towards the Income total in dashboard and reports. */
    val countsAsIncome: Boolean get() = this == INCOME

    /** Counts towards the Expense total in dashboard and reports. */
    val countsAsExpense: Boolean get() = this == EXPENSE

    /** A payment that reduces a liability, tracked separately from expenses. */
    val countsAsPayment: Boolean
        get() = this == SHOP_PAYMENT || this == LOAN_PAYMENT ||
            this == EMI_PAYMENT || this == PERSONAL_REPAYMENT

    companion object {
        fun from(value: String?): TransactionType =
            entries.firstOrNull { it.name == value } ?: EXPENSE
    }
}

/** Payment methods offered across every payment form. */
enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    BANK("Bank Transfer"),
    MOBILE_BANKING("Mobile Banking"),
    CARD("Card"),
    OTHER("Other");

    companion object {
        fun from(value: String?): PaymentMethod =
            entries.firstOrNull { it.name == value } ?: CASH
    }
}

/** Which side of a personal record the user is on. */
enum class DebtDirection { BORROWED, LENT }

/** Account families shown on the Accounts screen. */
enum class AccountKind(val label: String) {
    SHOP_CREDIT("Shop Credit"),
    LOAN("Loans"),
    EMI("EMI"),
    PERSONAL_DEBT("Personal Debt")
}
