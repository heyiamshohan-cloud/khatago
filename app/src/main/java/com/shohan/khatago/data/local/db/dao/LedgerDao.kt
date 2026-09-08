package com.shohan.khatago.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shohan.khatago.data.local.db.entity.CategoryEntity
import com.shohan.khatago.data.local.db.entity.ExpenseEntity
import com.shohan.khatago.data.local.db.entity.IncomeEntity
import com.shohan.khatago.data.local.db.entity.TransactionEntity
import com.shohan.khatago.data.local.db.entity.UserProfileEntity
import com.shohan.khatago.data.local.db.rows.CategoryTotalRow
import com.shohan.khatago.data.local.db.rows.MonthTotalRow
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {

    // ----------------------------------------------------------------- profile

    @Query("SELECT * FROM user_profile WHERE id = 0 LIMIT 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 0 LIMIT 1")
    suspend fun getProfile(): UserProfileEntity?

    @Insert
    suspend fun insertProfile(profile: UserProfileEntity)

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)

    // -------------------------------------------------------------- categories

    @Query("SELECT * FROM categories WHERE type = :type AND archived = 0 ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeCategories(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY type ASC, sortOrder ASC, name COLLATE NOCASE ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    // ------------------------------------------------------------------ income

    @Query("SELECT * FROM income ORDER BY dateEpochDay DESC, id DESC LIMIT :limit")
    fun observeIncome(limit: Int): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM income WHERE dateEpochDay BETWEEN :startDay AND :endDay ORDER BY dateEpochDay DESC, id DESC LIMIT :limit")
    fun observeIncomeBetween(startDay: Long, endDay: Long, limit: Int): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM income WHERE id = :id")
    suspend fun getIncome(id: Long): IncomeEntity?

    @Query("SELECT * FROM income ORDER BY id ASC")
    suspend fun getAllIncome(): List<IncomeEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM income WHERE dateEpochDay BETWEEN :startDay AND :endDay")
    suspend fun incomeTotal(startDay: Long, endDay: Long): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM income WHERE dateEpochDay = :epochDay")
    suspend fun incomeOnDay(epochDay: Long): Long

    @Query(
        """
        SELECT category AS name, COALESCE(SUM(amount), 0) AS total
        FROM income
        WHERE dateEpochDay BETWEEN :startDay AND :endDay
        GROUP BY category
        ORDER BY total DESC
        """
    )
    suspend fun incomeByCategory(startDay: Long, endDay: Long): List<CategoryTotalRow>

    @Query(
        """
        SELECT yearMonth AS period, COALESCE(SUM(amount), 0) AS total
        FROM income
        WHERE yearMonth BETWEEN :fromPeriod AND :toPeriod
        GROUP BY yearMonth
        ORDER BY yearMonth ASC
        """
    )
    suspend fun incomeByMonth(fromPeriod: Int, toPeriod: Int): List<MonthTotalRow>

    @Insert
    suspend fun insertIncome(entry: IncomeEntity): Long

    @Update
    suspend fun updateIncome(entry: IncomeEntity)

    @Delete
    suspend fun deleteIncome(entry: IncomeEntity)

    // ----------------------------------------------------------------- expense

    @Query("SELECT * FROM expenses ORDER BY dateEpochDay DESC, id DESC LIMIT :limit")
    fun observeExpenses(limit: Int): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE dateEpochDay BETWEEN :startDay AND :endDay ORDER BY dateEpochDay DESC, id DESC LIMIT :limit")
    fun observeExpensesBetween(startDay: Long, endDay: Long, limit: Int): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpense(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses ORDER BY id ASC")
    suspend fun getAllExpenses(): List<ExpenseEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE dateEpochDay BETWEEN :startDay AND :endDay")
    suspend fun expenseTotal(startDay: Long, endDay: Long): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE dateEpochDay = :epochDay")
    suspend fun expenseOnDay(epochDay: Long): Long

    @Query(
        """
        SELECT category AS name, COALESCE(SUM(amount), 0) AS total
        FROM expenses
        WHERE dateEpochDay BETWEEN :startDay AND :endDay
        GROUP BY category
        ORDER BY total DESC
        """
    )
    suspend fun expenseByCategory(startDay: Long, endDay: Long): List<CategoryTotalRow>

    @Query(
        """
        SELECT yearMonth AS period, COALESCE(SUM(amount), 0) AS total
        FROM expenses
        WHERE yearMonth BETWEEN :fromPeriod AND :toPeriod
        GROUP BY yearMonth
        ORDER BY yearMonth ASC
        """
    )
    suspend fun expenseByMonth(fromPeriod: Int, toPeriod: Int): List<MonthTotalRow>

    @Insert
    suspend fun insertExpense(entry: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(entry: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(entry: ExpenseEntity)

    // ------------------------------------------------------------ transactions

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC LIMIT :limit")
    fun observeRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE dateEpochDay BETWEEN :startDay AND :endDay
        ORDER BY timestamp DESC, id DESC
        LIMIT :limit
        """
    )
    fun observeTransactionsBetween(startDay: Long, endDay: Long, limit: Int): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE dateEpochDay BETWEEN :startDay AND :endDay
          AND (:type IS NULL OR type = :type)
        ORDER BY timestamp DESC, id DESC
        LIMIT :limit
        """
    )
    fun observeTransactionsFiltered(
        startDay: Long,
        endDay: Long,
        type: String?,
        limit: Int
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY id ASC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransaction(id: Long): TransactionEntity?

    /** Payments made in a period (liability settlements, never counted as income). */
    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE dateEpochDay BETWEEN :startDay AND :endDay
          AND type IN ('SHOP_PAYMENT', 'LOAN_PAYMENT', 'EMI_PAYMENT', 'PERSONAL_REPAYMENT')
        """
    )
    suspend fun paymentsTotal(startDay: Long, endDay: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE dateEpochDay = :epochDay
          AND type IN ('SHOP_PAYMENT', 'LOAN_PAYMENT', 'EMI_PAYMENT', 'PERSONAL_REPAYMENT')
        """
    )
    suspend fun paymentsOnDay(epochDay: Long): Long

    @Query(
        """
        SELECT yearMonth AS period, COALESCE(SUM(amount), 0) AS total
        FROM transactions
        WHERE yearMonth BETWEEN :fromPeriod AND :toPeriod AND type = :type
        GROUP BY yearMonth
        ORDER BY yearMonth ASC
        """
    )
    suspend fun transactionsByMonth(fromPeriod: Int, toPeriod: Int, type: String): List<MonthTotalRow>

    // --------------------------------------------------- reactive aggregates

    @Query("SELECT COALESCE(SUM(amount), 0) FROM income WHERE dateEpochDay = :epochDay")
    fun observeIncomeOnDay(epochDay: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM income WHERE dateEpochDay BETWEEN :startDay AND :endDay")
    fun observeIncomeTotal(startDay: Long, endDay: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE dateEpochDay BETWEEN :startDay AND :endDay")
    fun observeExpenseTotal(startDay: Long, endDay: Long): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE dateEpochDay BETWEEN :startDay AND :endDay
          AND type IN ('SHOP_PAYMENT', 'LOAN_PAYMENT', 'EMI_PAYMENT', 'PERSONAL_REPAYMENT')
        """
    )
    fun observePaymentsTotal(startDay: Long, endDay: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE dateEpochDay = :epochDay")
    fun observeExpenseOnDay(epochDay: Long): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE dateEpochDay = :epochDay
          AND type IN ('SHOP_PAYMENT', 'LOAN_PAYMENT', 'EMI_PAYMENT', 'PERSONAL_REPAYMENT')
        """
    )
    fun observePaymentsOnDay(epochDay: Long): Flow<Long>

    @Query(
        """
        SELECT yearMonth AS period, COALESCE(SUM(amount), 0) AS total
        FROM income
        WHERE yearMonth BETWEEN :fromPeriod AND :toPeriod
        GROUP BY yearMonth
        ORDER BY yearMonth ASC
        """
    )
    fun observeIncomeByMonth(fromPeriod: Int, toPeriod: Int): Flow<List<MonthTotalRow>>

    @Query(
        """
        SELECT yearMonth AS period, COALESCE(SUM(amount), 0) AS total
        FROM expenses
        WHERE yearMonth BETWEEN :fromPeriod AND :toPeriod
        GROUP BY yearMonth
        ORDER BY yearMonth ASC
        """
    )
    fun observeExpenseByMonth(fromPeriod: Int, toPeriod: Int): Flow<List<MonthTotalRow>>

    @Query(
        """
        SELECT yearMonth AS period, COALESCE(SUM(amount), 0) AS total
        FROM transactions
        WHERE yearMonth BETWEEN :fromPeriod AND :toPeriod
          AND type IN ('SHOP_PAYMENT', 'LOAN_PAYMENT', 'EMI_PAYMENT', 'PERSONAL_REPAYMENT')
        GROUP BY yearMonth
        ORDER BY yearMonth ASC
        """
    )
    fun observePaymentsByMonth(fromPeriod: Int, toPeriod: Int): Flow<List<MonthTotalRow>>

    @Query(
        """
        SELECT category AS name, COALESCE(SUM(amount), 0) AS total
        FROM income
        WHERE dateEpochDay BETWEEN :startDay AND :endDay
        GROUP BY category
        ORDER BY total DESC
        """
    )
    fun observeIncomeByCategory(startDay: Long, endDay: Long): Flow<List<CategoryTotalRow>>

    @Query(
        """
        SELECT category AS name, COALESCE(SUM(amount), 0) AS total
        FROM expenses
        WHERE dateEpochDay BETWEEN :startDay AND :endDay
        GROUP BY category
        ORDER BY total DESC
        """
    )
    fun observeExpenseByCategory(startDay: Long, endDay: Long): Flow<List<CategoryTotalRow>>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun countTransactions(): Int

    @Insert
    suspend fun insertTransaction(entry: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(entry: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(entry: TransactionEntity)

    @Query("DELETE FROM transactions WHERE relatedType = :relatedType AND relatedId = :relatedId")
    suspend fun deleteTransactionsFor(relatedType: String, relatedId: Long)

    @Query("DELETE FROM transactions WHERE originType = :originType AND originId = :originId")
    suspend fun deleteByOrigin(originType: String, originId: Long)
}
