package com.shohan.khatago

import com.google.common.truth.Truth.assertThat
import com.shohan.khatago.core.money.Money
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

/**
 * The financial core: schedules, payment allocation, validation, balances and
 * due states. These are the rules every module shares, so they are tested
 * without Android.
 */
class FinancialEngineTest {

    // ------------------------------------------------------------- schedules

    @Test
    fun schedule_generatesOneRowPerInstallment() {
        val schedule = ScheduleGenerator.generate(
            firstDueDate = LocalDate.of(2026, 1, 31),
            count = 6,
            frequency = Frequency.MONTHLY,
            totalAmount = 600_000L
        )
        assertThat(schedule).hasSize(6)
        assertThat(schedule.map { it.number }).containsExactly(1, 2, 3, 4, 5, 6).inOrder()
        assertThat(schedule.sumOf { it.amount }).isEqualTo(600_000L)
    }

    @Test
    fun schedule_clampsMonthEndsWithoutDrifting() {
        val schedule = ScheduleGenerator.generate(
            firstDueDate = LocalDate.of(2026, 1, 31),
            count = 4,
            frequency = Frequency.MONTHLY,
            totalAmount = 400L
        )
        assertThat(schedule.map { it.dueDate }).containsExactly(
            LocalDate.of(2026, 1, 31),
            LocalDate.of(2026, 2, 28),
            LocalDate.of(2026, 3, 31),
            LocalDate.of(2026, 4, 30)
        ).inOrder()
    }

    @Test
    fun schedule_handlesLeapYears() {
        val schedule = ScheduleGenerator.generate(
            firstDueDate = LocalDate.of(2028, 1, 31),
            count = 2,
            frequency = Frequency.MONTHLY,
            totalAmount = 200L
        )
        assertThat(schedule[1].dueDate).isEqualTo(LocalDate.of(2028, 2, 29))
    }

