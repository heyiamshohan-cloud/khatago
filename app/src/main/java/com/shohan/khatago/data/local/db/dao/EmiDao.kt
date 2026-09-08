package com.shohan.khatago.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shohan.khatago.data.local.db.entity.EmiInstallmentEntity
import com.shohan.khatago.data.local.db.entity.EmiPaymentEntity
import com.shohan.khatago.data.local.db.entity.EmiPurchaseEntity
import com.shohan.khatago.data.local.db.rows.EmiAccountRow
import kotlinx.coroutines.flow.Flow

@Dao
interface EmiDao {

    @Query(
        """
        SELECT e.id AS id,
               e.productName AS productName,
               e.seller AS seller,
               e.totalPrice AS totalPrice,
               e.downPayment AS downPayment,
               e.totalPayable AS totalPayable,
               e.installmentAmount AS installmentAmount,
               e.installmentCount AS installmentCount,
               e.frequency AS frequency,
               e.purchaseDateEpochDay AS purchaseDateEpochDay,
               e.firstDueDateEpochDay AS firstDueDateEpochDay,
               e.maturityDateEpochDay AS maturityDateEpochDay,
               COALESCE((SELECT SUM(i.scheduledAmount) FROM emi_installments i WHERE i.emiId = e.id), 0) AS scheduledTotal,
               COALESCE((SELECT SUM(i.paidAmount) FROM emi_installments i WHERE i.emiId = e.id), 0) AS paidAmount,
               COALESCE((SELECT SUM(i.scheduledAmount - i.paidAmount) FROM emi_installments i WHERE i.emiId = e.id AND i.paidAmount < i.scheduledAmount), 0) AS remainingAmount,
               (SELECT COUNT(*) FROM emi_installments i WHERE i.emiId = e.id AND i.paidAmount < i.scheduledAmount) AS remainingCount,
               (SELECT COUNT(*) FROM emi_installments i WHERE i.emiId = e.id AND i.paidAmount < i.scheduledAmount AND i.dueDateEpochDay < :todayEpochDay) AS overdueCount,
               (SELECT MIN(i.dueDateEpochDay) FROM emi_installments i WHERE i.emiId = e.id AND i.paidAmount < i.scheduledAmount) AS nextDueDateEpochDay,
               e.archived AS archived
        FROM emi_purchases e
        ORDER BY e.archived ASC, e.createdAt DESC
        """
    )
    fun observeEmiAccounts(todayEpochDay: Long): Flow<List<EmiAccountRow>>

    @Query("SELECT * FROM emi_purchases WHERE id = :emiId")
    fun observeEmi(emiId: Long): Flow<EmiPurchaseEntity?>

    @Query("SELECT * FROM emi_purchases WHERE id = :emiId")
    suspend fun getEmi(emiId: Long): EmiPurchaseEntity?

    @Query("SELECT * FROM emi_purchases ORDER BY id ASC")
    suspend fun getAllEmis(): List<EmiPurchaseEntity>

    @Insert
    suspend fun insertEmi(emi: EmiPurchaseEntity): Long

    @Update
    suspend fun updateEmi(emi: EmiPurchaseEntity)

    @Delete
    suspend fun deleteEmi(emi: EmiPurchaseEntity)

    // ------------------------------------------------------------ installments

    @Query("SELECT * FROM emi_installments WHERE emiId = :emiId ORDER BY number ASC")
    fun observeInstallments(emiId: Long): Flow<List<EmiInstallmentEntity>>

    @Query("SELECT * FROM emi_installments WHERE emiId = :emiId ORDER BY number ASC")
    suspend fun getInstallments(emiId: Long): List<EmiInstallmentEntity>

    @Query("SELECT * FROM emi_installments WHERE id = :installmentId")
    suspend fun getInstallment(installmentId: Long): EmiInstallmentEntity?

    @Insert
    suspend fun insertInstallments(installments: List<EmiInstallmentEntity>)

    @Update
    suspend fun updateInstallment(installment: EmiInstallmentEntity)

    @Query("DELETE FROM emi_installments WHERE emiId = :emiId")
    suspend fun deleteInstallmentsForEmi(emiId: Long)

    @Query(
        """
        SELECT * FROM emi_installments
        WHERE emiId = :emiId AND paidAmount < scheduledAmount
        ORDER BY dueDateEpochDay ASC, number ASC
        LIMIT 1
        """
    )
    suspend fun nextUnsettledInstallment(emiId: Long): EmiInstallmentEntity?

    // --------------------------------------------------------------- payments

    @Query("SELECT * FROM emi_payments WHERE emiId = :emiId ORDER BY dateEpochDay DESC, id DESC")
    fun observePayments(emiId: Long): Flow<List<EmiPaymentEntity>>

    @Query("SELECT * FROM emi_payments WHERE id = :paymentId")
    suspend fun getPayment(paymentId: Long): EmiPaymentEntity?

    @Query("SELECT * FROM emi_payments WHERE emiId = :emiId ORDER BY dateEpochDay ASC, id ASC")
    suspend fun getPayments(emiId: Long): List<EmiPaymentEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM emi_payments WHERE dateEpochDay = :epochDay")
    suspend fun paymentsOnDay(epochDay: Long): Long

    @Insert
    suspend fun insertPayment(payment: EmiPaymentEntity): Long

    @Update
    suspend fun updatePayment(payment: EmiPaymentEntity)

    @Delete
    suspend fun deletePayment(payment: EmiPaymentEntity)

    // ------------------------------------------------- obligations / reminders

    @Query(
        """
        SELECT e.productName AS title,
               e.seller AS subtitle,
               i.id AS refId,
               i.dueDateEpochDay AS dueDateEpochDay,
               i.scheduledAmount AS scheduledAmount,
               (i.scheduledAmount - i.paidAmount) AS remainingAmount
        FROM emi_installments i
        JOIN emi_purchases e ON e.id = i.emiId
        WHERE e.archived = 0 AND i.paidAmount < i.scheduledAmount AND i.dueDateEpochDay <= :horizonEpochDay
        ORDER BY i.dueDateEpochDay ASC
        LIMIT :limit
        """
    )
    suspend fun upcomingInstallments(horizonEpochDay: Long, limit: Int): List<UpcomingEmiRow>

    @Query(
        """
        SELECT e.productName AS title,
               e.seller AS subtitle,
               i.id AS refId,
               i.dueDateEpochDay AS dueDateEpochDay,
               i.scheduledAmount AS scheduledAmount,
               (i.scheduledAmount - i.paidAmount) AS remainingAmount
        FROM emi_installments i
        JOIN emi_purchases e ON e.id = i.emiId
        WHERE e.archived = 0 AND i.paidAmount < i.scheduledAmount AND i.dueDateEpochDay <= :horizonEpochDay
        ORDER BY i.dueDateEpochDay ASC
        LIMIT :limit
        """
    )
    fun observeUpcomingInstallments(horizonEpochDay: Long, limit: Int): kotlinx.coroutines.flow.Flow<List<UpcomingEmiRow>>

    @Query("SELECT COUNT(*) FROM emi_purchases WHERE archived = 0")
    suspend fun countActiveEmis(): Int
}

data class UpcomingEmiRow(
    val title: String,
    val subtitle: String,
    val refId: Long,
    val dueDateEpochDay: Long,
    val scheduledAmount: Long,
    val remainingAmount: Long
)
