package com.shohan.khatago.data.repository

import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.data.local.db.KhataGoDatabase
import com.shohan.khatago.data.local.db.dao.PersonalDao
import com.shohan.khatago.data.local.db.entity.PersonEntity
import com.shohan.khatago.data.local.db.entity.PersonalDebtEntity
import com.shohan.khatago.data.local.db.entity.PersonalLendingEntity
import com.shohan.khatago.data.local.db.entity.PersonalRepaymentEntity
import com.shohan.khatago.data.local.db.entity.PersonalReturnEntity
import com.shohan.khatago.data.local.db.entity.TransactionFactory
import com.shohan.khatago.domain.finance.BalanceEngine
import com.shohan.khatago.domain.finance.DueEngine
import com.shohan.khatago.domain.finance.PaymentValidator
import com.shohan.khatago.domain.model.BorrowedAccount
import com.shohan.khatago.domain.model.DueState
import com.shohan.khatago.domain.model.LentAccount
import com.shohan.khatago.domain.model.RelatedType
import com.shohan.khatago.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Money the user borrowed: money they must pay back. */
data class BorrowedInput(
    val id: Long? = null,
    val personId: Long = 0L,
    val amount: Long = 0L,
    val borrowedDate: LocalDate = LocalDate.now(),
    val expectedReturnDate: LocalDate? = null,
    val notes: String = ""
)

/** Money the user lent: money they expect back. */
data class LentInput(
    val id: Long? = null,
    val personId: Long = 0L,
    val amount: Long = 0L,
    val lentDate: LocalDate = LocalDate.now(),
    val expectedReturnDate: LocalDate? = null,
    val notes: String = ""
)

data class BorrowedDetail(
    val debt: PersonalDebtEntity? = null,
    val person: PersonEntity? = null,
    val repayments: List<PersonalRepaymentEntity> = emptyList(),
    val paidTotal: Long = 0L,
    val remaining: Long = 0L,
    val progressPercent: Int = 0,
    val dueState: DueState = DueState.UNSCHEDULED
)

data class LentDetail(
    val lending: PersonalLendingEntity? = null,
    val person: PersonEntity? = null,
    val returns: List<PersonalReturnEntity> = emptyList(),
    val receivedTotal: Long = 0L,
    val remaining: Long = 0L,
    val progressPercent: Int = 0,
    val dueState: DueState = DueState.UNSCHEDULED
)

/**
 * Personal debt in both directions:
 *
 *  - Borrowed  -> money the user took from someone (a payable)
 *  - Lent      -> money the user gave to someone (a receivable)
 *
 * Repayments and returns support partial and multiple payments, and are always
 * validated so the balance can never go negative.
 */
