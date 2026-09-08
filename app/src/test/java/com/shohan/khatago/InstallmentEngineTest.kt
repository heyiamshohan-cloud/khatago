package com.shohan.khatago

import com.google.common.truth.Truth.assertThat
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.domain.finance.AllocationEngine
import com.shohan.khatago.domain.finance.BalanceEngine
import com.shohan.khatago.domain.finance.DueEngine
import com.shohan.khatago.domain.finance.InstallmentStatusEngine
import com.shohan.khatago.domain.finance.PaymentValidator
import com.shohan.khatago.domain.finance.ScheduleGenerator
import com.shohan.khatago.domain.model.DueState
import com.shohan.khatago.domain.model.Frequency
import com.shohan.khatago.domain.model.InstallmentStatus
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * The installment engine: dates that survive month ends, leap years and year
 * changes, and statuses that stay honest as payments come in.
 */
class InstallmentEngineTest {

    private val today = LocalDate.of(2026, 6, 1)

    // -------------------------------------------------------------- schedules

    @Test
    fun monthlyScheduleAdvancesWholeMonths() {
        val schedule = ScheduleGenerator.generate(
            firstDueDate = LocalDate.of(2026, 3, 15),
            count = 4,
            frequency = Frequency.MONTHLY,
            totalAmount = 400_000L
        )
        assertThat(schedule.map { it.dueDate }).containsExactly(
            LocalDate.of(2026, 3, 15),
            LocalDate.of(2026, 4, 15),
            LocalDate.of(2026, 5, 15),
            LocalDate.of(2026, 6, 15)
        ).inOrder()
        assertThat(schedule.sumOf { it.amount }).isEqualTo(400_000L)
    }

    @Test
    fun monthlyScheduleClampsMonthEnds() {
        val schedule = ScheduleGenerator.generate(
            firstDueDate = LocalDate.of(2026, 1, 31),
            count = 5,
            frequency = Frequency.MONTHLY,
            totalAmount = 500L
        )
        assertThat(schedule.map { it.dueDate }).containsExactly(
            LocalDate.of(2026, 1, 31),
            LocalDate.of(2026, 2, 28),
            LocalDate.of(2026, 3, 31),
            LocalDate.of(2026, 4, 30),
            LocalDate.of(2026, 5, 31)
        ).inOrder()
    }

    @Test
    fun monthlyScheduleHandlesLeapYears() {
        val leap = ScheduleGenerator.generate(
            LocalDate.of(2028, 1, 31), 2, Frequency.MONTHLY, 200L
        )
        assertThat(leap[1].dueDate).isEqualTo(LocalDate.of(2028, 2, 29))

        val nonLeapCentury = ScheduleGenerator.generate(
            LocalDate.of(2100, 1, 31), 2, Frequency.MONTHLY, 200L
        )
        assertThat(nonLeapCentury[1].dueDate).isEqualTo(LocalDate.of(2100, 2, 28))
    }

    @Test
    fun monthlyScheduleCrossesYearBoundaries() {
        val schedule = ScheduleGenerator.generate(
            firstDueDate = LocalDate.of(2026, 11, 30),
            count = 4,
            frequency = Frequency.MONTHLY,
            totalAmount = 400L
        )
        assertThat(schedule.map { it.dueDate }).containsExactly(
            LocalDate.of(2026, 11, 30),
            LocalDate.of(2026, 12, 30),
            LocalDate.of(2027, 1, 30),
            LocalDate.of(2027, 2, 28)
        ).inOrder()
    }

    @Test
    fun weeklyScheduleAdvancesSevenDays() {
        val schedule = ScheduleGenerator.generate(
            firstDueDate = LocalDate.of(2026, 12, 28),
            count = 3,
            frequency = Frequency.WEEKLY,
            totalAmount = 300L
        )
        assertThat(schedule.map { it.dueDate }).containsExactly(
            LocalDate.of(2026, 12, 28),
            LocalDate.of(2027, 1, 4),
            LocalDate.of(2027, 1, 11)
        ).inOrder()
    }

    @Test
    fun longSchedulesNeverDrift() {
        val schedule = ScheduleGenerator.generate(
            firstDueDate = LocalDate.of(2026, 1, 31),
            count = 60,
            frequency = Frequency.MONTHLY,
            totalAmount = 6_000_000L
        )
        assertThat(schedule).hasSize(60)
        assertThat(schedule.sumOf { it.amount }).isEqualTo(6_000_000L)
        assertThat(schedule.last().dueDate).isEqualTo(LocalDate.of(2030, 12, 31))
        // Every date is either month-end or the same day-of-month as the anchor.
        assertThat(schedule.all { it.dueDate.dayOfMonth == 31 || it.dueDate.dayOfMonth == 30 || it.dueDate.dayOfMonth == 28 || it.dueDate.dayOfMonth == 29 }).isTrue()
    }

    @Test
    fun scheduleGenerationIsDeterministic() {
        val first = ScheduleGenerator.generate(LocalDate.of(2026, 1, 31), 12, Frequency.MONTHLY, 1_200_000L)
        val second = ScheduleGenerator.generate(LocalDate.of(2026, 1, 31), 12, Frequency.MONTHLY, 1_200_000L)
        assertThat(first).isEqualTo(second)
    }

    @Test
    fun anEmptyScheduleIsProducedForANonPositiveCount() {
        assertThat(ScheduleGenerator.generate(LocalDate.of(2026, 1, 1), 0, Frequency.MONTHLY, 100L)).isEmpty()
        assertThat(ScheduleGenerator.generate(LocalDate.of(2026, 1, 1), -5, Frequency.MONTHLY, 100L)).isEmpty()
    }

