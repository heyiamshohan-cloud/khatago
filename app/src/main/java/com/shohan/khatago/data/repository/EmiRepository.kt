package com.shohan.khatago.data.repository

import androidx.room.withTransaction
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.data.local.db.KhataGoDatabase
import com.shohan.khatago.data.local.db.dao.EmiDao
import com.shohan.khatago.data.local.db.entity.EmiInstallmentEntity
import com.shohan.khatago.data.local.db.entity.EmiPaymentEntity
import com.shohan.khatago.data.local.db.entity.EmiPurchaseEntity
import com.shohan.khatago.data.local.db.entity.TransactionFactory
import com.shohan.khatago.domain.finance.AllocationEngine
import com.shohan.khatago.domain.finance.BalanceEngine
import com.shohan.khatago.domain.finance.DueEngine
import com.shohan.khatago.domain.finance.InstallmentStatusEngine
import com.shohan.khatago.domain.finance.PaymentValidator
import com.shohan.khatago.domain.finance.ScheduleGenerator
import com.shohan.khatago.domain.model.DueState
import com.shohan.khatago.domain.model.EmiAccount
import com.shohan.khatago.domain.model.Frequency
import com.shohan.khatago.domain.model.InstallmentStatus
import com.shohan.khatago.domain.model.InstallmentView
import com.shohan.khatago.domain.model.RelatedType
import com.shohan.khatago.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Everything needed to create or edit an EMI purchase. */
data class EmiInput(
    val id: Long? = null,
    val productName: String = "",
    val seller: String = "",
    val purchaseDate: LocalDate = LocalDate.now(),
    val totalPrice: Long = 0L,
    val downPayment: Long = 0L,
    val totalPayable: Long = 0L,
    val installmentAmount: Long = 0L,
    val installmentCount: Int = 0,
    val frequency: Frequency = Frequency.MONTHLY,
    val firstDueDate: LocalDate = LocalDate.now(),
    val notes: String = ""
)

data class EmiDetail(
    val purchase: EmiPurchaseEntity? = null,
    val installments: List<InstallmentView> = emptyList(),
    val payments: List<EmiPaymentEntity> = emptyList(),
    val financedAmount: Long = 0L,
    val scheduledTotal: Long = 0L,
    val paidTotal: Long = 0L,
    val remaining: Long = 0L,
    val progressPercent: Int = 0,
    val remainingCount: Int = 0,
    val overdueCount: Int = 0,
    val overdueTotal: Long = 0L,
    val dueTodayTotal: Long = 0L,
    val nextDueDate: LocalDate? = null,
    val nextInstallment: InstallmentView? = null,
    val dueState: DueState = DueState.UNSCHEDULED
)

/**
 * Product installments (phone, laptop, refrigerator, furniture, ...).
 *
 * financedAmount = totalPayable − downPayment is always derived, never stored,
 * so changing the down payment can never leave a stale financed amount.
 */
