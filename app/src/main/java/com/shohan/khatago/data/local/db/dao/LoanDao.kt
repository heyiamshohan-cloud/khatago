package com.shohan.khatago.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shohan.khatago.data.local.db.entity.LoanEntity
import com.shohan.khatago.data.local.db.entity.LoanInstallmentEntity
import com.shohan.khatago.data.local.db.entity.LoanPaymentEntity
import com.shohan.khatago.data.local.db.rows.LoanAccountRow
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {

    @Query(
        """
        SELECT l.id AS id,
               l.name AS name,
               l.institution AS institution,
               l.principalAmount AS principalAmount,
               l.totalPayable AS totalPayable,
               l.installmentAmount AS installmentAmount,
               l.installmentCount AS installmentCount,
               l.frequency AS frequency,
               l.dateTakenEpochDay AS dateTakenEpochDay,
               l.firstDueDateEpochDay AS firstDueDateEpochDay,
               l.maturityDateEpochDay AS maturityDateEpochDay,
               COALESCE((SELECT SUM(i.scheduledAmount) FROM loan_installments i WHERE i.loanId = l.id), 0) AS scheduledTotal,
               COALESCE((SELECT SUM(i.paidAmount) FROM loan_installments i WHERE i.loanId = l.id), 0) AS paidAmount,
               COALESCE((SELECT SUM(i.scheduledAmount - i.paidAmount) FROM loan_installments i WHERE i.loanId = l.id AND i.paidAmount < i.scheduledAmount), 0) AS remainingAmount,
               (SELECT COUNT(*) FROM loan_installments i WHERE i.loanId = l.id AND i.paidAmount < i.scheduledAmount) AS remainingCount,
               (SELECT COUNT(*) FROM loan_installments i WHERE i.loanId = l.id AND i.paidAmount < i.scheduledAmount AND i.dueDateEpochDay < :todayEpochDay) AS overdueCount,
               (SELECT MIN(i.dueDateEpochDay) FROM loan_installments i WHERE i.loanId = l.id AND i.paidAmount < i.scheduledAmount) AS nextDueDateEpochDay,
               l.archived AS archived
        FROM loans l
        ORDER BY l.archived ASC, l.createdAt DESC
        """
    )
    fun observeLoanAccounts(todayEpochDay: Long): Flow<List<LoanAccountRow>>

    @Query("SELECT * FROM loans WHERE id = :loanId")
    fun observeLoan(loanId: Long): Flow<LoanEntity?>

    @Query("SELECT * FROM loans WHERE id = :loanId")
    suspend fun getLoan(loanId: Long): LoanEntity?

    @Query("SELECT * FROM loans ORDER BY id ASC")
    suspend fun getAllLoans(): List<LoanEntity>

    @Insert
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)

    // ------------------------------------------------------------ installments

    @Query("SELECT * FROM loan_installments WHERE loanId = :loanId ORDER BY number ASC")
    fun observeInstallments(loanId: Long): Flow<List<LoanInstallmentEntity>>

    @Query("SELECT * FROM loan_installments WHERE loanId = :loanId ORDER BY number ASC")
    suspend fun getInstallments(loanId: Long): List<LoanInstallmentEntity>

    @Query("SELECT * FROM loan_installments WHERE id = :installmentId")
    suspend fun getInstallment(installmentId: Long): LoanInstallmentEntity?

    @Insert
    suspend fun insertInstallments(installments: List<LoanInstallmentEntity>)

    @Update
    suspend fun updateInstallment(installment: LoanInstallmentEntity)

    @Query("DELETE FROM loan_installments WHERE loanId = :loanId")
    suspend fun deleteInstallmentsForLoan(loanId: Long)

    /** Oldest unsettled installment — payments are always applied here first. */
    @Query(
        """
        SELECT * FROM loan_installments
        WHERE loanId = :loanId AND paidAmount < scheduledAmount
        ORDER BY dueDateEpochDay ASC, number ASC
        LIMIT 1
        """
    )
    suspend fun nextUnsettledInstallment(loanId: Long): LoanInstallmentEntity?

    // --------------------------------------------------------------- payments

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY dateEpochDay DESC, id DESC")
    fun observePayments(loanId: Long): Flow<List<LoanPaymentEntity>>

    @Query("SELECT * FROM loan_payments WHERE id = :paymentId")
    suspend fun getPayment(paymentId: Long): LoanPaymentEntity?

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY dateEpochDay ASC, id ASC")
    suspend fun getPayments(loanId: Long): List<LoanPaymentEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM loan_payments WHERE dateEpochDay = :epochDay")
    suspend fun paymentsOnDay(epochDay: Long): Long

    @Insert
    suspend fun insertPayment(payment: LoanPaymentEntity): Long

    @Update
    suspend fun updatePayment(payment: LoanPaymentEntity)

    @Delete
    suspend fun deletePayment(payment: LoanPaymentEntity)

    // ------------------------------------------------- obligations / reminders

    @Query(
        """
        SELECT l.name AS title,
               l.institution AS subtitle,
               i.id AS refId,
               i.dueDateEpochDay AS dueDateEpochDay,
               i.scheduledAmount AS scheduledAmount,
               (i.scheduledAmount - i.paidAmount) AS remainingAmount
        FROM loan_installments i
        JOIN loans l ON l.id = i.loanId
        WHERE l.archived = 0 AND i.paidAmount < i.scheduledAmount AND i.dueDateEpochDay <= :horizonEpochDay
        ORDER BY i.dueDateEpochDay ASC
        LIMIT :limit
        """
    )
    suspend fun upcomingInstallments(horizonEpochDay: Long, limit: Int): List<UpcomingLoanRow>

    @Query(
        """
        SELECT l.name AS title,
               l.institution AS subtitle,
               i.id AS refId,
               i.dueDateEpochDay AS dueDateEpochDay,
               i.scheduledAmount AS scheduledAmount,
               (i.scheduledAmount - i.paidAmount) AS remainingAmount
        FROM loan_installments i
        JOIN loans l ON l.id = i.loanId
        WHERE l.archived = 0 AND i.paidAmount < i.scheduledAmount AND i.dueDateEpochDay <= :horizonEpochDay
        ORDER BY i.dueDateEpochDay ASC
        LIMIT :limit
        """
    )
    fun observeUpcomingInstallments(horizonEpochDay: Long, limit: Int): kotlinx.coroutines.flow.Flow<List<UpcomingLoanRow>>

    @Query("SELECT COUNT(*) FROM loans WHERE archived = 0")
    suspend fun countActiveLoans(): Int
}

/** Row returned by [LoanDao.upcomingInstallments]. */
data class UpcomingLoanRow(
    val title: String,
    val subtitle: String,
    val refId: Long,
    val dueDateEpochDay: Long,
    val scheduledAmount: Long,
    val remainingAmount: Long
)