    @Test
    fun schedule_advancesWeekly() {
        val schedule = ScheduleGenerator.generate(
            firstDueDate = LocalDate.of(2026, 3, 1),
            count = 3,
            frequency = Frequency.WEEKLY,
            totalAmount = 300L
        )
        assertThat(schedule.map { it.dueDate }).containsExactly(
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 8),
            LocalDate.of(2026, 3, 15)
        ).inOrder()
    }

    @Test
    fun schedule_sumsBackToTotalWithOddPaisa() {
        val schedule = ScheduleGenerator.generate(
            firstDueDate = LocalDate.of(2026, 1, 1),
            count = 7,
            frequency = Frequency.MONTHLY,
            totalAmount = 100_003L
        )
        assertThat(schedule.sumOf { it.amount }).isEqualTo(100_003L)
        assertThat(schedule.first().amount).isEqualTo(14_286L)
    }

    @Test
    fun maturityDate_isTheLastDueDate() {
        assertThat(
            ScheduleGenerator.maturityDate(LocalDate.of(2026, 1, 10), 12, Frequency.MONTHLY)
        ).isEqualTo(LocalDate.of(2026, 12, 10))
    }

    // ------------------------------------------------------------ allocation

    private fun slots(vararg pairs: Pair<Long, Long>) =
        pairs.map { AllocationEngine.ScheduleSlot(it.first, it.second) }

    @Test
    fun allocate_fillsTheEarliestOpenInstallmentFirst() {
        val schedule = slots(1L to 100L, 2L to 100L, 3L to 100L)
        val payments = listOf(
            AllocationEngine.PaymentSlot(10L, 0L, 150L)
        )
        val (paid, traces) = AllocationEngine.allocate(schedule, payments)
        assertThat(paid[1L]).isEqualTo(100L)
        assertThat(paid[2L]).isEqualTo(50L)
        assertThat(paid[3L]).isEqualTo(0L)
        assertThat(traces.single().primaryInstallmentId).isEqualTo(1L)
    }

    @Test
    fun allocate_honoursTheTargetedInstallmentBeforeSpillingOver() {
        val schedule = slots(1L to 100L, 2L to 100L)
        val payments = listOf(
            AllocationEngine.PaymentSlot(11L, 0L, 160L, targetInstallmentId = 2L)
        )
        val (paid, traces) = AllocationEngine.allocate(schedule, payments)
        assertThat(paid[1L]).isEqualTo(60L)
        assertThat(paid[2L]).isEqualTo(100L)
        assertThat(traces.single().primaryInstallmentId).isEqualTo(2L)
    }

    @Test
    fun allocate_neverOverpaysAnInstallment() {
        val schedule = slots(1L to 100L)
        val payments = listOf(
            AllocationEngine.PaymentSlot(1L, 0L, 60L),
            AllocationEngine.PaymentSlot(2L, 1L, 60L)
        )
        val (paid, _) = AllocationEngine.allocate(schedule, payments)
        assertThat(paid[1L]).isEqualTo(100L)
    }

    @Test
    fun allocate_appliesPaymentsInDateOrderNotInsertOrder() {
        val schedule = slots(1L to 100L, 2L to 100L)
        val payments = listOf(
            AllocationEngine.PaymentSlot(2L, 10L, 100L),
            AllocationEngine.PaymentSlot(1L, 5L, 100L)
        )
        val (paid, traces) = AllocationEngine.allocate(schedule, payments)
        assertThat(traces.map { it.paymentId }).containsExactly(1L, 2L).inOrder()
        assertThat(paid[1L]).isEqualTo(100L)
        assertThat(paid[2L]).isEqualTo(100L)
    }

    @Test
    fun allocateCredits_settlesTheOldestPurchasesFirst() {
        val covered = AllocationEngine.allocateCredits(
            creditsOrderedOldestFirst = listOf(1L to 100L, 2L to 100L, 3L to 100L),
            totalPaid = 250L
        )
        assertThat(covered[1L]).isEqualTo(100L)
        assertThat(covered[2L]).isEqualTo(100L)
        assertThat(covered[3L]).isEqualTo(50L)
    }

    @Test
    fun creditRemaining_onlyCountsWhatIsLeftAfterOlderPurchases() {
        assertThat(AllocationEngine.creditRemaining(100L, 0L, 40L)).isEqualTo(60L)
        assertThat(AllocationEngine.creditRemaining(100L, 100L, 40L)).isEqualTo(100L)
        assertThat(AllocationEngine.creditRemaining(100L, 100L, 250L)).isEqualTo(0L)
    }

    // ------------------------------------------------------------- validation

    @Test
    fun validate_rejectsEmptyInvalidSettledAndOverpayments() {
        assertThat(PaymentValidator.validate(null, 100L)).isEqualTo(PaymentValidator.Check.NotANumber)
        assertThat(PaymentValidator.validate(0L, 100L)).isEqualTo(PaymentValidator.Check.Empty)
        assertThat(PaymentValidator.validate(-1L, 100L)).isEqualTo(PaymentValidator.Check.Empty)
        assertThat(PaymentValidator.validate(10L, 0L)).isEqualTo(PaymentValidator.Check.AlreadySettled)
        assertThat(PaymentValidator.validate(101L, 100L)).isEqualTo(PaymentValidator.Check.Overpayment)
    }

    @Test
    fun validate_acceptsPartialAndFullPayments() {
        assertThat(PaymentValidator.validate(1L, 100L))
            .isEqualTo(PaymentValidator.Check.Valid(1L))
        assertThat(PaymentValidator.validate(100L, 100L))
            .isEqualTo(PaymentValidator.Check.Valid(100L))
    }

    @Test
    fun validate_messagesAreUserFacing() {
        assertThat(PaymentValidator.message(PaymentValidator.Check.Overpayment))
            .isEqualTo(PaymentValidator.MESSAGE_OVERPAYMENT)
        assertThat(PaymentValidator.message(PaymentValidator.Check.Valid(1L))).isEmpty()
    }

    // --------------------------------------------------------------- balances

    @Test
    fun remaining_neverGoesNegative() {
        assertThat(BalanceEngine.remaining(100L, 250L)).isEqualTo(0L)
        assertThat(BalanceEngine.remaining(100L, 25L)).isEqualTo(75L)
    }

    @Test
    fun paidPercent_isClamped() {
        assertThat(BalanceEngine.paidPercent(200L, 50L)).isEqualTo(25)
        assertThat(BalanceEngine.paidPercent(200L, 500L)).isEqualTo(100)
        assertThat(BalanceEngine.paidPercent(0L, 0L)).isEqualTo(100)
    }

    @Test
    fun netCashFlow_subtractsExpenseFromIncome() {
        assertThat(BalanceEngine.netCashFlow(1_000L, 250L)).isEqualTo(750L)
        assertThat(BalanceEngine.netCashFlow(250L, 1_000L)).isEqualTo(-750L)
    }

    // ------------------------------------------------------------- due states

    private val today = LocalDate.of(2026, 6, 1)

    @Test
    fun dueState_classifiesEveryCase() {
        assertThat(DueEngine.state(100L, null, today)).isEqualTo(DueState.UNSCHEDULED)
        assertThat(DueEngine.state(0L, today, today)).isEqualTo(DueState.SETTLED)
        assertThat(DueEngine.state(100L, LocalDate.of(2026, 5, 31), today)).isEqualTo(DueState.OVERDUE)
        assertThat(DueEngine.state(100L, today, today)).isEqualTo(DueState.DUE_TODAY)
        assertThat(DueEngine.state(100L, LocalDate.of(2026, 6, 5), today)).isEqualTo(DueState.DUE_SOON)
        assertThat(DueEngine.state(100L, LocalDate.of(2026, 6, 9), today)).isEqualTo(DueState.SCHEDULED)
    }

    @Test
    fun installmentStatus_isDerivedFromStoredData() {
        val due = LocalDate.of(2026, 6, 10)
        assertThat(InstallmentStatusEngine.status(0L, 100L, due, today))
            .isEqualTo(InstallmentStatus.UPCOMING)
        assertThat(InstallmentStatusEngine.status(40L, 100L, due, today))
            .isEqualTo(InstallmentStatus.PARTIALLY_PAID)
        assertThat(InstallmentStatusEngine.status(100L, 100L, due, today))
            .isEqualTo(InstallmentStatus.PAID)
        assertThat(InstallmentStatusEngine.status(40L, 100L, LocalDate.of(2026, 5, 1), today))
            .isEqualTo(InstallmentStatus.OVERDUE)
        assertThat(InstallmentStatusEngine.status(0L, 100L, today, today))
            .isEqualTo(InstallmentStatus.DUE_TODAY)
    }

    @Test
    fun installmentPaidInFullIsNeverOverdue() {
        val past = LocalDate.of(2020, 1, 1)
        assertThat(InstallmentStatusEngine.status(100L, 100L, past, today))
            .isEqualTo(InstallmentStatus.PAID)
        assertThat(InstallmentStatusEngine.isSettled(100L, 100L)).isTrue()
    }

    @Test
    fun money_andEngines_agreeOnScheduleTotals() {
        val total = 1_234_567L
        val schedule = ScheduleGenerator.generate(LocalDate.of(2026, 1, 5), 9, Frequency.MONTHLY, total)
        assertThat(schedule.sumOf { it.amount }).isEqualTo(total)
        assertThat(schedule.sumOf { it.amount }).isEqualTo(Money.splitEvenly(total, 9).sum())
    }
}
