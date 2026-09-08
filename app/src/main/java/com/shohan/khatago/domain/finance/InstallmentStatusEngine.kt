package com.shohan.khatago.domain.finance

import com.shohan.khatago.domain.model.DueState
import com.shohan.khatago.domain.model.InstallmentStatus
import java.time.LocalDate

/**
 * Status is always derived from stored data — paid amount, scheduled amount and
 * due date — so it can never go stale and a fully paid installment can never
 * remain "Overdue".
 */
object InstallmentStatusEngine {

    fun status(
        paidAmount: Long,
        scheduledAmount: Long,
        dueDate: LocalDate,
        today: LocalDate = LocalDate.now()
    ): InstallmentStatus = when {
        scheduledAmount <= 0L -> InstallmentStatus.PAID
        paidAmount >= scheduledAmount -> InstallmentStatus.PAID
        today.isAfter(dueDate) -> InstallmentStatus.OVERDUE
        paidAmount > 0L -> InstallmentStatus.PARTIALLY_PAID
        today.isEqual(dueDate) -> InstallmentStatus.DUE_TODAY
        else -> InstallmentStatus.UPCOMING
    }

    fun remaining(scheduledAmount: Long, paidAmount: Long): Long =
        (scheduledAmount - paidAmount).coerceAtLeast(0L)

    fun isSettled(scheduledAmount: Long, paidAmount: Long): Boolean =
        scheduledAmount <= 0L || paidAmount >= scheduledAmount
}

/**
 * Due-date classification shared by installments, personal debts and shop
 * credits that carry an expected date.
 */
object DueEngine {

    /** Anything due within this many days is surfaced as "Due Soon". */
    const val SOON_WINDOW_DAYS = 7L

    fun state(
        remaining: Long,
        dueDate: LocalDate?,
        today: LocalDate = LocalDate.now(),
        soonDays: Long = SOON_WINDOW_DAYS
    ): DueState = when {
        remaining <= 0L -> DueState.SETTLED
        dueDate == null -> DueState.UNSCHEDULED
        dueDate.isBefore(today) -> DueState.OVERDUE
        dueDate.isEqual(today) -> DueState.DUE_TODAY
        !dueDate.isAfter(today.plusDays(soonDays)) -> DueState.DUE_SOON
        else -> DueState.SCHEDULED
    }

    fun isActionable(state: DueState): Boolean =
        state == DueState.OVERDUE || state == DueState.DUE_TODAY || state == DueState.DUE_SOON
}
