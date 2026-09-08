package com.shohan.khatago.data.local.db.entity

import kotlinx.serialization.Serializable

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A single owner row holding the profile created during first-run setup. */
@Entity(tableName = "user_profile")
@Serializable
data class UserProfileEntity(
    @PrimaryKey val id: Long = SINGLETON_ID,
    val name: String = "",
    val currencyCode: String = "BDT",
    val currencySymbol: String = "৳",
    val onboardingCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    companion object {
        const val SINGLETON_ID = 0L
    }
}

/** Income or expense category. Defaults ship with the app; users may add more. */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "type"], unique = true)]
)
@Serializable
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** "INCOME" or "EXPENSE". */
    val type: String,
    val isCustom: Boolean = false,
    val sortOrder: Int = 0,
    val archived: Boolean = false
)

/**
 * [yearMonth] is `year * 100 + month` (202609 = September 2026). It is written on
 * insert and indexed so every monthly chart and report groups by calendar month
 * in SQL instead of loading rows into memory.
 */
@Entity(
    tableName = "income",
    indices = [Index(value = ["dateEpochDay"]), Index(value = ["category"]), Index(value = ["yearMonth"])]
)
@Serializable
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val dateEpochDay: Long,
    val yearMonth: Int = 0,
    val source: String = "",
    val category: String = "Other",
    val notes: String = "",
    val createdAt: Long = 0L
)

@Entity(
    tableName = "expenses",
    indices = [Index(value = ["dateEpochDay"]), Index(value = ["category"]), Index(value = ["yearMonth"])]
)
@Serializable
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val dateEpochDay: Long,
    val yearMonth: Int = 0,
    val category: String = "Other",
    val place: String = "",
    val notes: String = "",
    val createdAt: Long = 0L
)

/**
 * The central ledger. One row per financial event.
 *
 * Debt creating events are recorded so the activity timeline is complete, but the
 * [countsAsIncome]/[countsAsExpense] semantics live in TransactionType — see
 * FINANCIAL_LOGIC.md. Reports and the dashboard use the same rules.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["dateEpochDay"]),
        Index(value = ["type"]),
        Index(value = ["relatedType", "relatedId"]),
        Index(value = ["category"]),
        Index(value = ["yearMonth"])
    ]
)
@Serializable
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = 0L,
    val dateEpochDay: Long = 0L,
    /** `year * 100 + month`, kept in sync with [dateEpochDay] on write. */
    val yearMonth: Int = 0,
    val amount: Long = 0L,
    val type: String = "EXPENSE",
    val category: String = "",
    /** SHOP, LOAN, EMI, PERSON, INCOME, EXPENSE, NONE */
    /** The account a row belongs to, so a tap can open it: SHOP, LOAN, EMI, PERSON, INCOME, EXPENSE. */
    val relatedType: String = "NONE",
    val relatedId: Long = 0L,
    /** The exact record that created this row (e.g. SHOP_PAYMENT / loan payment id). */
    val originType: String = "NONE",
    val originId: Long = 0L,
    val description: String = "",
    val notes: String = "",
    val createdAt: Long = 0L
)

/**
 * Tracks what a reminder has already been shown for so notifications are never
 * duplicated, and so settled obligations stop producing reminders.
 */
@Entity(
    tableName = "reminders",
    indices = [Index(value = ["refType", "refId"], unique = true)]
)
@Serializable
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** LOAN_INSTALLMENT, EMI_INSTALLMENT, PERSONAL_DEBT, PERSONAL_LENDING, SHOP_CREDIT */
    val refType: String,
    val refId: Long,
    val dueDateEpochDay: Long = 0L,
    val lastNotifiedEpochDay: Long? = null,
    val snoozedUntilEpochDay: Long? = null,
    val createdAt: Long = 0L
)
