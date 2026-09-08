package com.shohan.khatago.domain.finance

import com.shohan.khatago.core.money.Money

/**
 * Payment allocation for scheduled debt (loans and EMIs).
 *
 * Rules:
 *  1. Payments are applied in date order.
 *  2. A payment aimed at a specific installment lands there first.
 *  3. Anything left over spills into the earliest installment that is still open,
 *     so an over-sized payment keeps working down the schedule instead of being
 *     rejected while money is still owed.
 *
 * Example — installments of 5,000, 5,000, 5,000 with a single payment of 12,000:
 *
 *      #1 paid 5,000 (Paid), #2 paid 5,000 (Paid), #3 paid 2,000 (Partially Paid)
 *
 * The engine is pure, so editing or deleting a payment simply re-derives every
 * installment's paid amount from the full payment list — balances can never drift.
 */
object AllocationEngine {

    data class ScheduleSlot(val id: Long, val scheduledAmount: Long)

    data class PaymentSlot(
        val id: Long,
        val dateEpochDay: Long,
        val amount: Long,
        val targetInstallmentId: Long? = null
    )

    /** The installment a payment first landed on — stored so payments stay traceable. */
    data class AllocatedPayment(val paymentId: Long, val primaryInstallmentId: Long?)

    /**
     * @return paid amount per installment id, plus the installment each payment
     * first landed on.
     */
    fun allocate(
        schedule: List<ScheduleSlot>,
        payments: List<PaymentSlot>
    ): Pair<Map<Long, Long>, List<AllocatedPayment>> {
        val paid = LinkedHashMap<Long, Long>()
        schedule.forEach { paid[it.id] = 0L }
        val traces = ArrayList<AllocatedPayment>(payments.size)

        val ordered = payments.sortedWith(compareBy({ it.dateEpochDay }, { it.id }))

        for (payment in ordered) {
            var left = payment.amount
            var primary: Long? = null

            payment.targetInstallmentId?.let { targetId ->
                val slot = schedule.firstOrNull { it.id == targetId }
                if (slot != null) {
                    val open = (slot.scheduledAmount - (paid[slot.id] ?: 0L)).coerceAtLeast(0L)
                    if (open > 0L) {
                        val applied = minOf(open, left)
                        paid[slot.id] = Money.safeAdd(paid[slot.id] ?: 0L, applied)
                        left -= applied
                        primary = slot.id
                    }
                }
            }

            for (slot in schedule) {
                if (left <= 0L) break
                val open = (slot.scheduledAmount - (paid[slot.id] ?: 0L)).coerceAtLeast(0L)
                if (open <= 0L) continue
                val applied = minOf(open, left)
                paid[slot.id] = Money.safeAdd(paid[slot.id] ?: 0L, applied)
                left -= applied
                if (primary == null) primary = slot.id
            }

            traces += AllocatedPayment(payment.id, primary)
        }

        return paid to traces
    }

    /**
     * Shop credit allocation: payments settle the oldest purchases first (FIFO).
     * @return the amount of each purchase that payments have covered.
     */
    fun allocateCredits(
        creditsOrderedOldestFirst: List<Pair<Long, Long>>,
        totalPaid: Long
    ): Map<Long, Long> {
        var remainingPayment = totalPaid.coerceAtLeast(0L)
        val covered = LinkedHashMap<Long, Long>()
        for ((creditId, total) in creditsOrderedOldestFirst) {
            val applied = minOf(total.coerceAtLeast(0L), remainingPayment)
            covered[creditId] = applied
            remainingPayment -= applied
        }
        return covered
    }

    /** Remaining for one credit once every older credit at that shop is settled. */
    fun creditRemaining(creditTotal: Long, olderCreditTotal: Long, shopPaid: Long): Long {
        val covered = (shopPaid - olderCreditTotal).coerceIn(0L, creditTotal.coerceAtLeast(0L))
        return (creditTotal - covered).coerceAtLeast(0L)
    }
}
