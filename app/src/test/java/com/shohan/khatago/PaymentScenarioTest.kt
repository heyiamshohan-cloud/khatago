package com.shohan.khatago

import com.google.common.truth.Truth.assertThat
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.domain.finance.AllocationEngine
import com.shohan.khatago.domain.finance.BalanceEngine
import com.shohan.khatago.domain.finance.PaymentValidator
import com.shohan.khatago.domain.model.Frequency
import com.shohan.khatago.domain.model.InstallmentStatus
import com.shohan.khatago.domain.finance.InstallmentStatusEngine
import com.shohan.khatago.domain.finance.ScheduleGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

/**
 * The payment scenarios the product specification calls out, expressed the way a
 * user would meet them: a 10,000 BDT debt paid off in pieces, an overpayment
 * that must be refused, and the arithmetic that has to survive big numbers,
 * zero, decimals and concurrency.
 *
 * All amounts are minor units (paisa). 10,000 BDT = 1,000,000.
 */
class PaymentScenarioTest {

    private val tenThousand = 1_000_000L   // 10,000.00 BDT
    private val twoThousand = 200_000L     //  2,000.00 BDT
    private val fifteenHundred = 150_000L  //  1,500.00 BDT
    private val fiveHundred = 50_000L      //    500.00 BDT
    private val sixThousand = 600_000L     //  6,000.00 BDT

    // ------------------------------------------------- the spec's own scenario

    @Test
    fun tenThousandDebt_afterTwoThousandFifteenHundredFiveHundred_sixThousandRemains() {
        var paid = 0L

        listOf(twoThousand, fifteenHundred, fiveHundred).forEach { amount ->
            val remaining = BalanceEngine.remaining(tenThousand, paid)
            val check = PaymentValidator.validate(amount, remaining)
            assertThat(check).isEqualTo(PaymentValidator.Check.Valid(amount))
            paid = Money.safeAdd(paid, (check as PaymentValidator.Check.Valid).amount)
        }

        assertThat(paid).isEqualTo(400_000L)                       // 4,000.00
        assertThat(BalanceEngine.remaining(tenThousand, paid)).isEqualTo(sixThousand)
        assertThat(BalanceEngine.paidPercent(tenThousand, paid)).isEqualTo(40)
    }

    @Test
    fun thenFullSettlementLeavesNothingBehind() {
        var paid = 400_000L
        val remaining = BalanceEngine.remaining(tenThousand, paid)
        val check = PaymentValidator.validate(sixThousand, remaining)
        assertThat(check).isEqualTo(PaymentValidator.Check.Valid(sixThousand))
        paid = Money.safeAdd(paid, sixThousand)

        assertThat(BalanceEngine.remaining(tenThousand, paid)).isEqualTo(0L)
        assertThat(BalanceEngine.paidPercent(tenThousand, paid)).isEqualTo(100)
        assertThat(InstallmentStatusEngine.isSettled(tenThousand, paid)).isTrue()
    }

    @Test
    fun overpaymentIsRefusedWithTheExactMessage() {
        val due = 500_000L          //  5,000.00 BDT
        val attempted = 600_000L    //  6,000.00 BDT
        val check = PaymentValidator.validate(attempted, due)
        assertThat(check).isEqualTo(PaymentValidator.Check.Overpayment)
        assertThat(PaymentValidator.message(check))
            .isEqualTo("This payment is higher than the amount due.")
    }

    @Test
    fun aPaymentOfExactlyTheRemainingAmountIsAllowed() {
        val check = PaymentValidator.validate(500_000L, 500_000L)
        assertThat(check).isEqualTo(PaymentValidator.Check.Valid(500_000L))
    }

    @Test
    fun aSettledDebtRefusesAnyFurtherPayment() {
        val check = PaymentValidator.validate(1L, 0L)
        assertThat(check).isEqualTo(PaymentValidator.Check.AlreadySettled)
        assertThat(PaymentValidator.message(check)).isEqualTo("This one is already settled.")
    }

    // ------------------------------------------------------------ cash flow

    @Test
    fun fiftyThousandIncomeMinusThirtyTwoThousandExpense_isEighteenThousand() {
        val income = 5_000_000L   // 50,000.00
        val expense = 3_200_000L  // 32,000.00
        assertThat(BalanceEngine.netCashFlow(income, expense)).isEqualTo(1_800_000L)
        assertThat(Money.format(BalanceEngine.netCashFlow(income, expense))).isEqualTo("৳18,000")
    }

    @Test
    fun netCashFlowCanBeNegative() {
        assertThat(BalanceEngine.netCashFlow(3_200_000L, 5_000_000L)).isEqualTo(-1_800_000L)
    }

    // ------------------------------------------------------------ big numbers

