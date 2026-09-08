package com.shohan.khatago.data.repository

import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.data.local.db.KhataGoDatabase
import com.shohan.khatago.data.local.db.dao.LoanDao
import com.shohan.khatago.data.local.db.entity.LoanEntity
import com.shohan.khatago.data.local.db.entity.LoanInstallmentEntity
import com.shohan.khatago.data.local.db.entity.LoanPaymentEntity
import com.shohan.khatago.data.local.db.entity.TransactionFactory
import com.shohan.khatago.domain.finance.AllocationEngine
import com.shohan.khatago.domain.finance.BalanceEngine
import com.shohan.khatago.domain.finance.DueEngine
import com.shohan.khatago.domain.finance.InstallmentStatusEngine
import com.shohan.khatago.domain.finance.PaymentValidator
import com.shohan.khatago.domain.finance.ScheduleGenerator
import com.shohan.khatago.domain.model.DueState
import com.shohan.khatago.domain.model.Frequency
import com.shohan.khatago.domain.model.InstallmentStatus
import com.shohan.khatago.domain.model.InstallmentView
import com.shohan.khatago.domain.model.LoanAccount
import com.shohan.khatago.domain.model.RelatedType
import com.shohan.khatago.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Everything needed to create or edit a loan. */
data class LoanInput(
    val id: Long? = null,
    val name: String = "",
    val institution: String = "",
    val principalAmount: Long = 0L,
    val processingFee: Long = 0L,
    val interestRateBps: Long = 0L,
    val dateTaken: LocalDate = LocalDate.now(),
    val totalPayable: Long = 0L,
    val installmentAmount: Long = 0L,
    val installmentCount: Int = 0,
    val frequency: Frequency = Frequency.MONTHLY,
    val firstDueDate: LocalDate = LocalDate.now(),
    val notes: String = ""
)