    // ------------------------------------------------- payment states on a loan

    private fun slots(count: Int = 3, amount: Long = 100_000L): List<AllocationEngine.ScheduleSlot> =
        (1..count).map { AllocationEngine.ScheduleSlot(it.toLong(), amount) }

    @Test
    fun aPartialPaymentLeavesAnInstallmentPartiallyPaid() {
        val schedule = slots()
        val (paid, _) = AllocationEngine.allocate(
            schedule,
            listOf(AllocationEngine.PaymentSlot(1L, 0L, 40_000L))
        )
        val status = InstallmentStatusEngine.status(
            paidAmount = paid[1L] ?: 0L,
            scheduledAmount = 100_000L,
            dueDate = today.plusMonths(1),
            today = today
        )
        assertThat(status).isEqualTo(InstallmentStatus.PARTIALLY_PAID)
        assertThat(InstallmentStatusEngine.remaining(100_000L, 40_000L)).isEqualTo(60_000L)
    }

    @Test
    fun severalPaymentsFillInstallmentsInOrder() {
        val schedule = slots(3, 100_000L)
        val payments = listOf(
            AllocationEngine.PaymentSlot(1L, 0L, 60_000L),
            AllocationEngine.PaymentSlot(2L, 1L, 60_000L),
            AllocationEngine.PaymentSlot(3L, 2L, 60_000L),
            AllocationEngine.PaymentSlot(4L, 3L, 60_000L),
            AllocationEngine.PaymentSlot(5L, 4L, 60_000L)
        )
        val (paid, _) = AllocationEngine.allocate(schedule, payments)
        assertThat(paid.values.sum()).isEqualTo(300_000L)
        paid.values.forEach { assertThat(it).isEqualTo(100_000L) }
        assertThat(InstallmentStatusEngine.isSettled(100_000L, paid[3L] ?: 0L)).isTrue()
    }

    @Test
    fun overpaymentBeyondTheScheduleIsRefused() {
        val schedule = slots(2, 100_000L)
        val scheduled = schedule.sumOf { it.scheduledAmount }
        val alreadyPaid = 150_000L
        val remaining = BalanceEngine.remaining(scheduled, alreadyPaid)
        assertThat(PaymentValidator.validate(60_000L, remaining))
            .isEqualTo(PaymentValidator.Check.Overpayment)
        assertThat(PaymentValidator.validate(50_000L, remaining))
            .isEqualTo(PaymentValidator.Check.Valid(50_000L))
    }

    // ----------------------------------------------------------- due statuses

    @Test
    fun dueStatusesCoverOverdueTodayUpcomingAndPaid() {
        val overdue = LocalDate.of(2026, 5, 20)
        val dueToday = today
        val upcoming = LocalDate.of(2026, 6, 20)

        assertThat(
            InstallmentStatusEngine.status(0L, 100_000L, overdue, today)
        ).isEqualTo(InstallmentStatus.OVERDUE)
        assertThat(
            InstallmentStatusEngine.status(0L, 100_000L, dueToday, today)
        ).isEqualTo(InstallmentStatus.DUE_TODAY)
        assertThat(
            InstallmentStatusEngine.status(0L, 100_000L, upcoming, today)
        ).isEqualTo(InstallmentStatus.UPCOMING)
        assertThat(
            InstallmentStatusEngine.status(100_000L, 100_000L, overdue, today)
        ).isEqualTo(InstallmentStatus.PAID)
    }

    @Test
    fun aFullyPaidInstallmentNeverBecomesOverdueEvenYearsLater() {
        val status = InstallmentStatusEngine.status(
            paidAmount = 100_000L,
            scheduledAmount = 100_000L,
            dueDate = LocalDate.of(2019, 1, 1),
            today = LocalDate.of(2026, 6, 1)
        )
        assertThat(status).isEqualTo(InstallmentStatus.PAID)
        assertThat(DueEngine.state(
            remaining = BalanceEngine.remaining(100_000L, 100_000L),
            dueDate = LocalDate.of(2019, 1, 1),
            today = today
        )).isEqualTo(DueState.SETTLED)
    }

    @Test
    fun dueEngineWindowSeparatesSoonFromScheduled() {
        assertThat(DueEngine.state(100L, today.plusDays(7), today)).isEqualTo(DueState.DUE_SOON)
        assertThat(DueEngine.state(100L, today.plusDays(8), today)).isEqualTo(DueState.SCHEDULED)
    }

    // --------------------------------------------------------------- "today"

    @Test
    fun todayUsesTheDeviceLocalDate() {
        val expected = LocalDate.now(ZoneId.systemDefault())
        assertThat(KhataGoTime.today()).isEqualTo(expected)
    }

    @Test
    fun maturityDateMatchesTheFinalInstallment() {
        val schedule = ScheduleGenerator.generate(
            LocalDate.of(2026, 1, 31), 24, Frequency.MONTHLY, 2_400_000L
        )
        assertThat(
            ScheduleGenerator.maturityDate(LocalDate.of(2026, 1, 31), 24, Frequency.MONTHLY)
        ).isEqualTo(schedule.last().dueDate)
    }

    @Test
    fun installmentAmountsAreSuggestedWithoutLosingMoney() {
        val total = 1_000_001L
        val parts = Money.splitEvenly(total, 3)
        assertThat(ScheduleGenerator.evenInstallmentAmount(total, 3)).isEqualTo(parts.first())
        assertThat(parts.sum()).isEqualTo(total)
    }
}