class PersonalRepository(
    private val database: KhataGoDatabase,
    private val dao: PersonalDao,
    private val ledger: LedgerRepository
) {

    // ------------------------------------------------------------------ people

    fun observePeople(): Flow<List<PersonEntity>> = dao.observePeople()

    suspend fun getPerson(personId: Long): PersonEntity? = dao.getPerson(personId)

    suspend fun savePerson(
        id: Long?,
        name: String,
        relationship: String,
        phone: String,
        notes: String
    ): Long {
        val now = KhataGoTime.nowMillis()
        val entity = PersonEntity(
            id = id ?: 0L,
            name = name.trim(),
            relationship = relationship.trim(),
            phone = phone.trim(),
            notes = notes.trim(),
            createdAt = now,
            updatedAt = now
        )
        return if (id == null) dao.insertPerson(entity) else {
            dao.updatePerson(entity)
            id
        }
    }

    // ---------------------------------------------------------------- borrowed

    fun observeBorrowed(): Flow<List<BorrowedAccount>> =
        dao.observeBorrowed().map { rows -> rows.map { it.toAccount() } }

    fun observeDebt(debtId: Long) = dao.observeDebt(debtId)

    suspend fun getDebt(debtId: Long): PersonalDebtEntity? = dao.getDebt(debtId)

    fun observeBorrowedDetail(debtId: Long): Flow<BorrowedDetail> = combineIfAvailable(
        dao.observeDebt(debtId),
        dao.observeRepayments(debtId)
    ) { debt, repayments ->
        val paid = repayments.sumOf { it.amount }
        val remaining = BalanceEngine.remaining(debt?.amount ?: 0L, paid)
        BorrowedDetail(
            debt = debt,
            repayments = repayments,
            paidTotal = paid,
            remaining = remaining,
            progressPercent = BalanceEngine.paidPercent(debt?.amount ?: 0L, paid),
            dueState = DueEngine.state(
                remaining,
                debt?.expectedReturnDateEpochDay?.let { LocalDate.ofEpochDay(it) }
            )
        )
    }

    suspend fun saveBorrowed(input: BorrowedInput): Long = database.withTransaction {
        val now = KhataGoTime.nowMillis()
        val entity = PersonalDebtEntity(
            id = input.id ?: 0L,
            personId = input.personId,
            amount = input.amount,
            borrowedDateEpochDay = input.borrowedDate.toEpochDay(),
            expectedReturnDateEpochDay = input.expectedReturnDate?.toEpochDay(),
            notes = input.notes.trim(),
            createdAt = now,
            updatedAt = now
        )
        val rowId = if (input.id == null) dao.insertDebt(entity) else {
            dao.updateDebt(entity)
            input.id
        }
        val personName = dao.getPerson(input.personId)?.name.orEmpty()
        ledger.deleteTransactionByOrigin(TransactionType.PERSONAL_BORROWING.name, rowId)
        ledger.record(
            TransactionFactory.create(
                type = TransactionType.PERSONAL_BORROWING,
                amount = input.amount,
                date = input.borrowedDate,
                category = "Borrowed",
                relatedType = RelatedType.PERSON,
                relatedId = input.personId,
                originType = TransactionType.PERSONAL_BORROWING.name,
                originId = rowId,
                description = personName.ifBlank { "Borrowed" },
                notes = input.notes.trim()
            )
        )
        rowId
    }

    suspend fun deleteDebt(debtId: Long) = database.withTransaction {
        val debt = dao.getDebt(debtId) ?: return@withTransaction
        ledger.deleteTransactionByOrigin(TransactionType.PERSONAL_BORROWING.name, debtId)
        dao.deleteDebt(debt)
    }

    suspend fun saveRepayment(
        debtId: Long,
        repaymentId: Long?,
        date: LocalDate,
        amount: Long,
        method: String,
        notes: String
    ): PaymentResult = database.withTransaction {
        val debt = dao.getDebt(debtId) ?: return@withTransaction PaymentResult.Rejected("We couldn't find that record.")
        val existing = dao.getRepayments(debtId)
        val paidExcludingThis = existing.filter { it.id != repaymentId }.sumOf { it.amount }
        val remaining = BalanceEngine.remaining(debt.amount, paidExcludingThis)

        when (val check = PaymentValidator.validate(amount, remaining)) {
            is PaymentValidator.Check.Valid -> {
                val entity = PersonalRepaymentEntity(
                    id = repaymentId ?: 0L,
                    debtId = debtId,
                    dateEpochDay = date.toEpochDay(),
                    amount = check.amount,
                    method = method,
                    notes = notes.trim(),
                    createdAt = KhataGoTime.nowMillis()
                )
                val rowId = if (repaymentId == null) dao.insertRepayment(entity) else {
                    dao.updateRepayment(entity)
                    repaymentId
                }
                val personName = dao.getPerson(debt.personId)?.name.orEmpty()
                ledger.deleteTransactionByOrigin(TransactionType.PERSONAL_REPAYMENT.name, rowId)
                ledger.record(
                    TransactionFactory.create(
                        type = TransactionType.PERSONAL_REPAYMENT,
                        amount = check.amount,
                        date = date,
                        category = "Repayment",
                        relatedType = RelatedType.PERSON,
                        relatedId = debt.personId,
                        originType = TransactionType.PERSONAL_REPAYMENT.name,
                        originId = rowId,
                        description = personName.ifBlank { "Repayment" },
                        notes = notes.trim()
                    )
                )
                PaymentResult.Success(rowId)
            }
            else -> PaymentResult.Rejected(PaymentValidator.message(check))
        }
    }

    suspend fun deleteRepayment(repaymentId: Long) = database.withTransaction {
        val repayment = dao.getRepayment(repaymentId) ?: return@withTransaction
        ledger.deleteTransactionByOrigin(TransactionType.PERSONAL_REPAYMENT.name, repaymentId)
        dao.deleteRepayment(repayment)
    }

    // -------------------------------------------------------------------- lent

    fun observeLent(): Flow<List<LentAccount>> =
        dao.observeLent().map { rows -> rows.map { it.toAccount() } }

    suspend fun getLending(lendingId: Long): PersonalLendingEntity? = dao.getLending(lendingId)

    fun observeLentDetail(lendingId: Long): Flow<LentDetail> = combineIfAvailable(
        dao.observeLending(lendingId),
        dao.observeReturns(lendingId)
    ) { lending, returns ->
        val received = returns.sumOf { it.amount }
        val remaining = BalanceEngine.remaining(lending?.amount ?: 0L, received)
        LentDetail(
            lending = lending,
            returns = returns,
            receivedTotal = received,
            remaining = remaining,
            progressPercent = BalanceEngine.paidPercent(lending?.amount ?: 0L, received),
            dueState = DueEngine.state(
                remaining,
                lending?.expectedReturnDateEpochDay?.let { LocalDate.ofEpochDay(it) }
            )
        )
    }

    suspend fun saveLent(input: LentInput): Long = database.withTransaction {
        val now = KhataGoTime.nowMillis()
        val entity = PersonalLendingEntity(
            id = input.id ?: 0L,
            personId = input.personId,
            amount = input.amount,
            lentDateEpochDay = input.lentDate.toEpochDay(),
            expectedReturnDateEpochDay = input.expectedReturnDate?.toEpochDay(),
            notes = input.notes.trim(),
            createdAt = now,
            updatedAt = now
        )
        val rowId = if (input.id == null) dao.insertLending(entity) else {
            dao.updateLending(entity)
            input.id
        }
        val personName = dao.getPerson(input.personId)?.name.orEmpty()
        ledger.deleteTransactionByOrigin(TransactionType.PERSONAL_LENDING.name, rowId)
        ledger.record(
            TransactionFactory.create(
                type = TransactionType.PERSONAL_LENDING,
                amount = input.amount,
                date = input.lentDate,
                category = "Lent",
                relatedType = RelatedType.PERSON,
                relatedId = input.personId,
                originType = TransactionType.PERSONAL_LENDING.name,
                originId = rowId,
                description = personName.ifBlank { "Lent" },
                notes = input.notes.trim()
            )
        )
        rowId
    }

    suspend fun deleteLending(lendingId: Long) = database.withTransaction {
        val lending = dao.getLending(lendingId) ?: return@withTransaction
        ledger.deleteTransactionByOrigin(TransactionType.PERSONAL_LENDING.name, lendingId)
        dao.deleteLending(lending)
    }

    suspend fun saveReturn(
        lendingId: Long,
        returnId: Long?,
        date: LocalDate,
        amount: Long,
        method: String,
        notes: String
    ): PaymentResult = database.withTransaction {
        val lending = dao.getLending(lendingId)
            ?: return@withTransaction PaymentResult.Rejected("We couldn't find that record.")
        val existing = dao.getReturns(lendingId)
        val receivedExcludingThis = existing.filter { it.id != returnId }.sumOf { it.amount }
        val remaining = BalanceEngine.remaining(lending.amount, receivedExcludingThis)

        when (val check = PaymentValidator.validate(amount, remaining)) {
            is PaymentValidator.Check.Valid -> {
                val entity = PersonalReturnEntity(
                    id = returnId ?: 0L,
                    lendingId = lendingId,
                    dateEpochDay = date.toEpochDay(),
                    amount = check.amount,
                    method = method,
                    notes = notes.trim(),
                    createdAt = KhataGoTime.nowMillis()
                )
                val rowId = if (returnId == null) dao.insertReturn(entity) else {
                    dao.updateReturn(entity)
                    returnId
                }
                val personName = dao.getPerson(lending.personId)?.name.orEmpty()
                ledger.deleteTransactionByOrigin(TransactionType.PERSONAL_RETURN.name, rowId)
                ledger.record(
                    TransactionFactory.create(
                        type = TransactionType.PERSONAL_RETURN,
                        amount = check.amount,
                        date = date,
                        category = "Received",
                        relatedType = RelatedType.PERSON,
                        relatedId = lending.personId,
                        originType = TransactionType.PERSONAL_RETURN.name,
                        originId = rowId,
                        description = personName.ifBlank { "Received" },
                        notes = notes.trim()
                    )
                )
                PaymentResult.Success(rowId)
            }
            else -> PaymentResult.Rejected(PaymentValidator.message(check))
        }
    }

    suspend fun deleteReturn(returnId: Long) = database.withTransaction {
        val entry = dao.getReturn(returnId) ?: return@withTransaction
        ledger.deleteTransactionByOrigin(TransactionType.PERSONAL_RETURN.name, returnId)
        dao.deleteReturn(entry)
    }

    suspend fun setPersonArchived(personId: Long, archived: Boolean) {
        val person = dao.getPerson(personId) ?: return
        dao.updatePerson(person.copy(archived = archived, updatedAt = KhataGoTime.nowMillis()))
    }

    suspend fun deletePerson(personId: Long) = database.withTransaction {
        val person = dao.getPerson(personId) ?: return@withTransaction
        dao.deletePerson(person)
    }
}