data class LoanDetail(
    val loan: LoanEntity? = null,
    val installments: List<InstallmentView> = emptyList(),
    val payments: List<LoanPaymentEntity> = emptyList(),
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
 * Loans with real installment schedules.
 *
 * A loan's schedule rows are the source of truth for what is owed; paid amounts
 * are re-derived from the payment list by [AllocationEngine] after every change,
 * so editing or deleting a payment can never leave a stale balance behind.
 */
class LoanRepository(
    private val database: KhataGoDatabase,
    private val dao: LoanDao,
    private val ledger: LedgerRepository
) {

    fun observeAccounts(todayEpochDay: Long): Flow<List<LoanAccount>> =
        dao.observeLoanAccounts(todayEpochDay).map { rows -> rows.map { it.toAccount(todayEpochDay) } }

    fun observeLoan(loanId: Long): Flow<LoanEntity?> = dao.observeLoan(loanId)

    suspend fun getLoan(loanId: Long): LoanEntity? = dao.getLoan(loanId)

    fun observeDetail(loanId: Long, todayEpochDay: Long): Flow<LoanDetail> = combine(
        dao.observeLoan(loanId),
        dao.observeInstallments(loanId),
        dao.observePayments(loanId)
    ) { loan, installments, payments ->
        buildDetail(loan, installments, payments, todayEpochDay)
    }

    fun buildDetail(
        loan: LoanEntity?,
        installments: List<LoanInstallmentEntity>,
        payments: List<LoanPaymentEntity>,
        todayEpochDay: Long
    ): LoanDetail {
        val today = LocalDate.ofEpochDay(todayEpochDay)
        val views = installments.map { it.toView(today) }
        val scheduledTotal = installments.sumOf { it.scheduledAmount }
        val paidTotal = installments.sumOf { it.paidAmount }
        val remaining = BalanceEngine.remaining(scheduledTotal, paidTotal)
        val overdue = views.filter { it.status == InstallmentStatus.OVERDUE }
        val dueToday = views.filter { it.status == InstallmentStatus.DUE_TODAY }
        val next = views.firstOrNull { it.status != InstallmentStatus.PAID }
        return LoanDetail(
            loan = loan,
            installments = views,
            payments = payments,
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

    fun observeInstallments(loanId: Long): Flow<List<LoanInstallmentEntity>> =
        dao.observeInstallments(loanId)

    suspend fun getInstallment(installmentId: Long): LoanInstallmentEntity? =
        dao.getInstallment(installmentId)

    suspend fun getPayment(paymentId: Long): LoanPaymentEntity? = dao.getPayment(paymentId)

    suspend fun remaining(loanId: Long): Long {
        val installments = dao.getInstallments(loanId)
        return BalanceEngine.remaining(
            installments.sumOf { it.scheduledAmount },
            installments.sumOf { it.paidAmount }
        )
    }

    /** Oldest unsettled installment — the default target for a new payment. */
    suspend fun nextUnsettled(loanId: Long): LoanInstallmentEntity? =
        dao.nextUnsettledInstallment(loanId)

    // ------------------------------------------------------------------ writes

    suspend fun saveLoan(input: LoanInput): Long = database.withTransaction {
        val now = KhataGoTime.nowMillis()
        val maturity = ScheduleGenerator.maturityDate(
            input.firstDueDate,
            input.installmentCount,
            input.frequency
        )
        val entity = LoanEntity(
            id = input.id ?: 0L,
            name = input.name.trim(),
            institution = input.institution.trim(),
            principalAmount = input.principalAmount,
            processingFee = input.processingFee,
            interestRateBps = input.interestRateBps,
            dateTakenEpochDay = input.dateTaken.toEpochDay(),
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
        val existing = input.id?.let { dao.getLoan(it) }
        if (existing == null) {
            rowId = dao.insertLoan(entity)
            dao.insertInstallments(buildSchedule(rowId, input))
        } else {
            rowId = existing.id
            dao.updateLoan(entity.copy(createdAt = existing.createdAt))
            val scheduleChanged =
                existing.totalPayable != input.totalPayable ||
                    existing.installmentCount != input.installmentCount ||
                    existing.frequency != input.frequency.name ||
                    existing.firstDueDateEpochDay != input.firstDueDate.toEpochDay()
            if (scheduleChanged) {
                // Schedule rows are rebuilt, but every payment is kept and then
                // re-applied, so the amounts the user actually paid never move.
                dao.deleteInstallmentsForLoan(rowId)
                dao.insertInstallments(buildSchedule(rowId, input))
            }
            reallocate(rowId)
        }

        ledger.deleteTransactionByOrigin(TransactionType.LOAN.name, rowId)
        ledger.record(
            TransactionFactory.create(
                type = TransactionType.LOAN,
                amount = input.principalAmount,
                date = input.dateTaken,
                category = "Loan",
                relatedType = RelatedType.LOAN,
                relatedId = rowId,
                originType = TransactionType.LOAN.name,
                originId = rowId,
                description = input.name.trim().ifBlank { "Loan" },
                notes = input.institution.trim()
            )
        )
        rowId
    }

    suspend fun setArchived(loanId: Long, archived: Boolean) {
        val loan = dao.getLoan(loanId) ?: return
        dao.updateLoan(loan.copy(archived = archived, updatedAt = KhataGoTime.nowMillis()))
    }

    suspend fun deleteLoan(loanId: Long) = database.withTransaction {
        val loan = dao.getLoan(loanId) ?: return@withTransaction
        ledger.deleteTransactionByOrigin(TransactionType.LOAN.name, loanId)
        dao.deleteLoan(loan)
    }

    /** Records a payment and re-applies every payment to the schedule. */
    suspend fun savePayment(
        loanId: Long,
        paymentId: Long?,
        date: LocalDate,
        amount: Long,
        method: String,
        notes: String,
        targetInstallmentId: Long? = null
    ): PaymentResult = database.withTransaction {
        val installments = dao.getInstallments(loanId)
        val scheduled = installments.sumOf { it.scheduledAmount }

        val existingPayments = dao.getPayments(loanId)
        val paidExcludingThis = existingPayments
            .filter { it.id != paymentId }
            .sumOf { it.amount }
        val remaining = BalanceEngine.remaining(scheduled, paidExcludingThis)

        when (val check = PaymentValidator.validate(amount, remaining)) {
            is PaymentValidator.Check.Valid -> {
                val entity = LoanPaymentEntity(
                    id = paymentId ?: 0L,
                    loanId = loanId,
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
                reallocate(loanId)

                val loanName = dao.getLoan(loanId)?.name.orEmpty()
                ledger.deleteTransactionByOrigin(TransactionType.LOAN_PAYMENT.name, rowId)
                ledger.record(
                    TransactionFactory.create(
                        type = TransactionType.LOAN_PAYMENT,
                        amount = check.amount,
                        date = date,
                        category = "Loan Payment",
                        relatedType = RelatedType.LOAN,
                        relatedId = loanId,
                        originType = TransactionType.LOAN_PAYMENT.name,
                        originId = rowId,
                        description = loanName.ifBlank { "Loan payment" },
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
        val loanId = payment.loanId
        ledger.deleteTransactionByOrigin(TransactionType.LOAN_PAYMENT.name, paymentId)
        dao.deletePayment(payment)
        reallocate(loanId)
    }

    /** Re-derives every installment's paid amount from the stored payments. */
    private suspend fun reallocate(loanId: Long) {
        val installments = dao.getInstallments(loanId)
        if (installments.isEmpty()) return
        val payments = dao.getPayments(loanId)
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

    private fun buildSchedule(loanId: Long, input: LoanInput): List<LoanInstallmentEntity> {
        val schedule = ScheduleGenerator.generate(
            firstDueDate = input.firstDueDate,
            count = input.installmentCount,
            frequency = input.frequency,
            totalAmount = input.totalPayable
        )
        val now = KhataGoTime.nowMillis()
        return schedule.map { slot ->
            LoanInstallmentEntity(
                loanId = loanId,
                number = slot.number,
                dueDateEpochDay = slot.dueDate.toEpochDay(),
                scheduledAmount = slot.amount,
                paidAmount = 0L,
                createdAt = now
            )
        }
    }

    fun observeUpcoming(horizonEpochDay: Long, limit: Int) =
        dao.upcomingInstallments(horizonEpochDay, limit)
}

private fun com.shohan.khatago.data.local.db.rows.LoanAccountRow.toAccount(todayEpochDay: Long): LoanAccount {
    val today = LocalDate.ofEpochDay(todayEpochDay)
    val remaining = BalanceEngine.remaining(totalPayable, paidAmount)
    return LoanAccount(
        id = id,
        name = name,
        institution = institution,
        principalAmount = principalAmount,
        totalPayable = totalPayable,
        scheduledTotal = scheduledTotal,
        paidAmount = paidAmount,
        remaining = remaining,
        installmentAmount = installmentAmount,
        installmentCount = installmentCount,
        remainingCount = remainingCount,
        overdueCount = overdueCount,
        nextDueDate = nextDueDateEpochDay?.let { LocalDate.ofEpochDay(it) },
        dateTaken = LocalDate.ofEpochDay(dateTakenEpochDay),
        maturityDate = LocalDate.ofEpochDay(maturityDateEpochDay),
        frequency = Frequency.from(frequency),
        progressPercent = BalanceEngine.paidPercent(totalPayable, paidAmount),
        dueState = DueEngine.state(remaining, nextDueDateEpochDay?.let { LocalDate.ofEpochDay(it) }, today),
        archived = archived
    )
}

fun LoanInstallmentEntity.toView(today: LocalDate): InstallmentView {
    val remaining = InstallmentStatusEngine.remaining(scheduledAmount, paidAmount)
    return InstallmentView(
        id = id,
        number = number,
        dueDate = LocalDate.ofEpochDay(dueDateEpochDay),
        scheduledAmount = scheduledAmount,
        paidAmount = paidAmount,
        remaining = remaining,
        status = InstallmentStatusEngine.status(paidAmount, scheduledAmount, LocalDate.ofEpochDay(dueDateEpochDay), today)
    )
}
