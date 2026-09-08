package com.shohan.khatago.data.repository

import androidx.room.withTransaction
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.data.local.db.dao.LedgerDao
import com.shohan.khatago.data.local.db.entity.CategoryEntity
import com.shohan.khatago.data.local.db.entity.ExpenseEntity
import com.shohan.khatago.data.local.db.entity.IncomeEntity
import com.shohan.khatago.data.local.db.entity.TransactionEntity
import com.shohan.khatago.data.local.db.entity.TransactionFactory
import com.shohan.khatago.data.local.db.entity.UserProfileEntity
import com.shohan.khatago.data.local.db.rows.CategoryTotalRow
import com.shohan.khatago.data.local.db.rows.MonthTotalRow
import com.shohan.khatago.domain.model.LedgerEntry
import com.shohan.khatago.domain.model.RelatedType
import com.shohan.khatago.domain.model.TransactionType
import com.shohan.khatago.domain.model.TodaySnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Category names installed with a fresh database. */
object DefaultCategories {
    val INCOME = listOf("Salary", "Business", "Freelance", "Bonus", "Gift", "Refund", "Other")
    val EXPENSE = listOf(
        "Food", "Groceries", "Transport", "Home", "Medical",
        "Education", "Bills", "Shopping", "Entertainment", "Other"
    )
}

/**
 * Income, expense, categories, the central transaction ledger and the user
 * profile. Every write goes through a Room transaction so the ledger can never
 * drift from the accounts that produced it.
 */