class EmiRepository(
    private val database: KhataGoDatabase,
    private val dao: EmiDao,
    private val ledger: LedgerRepository
) {

    fun observeAccounts(todayEpochDay: Long): Flow<List<EmiAccount>> =
        dao.observeEmiAccounts(todayEpochDay).map { rows -> rows.map { it.toAccount(todayEpochDay) } }

    fun observeEmi(emiId: Long): Flow<EmiPurchaseEntity?> = dao.observeEmi(emiId)

    suspend fun getEmi(emiId: Long): EmiPurchaseEntity? = dao.getEmi(emiId)

    fun observeDetail(emiId: Long, todayEpochDay: Long): Flow<EmiDetail> = combine(
        dao.observeEmi(emiId),
        dao.observeInstallments(emiId),
        dao.observePayments(emiId)
    ) { purchase, installments, payments ->
        buildDetail(purchase, installments, payments, todayEpochDay)
    }

    fun buildDetail(
        purchase: EmiPurchaseEntity?,
        installments: List<EmiInstallmentEntity>,
        payments: List<EmiPaymentEntity>,
        todayEpochDay: Long
    ): EmiDetail {
        val today = LocalDate.ofEpochDay(todayEpochDay)
        val views = installments.map { it.toView(today) }
        val scheduledTotal = installments.sumOf { it.scheduledAmount }
        val paidTotal = installments.sumOf { it.paidAmount }
        val remaining = BalanceEngine.remaining(scheduledTotal, paidTotal)
        val overdue = views.filter { it.status == InstallmentStatus.OVERDUE }
        val dueToday = views.filter { it.status == InstallmentStatus.DUE_TODAY }
        val next = views.firstOrNull { it.status != InstallmentStatus.PAID }
        return EmiDetail(
            purchase = purchase,
            installments = views,
            payments = payments,
            financedAmount = purchase?.let {
                (it.totalPayable - it.downPayment).coerceAtLeast(0L)
            } ?: 0L,
            scheduledTotal = scheduledTotal,
            paidTotal = paidTotal,
            remaining = remaining,
            progressPercent = BalanceEngine.paidPercent(scheduledTotal, paidTotal),
            remainingCount = views.count { it.status != InstallmentStatus.PAID },
            overdueCount = overdue.size,
            overdueTotal = overdue.sumOf { it.remaining },
            dueTodayTotal = dueToday.sumOf { it.remaining },
            nextDueDate = next?.dueDate,
            nextInstallment = next,
            dueState = DueEngine.state(remaining, next?.dueDate, today)
        )
    }

    fun observeInstallments(emiId: Long): Flow<List<EmiInstallmentEntity>> =
        dao.observeInstallments(emiId)

    suspend fun getPayment(paymentId: Long): EmiPaymentEntity? = dao.getPayment(paymentId)

    suspend fun remaining(emiId: Long): Long {
        val installments = dao.getInstallments(emiId)
        return BalanceEngine.remaining(
            installments.sumOf { it.scheduledAmount },
            installments.sumOf { it.paidAmount }
        )
    }

    suspend fun nextUnsettled(emiId: Long): EmiInstallmentEntity? = dao.nextUnsettledInstallment(emiId)

    // ------------------------------------------------------------------ writes

    suspend fun saveEmi(input: EmiInput): Long = database.withTransaction {
        val now = KhataGoTime.nowMillis()
        val maturity = ScheduleGenerator.maturityDate(
            input.firstDueDate,
            input.installmentCount,
            input.frequency
        )
        val entity = EmiPurchaseEntity(
            id = input.id ?: 0L,
            productName = input.productName.trim(),
            seller = input.seller.trim(),
            purchaseDateEpochDay = input.purchaseDate.toEpochDay(),
            totalPrice = input.totalPrice,
            downPayment = input.downPayment,
            totalPayable = input.totalPayable,
            installmentAmount = input.installmentAmount,
            installmentCount = input.installmentCount,
            frequency = input.frequency.name,
            firstDueDateEpochDay = input.firstDueDate.toEpochDay(),
            maturityDateEpochDay = maturity.toEpochDay(),
            notes = input.notes.trim(),
            createdAt = now,
            updatedAt = now
        )

        val rowId: Long
        val existing = input.id?.let { dao.getEmi(it) }
        if (existing == null) {
            rowId = dao.insertEmi(entity)
            dao.insertInstallments(buildSchedule(rowId, input))
        } else {
            rowId = existing.id
            dao.updateEmi(entity.copy(createdAt = existing.createdAt))
            val scheduleChanged =
                existing.totalPayable != input.totalPayable ||
                    existing.installmentCount != input.installmentCount ||
                    existing.frequency != input.frequency.name ||
                    existing.firstDueDateEpochDay != input.firstDueDate.toEpochDay()
            if (scheduleChanged) {
                dao.deleteInstallmentsForEmi(rowId)
                dao.insertInstallments(buildSchedule(rowId, input))
            }
            reallocate(rowId)
        }

        ledger.deleteTransactionByOrigin(TransactionType.EMI_PURCHASE.name, rowId)
        ledger.record(
            TransactionFactory.create(
                type = TransactionType.EMI_PURCHASE,
                amount = input.totalPayable,
                date = input.purchaseDate,
                category = "EMI",
                relatedType = RelatedType.EMI,
                relatedId = rowId,
                originType = TransactionType.EMI_PURCHASE.name,
                originId = rowId,
                description = input.productName.trim().ifBlank { "EMI purchase" },
                notes = input.seller.trim()
            )
        )
        rowId
    }

    suspend fun setArchived(emiId: Long, archived: Boolean) {
        val emi = dao.getEmi(emiId) ?: return
        dao.updateEmi(emi.copy(archived = archived, updatedAt = KhataGoTime.nowMillis()))
    }

    suspend fun deleteEmi(emiId: Long) = database.withTransaction {
        val emi = dao.getEmi(emiId) ?: return@withTransaction
        ledger.deleteTransactionByOrigin(TransactionType.EMI_PURCHASE.name, emiId)
        dao.deleteEmi(emi)
    }

    suspend fun savePayment(
        emiId: Long,
        paymentId: Long?,
        date: LocalDate,
        amount: Long,
        method: String,
        notes: String,
        targetInstallmentId: Long? = null
    ): PaymentResult = database.withTransaction {
        val installments = dao.getInstallments(emiId)
        val scheduled = installments.sumOf { it.scheduledAmount }
        val existingPayments = dao.getPayments(emiId)
        val paidExcludingThis = existingPayments.filter { it.id != paymentId }.sumOf { it.amount }
        val remaining = BalanceEngine.remaining(scheduled, paidExcludingThis)

        when (val check = PaymentValidator.validate(amount, remaining)) {
            is PaymentValidator.Check.Valid -> {
                val entity = EmiPaymentEntity(
                    id = paymentId ?: 0L,
                    emiId = emiId,
                    installmentId = targetInstallmentId,
                    dateEpochDay = date.toEpochDay(),
                    amount = check.amount,
                    method = method,
                    notes = notes.trim(),
                    createdAt = KhataGoTime.nowMillis()
                )
                val rowId = if (paymentId == null) dao.insertPayment(entity) else {
                    dao.updatePayment(entity)
                    paymentId
                }
                reallocate(emiId)

                val product = dao.getEmi(emiId)?.productName.orEmpty()
                ledger.deleteTransactionByOrigin(TransactionType.EMI_PAYMENT.name, rowId)
                ledger.record(
                    TransactionFactory.create(
                        type = TransactionType.EMI_PAYMENT,
                        amount = check.amount,
                        date = date,
                        category = "EMI Payment",
                        relatedType = RelatedType.EMI,
                        relatedId = emiId,
                        originType = TransactionType.EMI_PAYMENT.name,
                        originId = rowId,
                        description = product.ifBlank { "EMI payment" },
                        notes = notes.trim()
                    )
                )
                PaymentResult.Success(rowId)
            }
            else -> PaymentResult.Rejected(PaymentValidator.message(check))
        }
    }

    suspend fun deletePayment(paymentId: Long) = database.withTransaction {
        val payment = dao.getPayment(paymentId) ?: return@withTransaction
        val emiId = payment.emiId
        ledger.deleteTransactionByOrigin(TransactionType.EMI_PAYMENT.name, paymentId)
        dao.deletePayment(payment)
        reallocate(emiId)
    }

    private suspend fun reallocate(emiId: Long) {
        val installments = dao.getInstallments(emiId)
        if (installments.isEmpty()) return
        val payments = dao.getPayments(emiId)
        val (paid, traces) = AllocationEngine.allocate(
            schedule = installments.map { AllocationEngine.ScheduleSlot(it.id, it.scheduledAmount) },
            payments = payments.map {
                AllocationEngine.PaymentSlot(it.id, it.dateEpochDay, it.amount, it.installmentId)
            }
        )
        installments.forEach { installment ->
            val newPaid = paid[installment.id] ?: 0L
            if (newPaid != installment.paidAmount) {
                dao.updateInstallment(installment.copy(paidAmount = newPaid))
            }
        }
        traces.forEach { trace ->
            val payment = payments.firstOrNull { it.id == trace.paymentId } ?: return@forEach
            if (payment.installmentId != trace.primaryInstallmentId) {
                dao.updatePayment(payment.copy(installmentId = trace.primaryInstallmentId))
            }
        }
    }

    private fun buildSchedule(emiId: Long, input: EmiInput): List<EmiInstallmentEntity> {
        val schedule = ScheduleGenerator.generate(
            firstDueDate = input.firstDueDate,
            count = input.installmentCount,
            frequency = input.frequency,
            totalAmount = input.totalPayable
        )
        val now = KhataGoTime.nowMillis()
        return schedule.map { slot ->
            EmiInstallmentEntity(
                emiId = emiId,
                number = slot.number,
                dueDateEpochDay = slot.dueDate.toEpochDay(),
                scheduledAmount = slot.amount,
                paidAmount = 0L,
                createdAt = now
            )
        }
    }
}

