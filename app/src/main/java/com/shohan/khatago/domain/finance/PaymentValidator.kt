package com.shohan.khatago.domain.finance

/**
 * Guards every payment written by KhataGo.
 *
 * A payment may never exceed what is actually due, and a payment of zero is
 * never recorded. The result carries the copy the UI shows, so no error ever
 * leaks a stack trace or a database message to the user.
 */
object PaymentValidator {

    sealed interface Check {
        data class Valid(val amount: Long) : Check
        data object Empty : Check
        data object NotANumber : Check
        data object Overpayment : Check
        data object AlreadySettled : Check
    }

    const val MESSAGE_EMPTY = "Enter a valid amount."
    const val MESSAGE_INVALID = "Enter a valid amount."
    const val MESSAGE_OVERPAYMENT = "This payment is higher than the amount due."
    const val MESSAGE_SETTLED = "This one is already settled."

    fun validate(amountMinor: Long?, remaining: Long): Check = when {
        amountMinor == null -> Check.NotANumber
        amountMinor <= 0L -> Check.Empty
        remaining <= 0L -> Check.AlreadySettled
        amountMinor > remaining -> Check.Overpayment
        else -> Check.Valid(amountMinor)
    }

    fun message(check: Check): String = when (check) {
        is Check.Valid -> ""
        Check.Empty -> MESSAGE_EMPTY
        Check.NotANumber -> MESSAGE_INVALID
        Check.Overpayment -> MESSAGE_OVERPAYMENT
        Check.AlreadySettled -> MESSAGE_SETTLED
    }
}

/**
 * Balance helpers used by every module so the reconciliation rule is identical
 * everywhere:
 *
 *      original + charges − valid payments = remaining
 */
object BalanceEngine {
    fun remaining(totalPayable: Long, totalPaid: Long): Long =
        (totalPayable - totalPaid).coerceAtLeast(0L)

    fun paidPercent(totalPayable: Long, totalPaid: Long): Int {
        if (totalPayable <= 0L) return 100
        val percent = (totalPaid * 100L) / totalPayable
        return percent.coerceIn(0L, 100L).toInt()
    }

    fun netCashFlow(income: Long, expense: Long): Long = income - expense
}