class LedgerRepository(
    private val dao: LedgerDao,
    private val database: com.shohan.khatago.data.local.db.KhataGoDatabase
) {

    // ------------------------------------------------------------------ profile

    fun observeProfile(): Flow<UserProfileEntity?> = dao.observeProfile()

    suspend fun getProfile(): UserProfileEntity? = dao.getProfile()

    suspend fun saveProfile(name: String, onboardingCompleted: Boolean = true) {
        val now = KhataGoTime.nowMillis()
        val existing = dao.getProfile()
        val profile = if (existing == null) {
            UserProfileEntity(
                id = UserProfileEntity.SINGLETON_ID,
                name = name.trim(),
                onboardingCompleted = onboardingCompleted,
                createdAt = now,
                updatedAt = now
            )
        } else {
            existing.copy(
                name = name.trim(),
                onboardingCompleted = onboardingCompleted,
                updatedAt = now
            )
        }
        if (existing == null) dao.insertProfile(profile) else dao.updateProfile(profile)
    }

    // --------------------------------------------------------------- categories

    fun observeCategories(type: String): Flow<List<CategoryEntity>> = dao.observeCategories(type)

    suspend fun ensureDefaultCategories() {
        if (dao.getAllCategories().isNotEmpty()) return
        DefaultCategories.INCOME.forEachIndexed { index, name ->
            dao.insertCategory(CategoryEntity(name = name, type = "INCOME", sortOrder = index))
        }
        DefaultCategories.EXPENSE.forEachIndexed { index, name ->
            dao.insertCategory(CategoryEntity(name = name, type = "EXPENSE", sortOrder = index))
        }
    }

    suspend fun addCategory(name: String, type: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return false
        if (dao.getAllCategories().any { it.name.equals(trimmed, ignoreCase = true) && it.type == type }) {
            return false
        }
        val order = dao.getAllCategories().filter { it.type == type }.maxOfOrNull { it.sortOrder + 1 } ?: 0
        dao.insertCategory(
            CategoryEntity(name = trimmed, type = type, isCustom = true, sortOrder = order)
        )
        return true
    }

    suspend fun deleteCategory(category: CategoryEntity) = dao.deleteCategory(category)

    // ------------------------------------------------------------------- income

    fun observeIncome(limit: Int = 200): Flow<List<IncomeEntity>> = dao.observeIncome(limit)

    fun observeIncomeBetween(startDay: Long, endDay: Long, limit: Int = 500): Flow<List<IncomeEntity>> =
        dao.observeIncomeBetween(startDay, endDay, limit)

    suspend fun getIncome(id: Long): IncomeEntity? = dao.getIncome(id)

    suspend fun saveIncome(
        id: Long?,
        amount: Long,
        date: LocalDate,
        source: String,
        category: String,
        notes: String
    ): Long = database.withTransaction {
        val epochDay = date.toEpochDay()
        val entity = IncomeEntity(
            id = id ?: 0L,
            amount = amount,
            dateEpochDay = epochDay,
            yearMonth = date.year * 100 + date.monthValue,
            source = source.trim(),
            category = category,
            notes = notes.trim(),
            createdAt = KhataGoTime.nowMillis()
        )
        val rowId = if (id == null) dao.insertIncome(entity) else {
            dao.updateIncome(entity)
            id
        }
        dao.deleteByOrigin(TransactionType.INCOME.name, rowId)
        dao.insertTransaction(
            TransactionFactory.create(
                type = TransactionType.INCOME,
                amount = amount,
                date = date,
                category = category,
                relatedType = RelatedType.INCOME,
                relatedId = rowId,
                originType = TransactionType.INCOME.name,
                originId = rowId,
                description = source.trim().ifBlank { category },
                notes = notes.trim()
            )
        )
        rowId
    }

    suspend fun deleteIncome(entry: IncomeEntity) = database.withTransaction {
        dao.deleteByOrigin(TransactionType.INCOME.name, entry.id)
        dao.deleteIncome(entry)
    }

    // ------------------------------------------------------------------ expense

    fun observeExpenses(limit: Int = 200): Flow<List<ExpenseEntity>> = dao.observeExpenses(limit)

    fun observeExpensesBetween(startDay: Long, endDay: Long, limit: Int = 500): Flow<List<ExpenseEntity>> =
        dao.observeExpensesBetween(startDay, endDay, limit)

    suspend fun getExpense(id: Long): ExpenseEntity? = dao.getExpense(id)

    suspend fun saveExpense(
        id: Long?,
        amount: Long,
        date: LocalDate,
        category: String,
        place: String,
        notes: String
    ): Long = database.withTransaction {
        val entity = ExpenseEntity(
            id = id ?: 0L,
            amount = amount,
            dateEpochDay = date.toEpochDay(),
            yearMonth = date.year * 100 + date.monthValue,
            category = category,
            place = place.trim(),
            notes = notes.trim(),
            createdAt = KhataGoTime.nowMillis()
        )
        val rowId = if (id == null) dao.insertExpense(entity) else {
            dao.updateExpense(entity)
            id
        }
        dao.deleteByOrigin(TransactionType.EXPENSE.name, rowId)
        dao.insertTransaction(
            TransactionFactory.create(
                type = TransactionType.EXPENSE,
                amount = amount,
                date = date,
                category = category,
                relatedType = RelatedType.EXPENSE,
                relatedId = rowId,
                originType = TransactionType.EXPENSE.name,
                originId = rowId,
                description = place.trim().ifBlank { category },
                notes = notes.trim()
            )
        )
        rowId
    }

    suspend fun deleteExpense(entry: ExpenseEntity) = database.withTransaction {
        dao.deleteByOrigin(TransactionType.EXPENSE.name, entry.id)
        dao.deleteExpense(entry)
    }

    // ------------------------------------------------------------- transactions

    fun observeRecentTransactions(limit: Int = 8): Flow<List<LedgerEntry>> =
        dao.observeRecentTransactions(limit).map { rows -> rows.map { it.toEntry() } }

    fun observeTransactionsBetween(
        startDay: Long,
        endDay: Long,
        limit: Int = 500
    ): Flow<List<LedgerEntry>> =
        dao.observeTransactionsBetween(startDay, endDay, limit).map { rows -> rows.map { it.toEntry() } }

    fun observeTransactionsFiltered(
        startDay: Long,
        endDay: Long,
        type: TransactionType?,
        limit: Int = 500
    ): Flow<List<LedgerEntry>> =
        dao.observeTransactionsFiltered(startDay, endDay, type?.name, limit)
            .map { rows -> rows.map { it.toEntry() } }

    suspend fun record(entry: TransactionEntity): Long = dao.insertTransaction(entry)

    suspend fun deleteTransactionByOrigin(originType: String, originId: Long) =
        dao.deleteByOrigin(originType, originId)

    suspend fun getAllTransactions(): List<TransactionEntity> = dao.getAllTransactions()

    suspend fun countTransactions(): Int = dao.countTransactions()

    suspend fun incomeTotal(startDay: Long, endDay: Long): Long = dao.incomeTotal(startDay, endDay)
    suspend fun expenseTotal(startDay: Long, endDay: Long): Long = dao.expenseTotal(startDay, endDay)
    suspend fun paymentsTotal(startDay: Long, endDay: Long): Long = dao.paymentsTotal(startDay, endDay)

    suspend fun incomeByCategory(startDay: Long, endDay: Long): List<CategoryTotalRow> =
        dao.incomeByCategory(startDay, endDay)

    suspend fun expenseByCategory(startDay: Long, endDay: Long): List<CategoryTotalRow> =
        dao.expenseByCategory(startDay, endDay)

    suspend fun incomeByMonth(from: Int, to: Int): List<MonthTotalRow> = dao.incomeByMonth(from, to)
    suspend fun expenseByMonth(from: Int, to: Int): List<MonthTotalRow> = dao.expenseByMonth(from, to)
    suspend fun paymentsByMonth(from: Int, to: Int): List<MonthTotalRow> =
        dao.transactionsByMonth(from, to, TransactionType.LOAN_PAYMENT.name)

    // ------------------------------------------------------- reactive aggregates

    fun observeTodaySnapshot(epochDay: Long): Flow<TodaySnapshot> = combine(
        dao.observeIncomeOnDay(epochDay),
        dao.observeExpenseOnDay(epochDay),
        dao.observePaymentsOnDay(epochDay)
    ) { income, expense, payments ->
        TodaySnapshot(income = income, expense = expense, payments = payments)
    }

    fun observeIncomeByMonth(from: Int, to: Int): Flow<List<MonthTotalRow>> =
        dao.observeIncomeByMonth(from, to)

    fun observeExpenseByMonth(from: Int, to: Int): Flow<List<MonthTotalRow>> =
        dao.observeExpenseByMonth(from, to)

    fun observePaymentsByMonth(from: Int, to: Int): Flow<List<MonthTotalRow>> =
        dao.observePaymentsByMonth(from, to)

    fun observeIncomeByCategory(startDay: Long, endDay: Long): Flow<List<CategoryTotalRow>> =
        dao.observeIncomeByCategory(startDay, endDay)

    fun observeExpenseByCategory(startDay: Long, endDay: Long): Flow<List<CategoryTotalRow>> =
        dao.observeExpenseByCategory(startDay, endDay)

    fun observeIncomeTotalFlow(startDay: Long, endDay: Long): Flow<Long> =
        dao.observeIncomeTotal(startDay, endDay)

    fun observeExpenseTotalFlow(startDay: Long, endDay: Long): Flow<Long> =
        dao.observeExpenseTotal(startDay, endDay)

    fun observePaymentsTotalFlow(startDay: Long, endDay: Long): Flow<Long> =
        dao.observePaymentsTotal(startDay, endDay)
}

fun TransactionEntity.toEntry(): LedgerEntry = LedgerEntry(
    id = id,
    timestamp = timestamp,
    date = dateEpochDay.let { LocalDate.ofEpochDay(it) },
    amount = amount,
    type = TransactionType.from(type),
    category = category,
    relatedType = runCatching { RelatedType.valueOf(relatedType) }.getOrDefault(RelatedType.NONE),
    relatedId = relatedId,
    description = description,
    notes = notes
)