    @Test
    fun tenCroreBDTIsHandledExactly() {
        val tenCrore = 10_000_000_000L           // 10,00,00,000.00 BDT
        assertThat(Money.format(tenCrore)).isEqualTo("৳100,000,000")
        assertThat(Money.toDecimal(tenCrore).toPlainString()).isEqualTo("100000000.00")

        val payments = Money.splitEvenly(tenCrore, 12)
        assertThat(payments.sum()).isEqualTo(tenCrore)

        val halfPaid = tenCrore / 2
        assertThat(BalanceEngine.remaining(tenCrore, halfPaid)).isEqualTo(tenCrore - halfPaid)
        assertThat(PaymentValidator.validate(tenCrore + 1L, tenCrore))
            .isEqualTo(PaymentValidator.Check.Overpayment)
    }

    @Test
    fun tenCroreScheduleKeepsEveryPaisa() {
        val tenCrore = 10_000_000_000L
        val schedule = ScheduleGenerator.generate(
            firstDueDate = LocalDate.of(2026, 1, 5),
            count = 240,
            frequency = Frequency.MONTHLY,
            totalAmount = tenCrore
        )
        assertThat(schedule.sumOf { it.amount }).isEqualTo(tenCrore)
    }

    // ---------------------------------------------------------- zero and edges

    @Test
    fun zeroValuesAreRejectedAsPaymentsButNeverBreakBalances() {
        assertThat(PaymentValidator.validate(0L, 1_000L)).isEqualTo(PaymentValidator.Check.Empty)
        // A blank amount is reported as an empty payment even when the account is
        // settled: "Enter a valid amount." is the actionable message.
        assertThat(PaymentValidator.validate(0L, 0L)).isEqualTo(PaymentValidator.Check.Empty)
        assertThat(PaymentValidator.validate(1L, 0L)).isEqualTo(PaymentValidator.Check.AlreadySettled)
        assertThat(BalanceEngine.remaining(0L, 0L)).isEqualTo(0L)
        assertThat(BalanceEngine.paidPercent(0L, 0L)).isEqualTo(100)
        assertThat(Money.splitEvenly(0L, 3)).containsExactly(0L, 0L, 0L).inOrder()
    }

    @Test
    fun aOnePaisaDebtCanBeSettledExactly() {
        assertThat(PaymentValidator.validate(1L, 1L)).isEqualTo(PaymentValidator.Check.Valid(1L))
        assertThat(BalanceEngine.remaining(1L, 1L)).isEqualTo(0L)
        assertThat(PaymentValidator.validate(2L, 1L)).isEqualTo(PaymentValidator.Check.Overpayment)
    }

    // -------------------------------------------------- decimals and rounding

    @Test
    fun decimalInputIsParsedToExactPaisa() {
        assertThat(Money.parse("1250.75")).isEqualTo(125_075L)
        assertThat(Money.parse("0.01")).isEqualTo(1L)
        assertThat(Money.parse("0.005")).isEqualTo(1L)   // half up
        assertThat(Money.parse("0.004")).isEqualTo(0L)
        assertThat(Money.parse("1,00,000.50")).isEqualTo(10_000_050L)
    }

    @Test
    fun roundingNeverLosesOrInventsMoney() {
        val schedule = ScheduleGenerator.generate(
            firstDueDate = LocalDate.of(2026, 1, 1),
            count = 3,
            frequency = Frequency.MONTHLY,
            totalAmount = 100L
        )
        assertThat(schedule.map { it.amount }).containsExactly(34L, 33L, 33L).inOrder()
        assertThat(schedule.sumOf { it.amount }).isEqualTo(100L)
    }

    @Test
    fun invalidAmountsAreRejectedNotRecorded() {
        listOf(null, "", "  ", "abc", ".", "-5", "1.2.3").forEach { input ->
            assertThat(Money.parse(input)).isNull()
        }
        assertThat(PaymentValidator.validate(Money.parse("abc"), 1_000L))
            .isEqualTo(PaymentValidator.Check.NotANumber)
    }

    // ---------------------------------------------- many, duplicate, concurrent

    @Test
    fun manySmallPaymentsSettleTheDebtExactly() {
        val total = 1_000_000L
        var paid = 0L
        repeat(20) {
            val remaining = BalanceEngine.remaining(total, paid)
            val check = PaymentValidator.validate(50_000L, remaining)
            paid = Money.safeAdd(paid, (check as PaymentValidator.Check.Valid).amount)
        }
        assertThat(paid).isEqualTo(total)
        assertThat(BalanceEngine.remaining(total, paid)).isEqualTo(0L)

        // A duplicate attempt after settlement is refused.
        assertThat(PaymentValidator.validate(50_000L, BalanceEngine.remaining(total, paid)))
            .isEqualTo(PaymentValidator.Check.AlreadySettled)
    }

