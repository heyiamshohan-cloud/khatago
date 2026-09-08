package com.shohan.khatago.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shohan.khatago.data.local.db.entity.ShopCreditEntity
import com.shohan.khatago.data.local.db.entity.ShopCreditItemEntity
import com.shohan.khatago.data.local.db.entity.ShopEntity
import com.shohan.khatago.data.local.db.entity.ShopPaymentEntity
import com.shohan.khatago.data.local.db.rows.ShopAccountRow
import com.shohan.khatago.data.local.db.rows.ShopCreditRow
import com.shohan.khatago.data.local.db.rows.ShopDueRow
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {

    // ------------------------------------------------------------------- shops

    @Query("SELECT * FROM shops WHERE archived = 0 ORDER BY name COLLATE NOCASE ASC")
    fun observeShops(): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shops WHERE id = :shopId")
    fun observeShop(shopId: Long): Flow<ShopEntity?>

    @Query("SELECT * FROM shops WHERE id = :shopId")
    suspend fun getShop(shopId: Long): ShopEntity?

    @Query("SELECT * FROM shops")
    suspend fun getAllShopsIncludingArchived(): List<ShopEntity>

    @Query("SELECT * FROM shop_payments WHERE shopId = :shopId ORDER BY dateEpochDay ASC, id ASC")
    suspend fun paymentsForShop(shopId: Long): List<ShopPaymentEntity>

    @Query(
        """
        SELECT s.id AS id,
               s.name AS name,
               s.ownerName AS ownerName,
               s.phone AS phone,
               COALESCE((SELECT SUM(c.totalAmount) FROM shop_credits c WHERE c.shopId = s.id), 0) AS totalCredit,
               COALESCE((SELECT SUM(p.amount) FROM shop_payments p WHERE p.shopId = s.id), 0) AS totalPaid,
               (SELECT COUNT(*) FROM shop_credits c WHERE c.shopId = s.id) AS creditCount,
               (SELECT MAX(c.dateEpochDay) FROM shop_credits c WHERE c.shopId = s.id) AS lastActivityEpochDay
        FROM shops s
        WHERE s.archived = 0
        ORDER BY s.name COLLATE NOCASE ASC
        """
    )
    fun observeShopAccounts(): Flow<List<ShopAccountRow>>

    @Query("SELECT COUNT(*) FROM shops WHERE archived = 0")
    suspend fun countActiveShops(): Int

    @Insert
    suspend fun insertShop(shop: ShopEntity): Long

    @Update
    suspend fun updateShop(shop: ShopEntity)

    @Delete
    suspend fun deleteShop(shop: ShopEntity)

    // ---------------------------------------------------------------- credits

    @Query(
        """
        SELECT c.id AS id,
               c.shopId AS shopId,
               c.dateEpochDay AS dateEpochDay,
               c.dueDateEpochDay AS dueDateEpochDay,
               c.totalAmount AS totalAmount,
               c.notes AS notes,
               (SELECT COUNT(*) FROM shop_credit_items i WHERE i.creditId = c.id) AS itemCount
        FROM shop_credits c
        WHERE c.shopId = :shopId
        ORDER BY c.dateEpochDay DESC, c.id DESC
        """
    )
    fun observeCredits(shopId: Long): Flow<List<ShopCreditRow>>

    @Query("SELECT * FROM shop_credits WHERE id = :creditId")
    suspend fun getCredit(creditId: Long): ShopCreditEntity?

    @Query("SELECT * FROM shop_credits WHERE shopId = :shopId ORDER BY dateEpochDay ASC, id ASC")
    suspend fun getCreditsForShop(shopId: Long): List<ShopCreditEntity>

    @Insert
    suspend fun insertCredit(credit: ShopCreditEntity): Long

    @Update
    suspend fun updateCredit(credit: ShopCreditEntity)

    @Delete
    suspend fun deleteCredit(credit: ShopCreditEntity)

    // ------------------------------------------------------------------ items

    @Query("SELECT * FROM shop_credit_items WHERE creditId = :creditId ORDER BY sortOrder ASC, id ASC")
    fun observeItems(creditId: Long): Flow<List<ShopCreditItemEntity>>

    @Query("SELECT * FROM shop_credit_items WHERE creditId = :creditId ORDER BY sortOrder ASC, id ASC")
    suspend fun getItems(creditId: Long): List<ShopCreditItemEntity>

    @Insert
    suspend fun insertItems(items: List<ShopCreditItemEntity>)

    @Query("DELETE FROM shop_credit_items WHERE creditId = :creditId")
    suspend fun deleteItemsForCredit(creditId: Long)

    // --------------------------------------------------------------- payments

    @Query("SELECT * FROM shop_payments WHERE shopId = :shopId ORDER BY dateEpochDay DESC, id DESC")
    fun observePayments(shopId: Long): Flow<List<ShopPaymentEntity>>

    @Query("SELECT * FROM shop_payments WHERE id = :paymentId")
    suspend fun getPayment(paymentId: Long): ShopPaymentEntity?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM shop_payments WHERE shopId = :shopId")
    suspend fun totalPaid(shopId: Long): Long

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM shop_credits WHERE shopId = :shopId")
    suspend fun totalCredit(shopId: Long): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM shop_payments WHERE dateEpochDay = :epochDay")
    suspend fun paymentsOnDay(epochDay: Long): Long

    @Insert
    suspend fun insertPayment(payment: ShopPaymentEntity): Long

    @Update
    suspend fun updatePayment(payment: ShopPaymentEntity)

    @Delete
    suspend fun deletePayment(payment: ShopPaymentEntity)

    // ------------------------------------------------- due dates / obligations

    @Query(
        """
        SELECT c.id AS id,
               c.shopId AS shopId,
               s.name AS shopName,
               c.dateEpochDay AS dateEpochDay,
               c.dueDateEpochDay AS dueDateEpochDay,
               c.totalAmount AS totalAmount,
               (SELECT COALESCE(SUM(c2.totalAmount), 0)
                  FROM shop_credits c2
                 WHERE c2.shopId = c.shopId
                   AND (c2.dateEpochDay < c.dateEpochDay
                        OR (c2.dateEpochDay = c.dateEpochDay AND c2.id <= c.id))) AS cumulativeCredit,
               (SELECT COALESCE(SUM(p.amount), 0) FROM shop_payments p WHERE p.shopId = c.shopId) AS shopPaid
        FROM shop_credits c
        JOIN shops s ON s.id = c.shopId
        WHERE c.dueDateEpochDay IS NOT NULL AND s.archived = 0
        ORDER BY c.dueDateEpochDay ASC
        """
    )
    suspend fun creditsWithDueDate(): List<ShopDueRow>

    @Query(
        """
        SELECT c.id AS id,
               c.shopId AS shopId,
               s.name AS shopName,
               c.dateEpochDay AS dateEpochDay,
               c.dueDateEpochDay AS dueDateEpochDay,
               c.totalAmount AS totalAmount,
               (SELECT COALESCE(SUM(c2.totalAmount), 0)
                  FROM shop_credits c2
                 WHERE c2.shopId = c.shopId
                   AND (c2.dateEpochDay < c.dateEpochDay
                        OR (c2.dateEpochDay = c.dateEpochDay AND c2.id <= c.id))) AS cumulativeCredit,
               (SELECT COALESCE(SUM(p.amount), 0) FROM shop_payments p WHERE p.shopId = c.shopId) AS shopPaid
        FROM shop_credits c
        JOIN shops s ON s.id = c.shopId
        WHERE c.dueDateEpochDay IS NOT NULL AND s.archived = 0
        ORDER BY c.dueDateEpochDay ASC
        """
    )
    fun observeCreditsWithDueDate(): Flow<List<ShopDueRow>>
}
