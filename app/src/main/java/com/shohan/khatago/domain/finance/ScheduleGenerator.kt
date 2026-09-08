package com.shohan.khatago.domain.finance

import com.shohan.khatago.core.money.Money
import com.shohan.khatago.domain.model.Frequency
import java.time.LocalDate

/** One generated installment before it is persisted. */
data class ScheduledInstallment(
    val number: Int,
    val dueDate: LocalDate,
    val amount: Long
)

/**
 * Builds real installment schedules for loans and EMIs.
 *
 * Date rules:
 *  - Weekly schedules advance 7 days at a time from the first due date.
 *  - Monthly schedules advance whole months from the anchor date, so month
 *    lengths, month ends, year changes and leap years are handled by java.time:
 *    31 Jan -> 28 Feb (29 in a leap year) -> 31 Mar, with no drift.
 *
 * Amount rules:
 *  - The total is split so the parts always sum back to exactly the total; the
 *    first `total % count` parts take one extra paisa.
 */
object ScheduleGenerator {

    fun generate(
        firstDueDate: LocalDate,
        count: Int,
        frequency: Frequency,
        totalAmount: Long
    ): List<ScheduledInstallment> {
        if (count <= 0) return emptyList()
        val amounts = Money.splitEvenly(totalAmount, count)
        return amounts.mapIndexed { index, amount ->
            ScheduledInstallment(
                number = index + 1,
                dueDate = advance(firstDueDate, frequency, index),
                amount = amount
            )
        }
    }

    fun advance(anchor: LocalDate, frequency: Frequency, periods: Int): LocalDate =
        when (frequency) {
            Frequency.WEEKLY -> anchor.plusWeeks(periods.toLong())
            Frequency.MONTHLY -> anchor.plusMonths(periods.toLong())
        }

    /** Last due date of the schedule. */
    fun maturityDate(firstDueDate: LocalDate, count: Int, frequency: Frequency): LocalDate =
        if (count <= 0) firstDueDate else advance(firstDueDate, frequency, count - 1)

    /** Suggested installment amount when the user has not typed one. */
    fun evenInstallmentAmount(totalAmount: Long, count: Int): Long =
        if (count <= 0) 0L else Money.splitEvenly(totalAmount, count).firstOrNull() ?: 0L
}