/** Combines an optional record with its list of movements. */
private fun <A, B, R> combineIfAvailable(
    first: Flow<A?>,
    second: Flow<List<B>>,
    transform: suspend (A?, List<B>) -> R
): Flow<R> = kotlinx.coroutines.flow.combine(first, second) { a, b -> transform(a, b) }

private fun com.shohan.khatago.data.local.db.rows.BorrowedRow.toAccount(): BorrowedAccount {
    val remaining = BalanceEngine.remaining(amount, paidAmount)
    return BorrowedAccount(
        id = id,
        personId = personId,
        personName = personName,
        relationship = relationship,
        amount = amount,
        paidAmount = paidAmount,
        remaining = remaining,
        borrowedDate = LocalDate.ofEpochDay(borrowedDateEpochDay),
        expectedReturnDate = expectedReturnDateEpochDay?.let { LocalDate.ofEpochDay(it) },
        notes = notes,
        progressPercent = BalanceEngine.paidPercent(amount, paidAmount),
        dueState = DueEngine.state(remaining, expectedReturnDateEpochDay?.let { LocalDate.ofEpochDay(it) }),
        archived = archived
    )
}

private fun com.shohan.khatago.data.local.db.rows.LentRow.toAccount(): LentAccount {
    val remaining = BalanceEngine.remaining(amount, receivedAmount)
    return LentAccount(
        id = id,
        personId = personId,
        personName = personName,
        relationship = relationship,
        amount = amount,
        receivedAmount = receivedAmount,
        remaining = remaining,
        lentDate = LocalDate.ofEpochDay(lentDateEpochDay),
        expectedReturnDate = expectedReturnDateEpochDay?.let { LocalDate.ofEpochDay(it) },
        notes = notes,
        progressPercent = BalanceEngine.paidPercent(amount, receivedAmount),
        dueState = DueEngine.state(remaining, expectedReturnDateEpochDay?.let { LocalDate.ofEpochDay(it) }),
        archived = archived
    )
}