    @Test
    fun theLastPaymentMayBeSmallerThanTheRest() {
        val total = 1_000_001L
        var paid = 0L
        repeat(333) {
            val check = PaymentValidator.validate(3_000L, BalanceEngine.remaining(total, paid))
            paid = Money.safeAdd(paid, (check as PaymentValidator.Check.Valid).amount)
        }
        val finalRemaining = BalanceEngine.remaining(total, paid)
        val last = PaymentValidator.validate(finalRemaining, finalRemaining)
        assertThat(last).isEqualTo(PaymentValidator.Check.Valid(finalRemaining))
        paid = Money.safeAdd(paid, finalRemaining)
        assertThat(paid).isEqualTo(total)
    }

    @Test
    fun concurrentPaymentAttemptsNeverExceedTheDebt() = runTest {
        val total = 1_000_000L
        var paid = 0L
        val lock = Any()
        var accepted = 0

        val jobs = (1..8).map {
            launch(Dispatchers.Default) {
                repeat(5) {
                    // Read -> validate -> commit as one critical section, which is
                    // what a Room transaction gives a real payment: no attempt can
                    // validate against a balance another attempt has already taken.
                    synchronized(lock) {
                        val remaining = BalanceEngine.remaining(total, paid)
                        val check = PaymentValidator.validate(200_000L, remaining)
                        if (check is PaymentValidator.Check.Valid) {
                            paid = Money.safeAdd(paid, check.amount)
                            accepted++
                        }
                    }
                }
            }
        }
        jobs.forEach { it.join() }

        assertThat(paid).isAtMost(total)
        assertThat(paid).isEqualTo(accepted.toLong() * 200_000L)
        assertThat(BalanceEngine.remaining(total, paid)).isAtLeast(0L)
        assertThat(accepted).isEqualTo(5)   // 5 x 2,000 = 10,000, exactly the debt
    }

    // -------------------------------------------------- allocation: edit/delete

    private fun schedule(count: Int, amount: Long) =
        ScheduleGenerator.generate(LocalDate.of(2026, 1, 10), count, Frequency.MONTHLY, amount)
            .mapIndexed { index, it -> AllocationEngine.ScheduleSlot((index + 1).toLong(), it.amount) }

    @Test
    fun editingAPaymentRedistributesTheSchedule() {
        val slots = schedule(3, 300_000L)               // 3 x 1,000.00
        val before = AllocationEngine.allocate(
            slots,
            listOf(AllocationEngine.PaymentSlot(1L, 0L, 250_000L))
        ).first
        assertThat(before[1L]).isEqualTo(100_000L)
        assertThat(before[2L]).isEqualTo(100_000L)
        assertThat(before[3L]).isEqualTo(50_000L)

        // The same payment edited down to 1,200.00
        val after = AllocationEngine.allocate(
            slots,
            listOf(AllocationEngine.PaymentSlot(1L, 0L, 120_000L))
        ).first
        assertThat(after[1L]).isEqualTo(100_000L)
        assertThat(after[2L]).isEqualTo(20_000L)
        assertThat(after[3L]).isEqualTo(0L)
        assertThat(after.values.sum()).isEqualTo(120_000L)
    }

    @Test
    fun deletingAPaymentReopensTheInstallmentItCovered() {
        val slots = schedule(2, 200_000L)
        val withPayment = AllocationEngine.allocate(
            slots,
            listOf(AllocationEngine.PaymentSlot(1L, 0L, 100_000L))
        ).first
        assertThat(withPayment[1L]).isEqualTo(100_000L)

        val deleted = AllocationEngine.allocate(slots, emptyList()).first
        assertThat(deleted[1L]).isEqualTo(0L)
        assertThat(deleted[2L]).isEqualTo(0L)

        val statusWithPayment = InstallmentStatusEngine.status(
            paidAmount = withPayment[1L] ?: 0L,
            scheduledAmount = 100_000L,
            dueDate = LocalDate.of(2020, 1, 1),
            today = LocalDate.of(2026, 6, 1)
        )
        val statusAfterDelete = InstallmentStatusEngine.status(
            paidAmount = deleted[1L] ?: 0L,
            scheduledAmount = 100_000L,
            dueDate = LocalDate.of(2020, 1, 1),
            today = LocalDate.of(2026, 6, 1)
        )
        assertThat(statusWithPayment).isEqualTo(InstallmentStatus.PAID)
        assertThat(statusAfterDelete).isEqualTo(InstallmentStatus.OVERDUE)
    }

    @Test
    fun aFullyPaidInstallmentIsNeverOverdue() {
        val pastDue = LocalDate.of(2020, 1, 1)
        val today = LocalDate.of(2026, 6, 1)
        val status = InstallmentStatusEngine.status(
            paidAmount = 100_000L,
            scheduledAmount = 100_000L,
            dueDate = pastDue,
            today = today
        )
        assertThat(status).isEqualTo(InstallmentStatus.PAID)
    }
}
