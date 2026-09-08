package com.shohan.khatago.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query

/**
 * Global search queries. Every match is case-insensitive (SQLite LIKE ignores
 * case for ASCII) and each table is queried with its own limit so the result
 * list stays responsive.
 */
@Dao
interface SearchDao {

    @Query(
        """
        SELECT id, name AS title, ownerName AS subtitle, phone AS detail
        FROM shops
        WHERE archived = 0 AND (name LIKE '%' || :query || '%' OR ownerName LIKE '%' || :query || '%'
              OR phone LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%')
        ORDER BY name COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    suspend fun searchShops(query: String, limit: Int): List<SimpleHit>

    @Query(
        """
        SELECT id, name AS title, institution AS subtitle, '' AS detail
        FROM loans
        WHERE archived = 0 AND (name LIKE '%' || :query || '%' OR institution LIKE '%' || :query || '%'
              OR notes LIKE '%' || :query || '%')
        ORDER BY name COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    suspend fun searchLoans(query: String, limit: Int): List<SimpleHit>

    @Query(
        """
        SELECT id, productName AS title, seller AS subtitle, '' AS detail
        FROM emi_purchases
        WHERE archived = 0 AND (productName LIKE '%' || :query || '%' OR seller LIKE '%' || :query || '%'
              OR notes LIKE '%' || :query || '%')
        ORDER BY productName COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    suspend fun searchEmis(query: String, limit: Int): List<SimpleHit>

    @Query(
        """
        SELECT id, name AS title, relationship AS subtitle, phone AS detail
        FROM people
        WHERE archived = 0 AND (name LIKE '%' || :query || '%' OR relationship LIKE '%' || :query || '%'
              OR phone LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%')
        ORDER BY name COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    suspend fun searchPeople(query: String, limit: Int): List<SimpleHit>

    @Query(
        """
        SELECT id, description AS title, category AS subtitle, type AS detail, amount AS amount, dateEpochDay AS dateEpochDay
        FROM transactions
        WHERE description LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%'
              OR category LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    suspend fun searchTransactions(query: String, limit: Int): List<TransactionHit>

    @Query(
        """
        SELECT id, source AS title, category AS subtitle, 'INCOME' AS detail, amount AS amount, dateEpochDay AS dateEpochDay
        FROM income
        WHERE source LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'
        ORDER BY dateEpochDay DESC
        LIMIT :limit
        """
    )
    suspend fun searchIncome(query: String, limit: Int): List<TransactionHit>

    @Query(
        """
        SELECT id, COALESCE(NULLIF(place, ''), category) AS title, category AS subtitle, 'EXPENSE' AS detail,
               amount AS amount, dateEpochDay AS dateEpochDay
        FROM expenses
        WHERE place LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'
        ORDER BY dateEpochDay DESC
        LIMIT :limit
        """
    )
    suspend fun searchExpenses(query: String, limit: Int): List<TransactionHit>
}

data class SimpleHit(
    val id: Long,
    val title: String,
    val subtitle: String,
    val detail: String
)

data class TransactionHit(
    val id: Long,
    val title: String,
    val subtitle: String,
    val detail: String,
    val amount: Long,
    val dateEpochDay: Long
)