private fun com.shohan.khatago.data.local.db.rows.EmiAccountRow.toAccount(todayEpochDay: Long): EmiAccount {
    val today = LocalDate.ofEpochDay(todayEpochDay)
    val remaining = BalanceEngine.remaining(totalPayable, paidAmount)
    return EmiAccount(
        id = id,
        productName = productName,
        seller = seller,
        totalPrice = totalPrice,
        downPayment = downPayment,
        financedAmount = Money.safeSubtract(totalPayable, downPayment).coerceAtLeast(0L),
        totalPayable = totalPayable,
        scheduledTotal = scheduledTotal,
        paidAmount = paidAmount,
        remaining = remaining,
        installmentAmount = installmentAmount,
        installmentCount = installmentCount,
        remainingCount = remainingCount,
        overdueCount = overdueCount,
        nextDueDate = nextDueDateEpochDay?.let { LocalDate.ofEpochDay(it) },
        purchaseDate = LocalDate.ofEpochDay(purchaseDateEpochDay),
        maturityDate = LocalDate.ofEpochDay(maturityDateEpochDay),
        frequency = Frequency.from(frequency),
        progressPercent = BalanceEngine.paidPercent(totalPayable, paidAmount),
        dueState = DueEngine.state(remaining, nextDueDateEpochDay?.let { LocalDate.ofEpochDay(it) }, today),
        archived = archived
    )
}

fun EmiInstallmentEntity.toView(today: LocalDate): InstallmentView {
    val remaining = InstallmentStatusEngine.remaining(scheduledAmount, paidAmount)
    return InstallmentView(
        id = id,
        number = number,
        dueDate = LocalDate.ofEpochDay(dueDateEpochDay),
        scheduledAmount = scheduledAmount,
        paidAmount = paidAmount,
        remaining = remaining,
        status = InstallmentStatusEngine.status(
            paidAmount,
            scheduledAmount,
            LocalDate.ofEpochDay(dueDateEpochDay),
            today
        )
    )
}
