package com.shohan.khatago.data.local.db.entity

import kotlinx.serialization.Serializable

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A person the user borrows from or lends to.
 */
@Entity(tableName = "people", indices = [Index(value = ["name"])])
@Serializable
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relationship: String = "",
    val phone: String = "",
    val notes: String = "",
    val archived: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

/** Money the user BORROWED from a person (a payable). */
@Entity(
    tableName = "personal_debts",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["personId"]), Index(value = ["expectedReturnDateEpochDay"])]
)
@Serializable
data class PersonalDebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val amount: Long,
    val borrowedDateEpochDay: Long = 0L,
    val expectedReturnDateEpochDay: Long? = null,
    val notes: String = "",
    val archived: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

/** A repayment made against a [PersonalDebtEntity]. */
@Entity(
    tableName = "personal_repayments",
    foreignKeys = [
        ForeignKey(
            entity = PersonalDebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["debtId"]), Index(value = ["dateEpochDay"])]
)
@Serializable
data class PersonalRepaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val dateEpochDay: Long,
    val amount: Long,
    val method: String = "CASH",
    val notes: String = "",
    val createdAt: Long = 0L
)

/** Money the user LENT to a person (a receivable). */
@Entity(
    tableName = "personal_lending",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["personId"]), Index(value = ["expectedReturnDateEpochDay"])]
)
@Serializable
data class PersonalLendingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val amount: Long,
    val lentDateEpochDay: Long = 0L,
    val expectedReturnDateEpochDay: Long? = null,
    val notes: String = "",
    val archived: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

/** Money received back against a [PersonalLendingEntity]. */
@Entity(
    tableName = "personal_returns",
    foreignKeys = [
        ForeignKey(
            entity = PersonalLendingEntity::class,
            parentColumns = ["id"],
            childColumns = ["lendingId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["lendingId"]), Index(value = ["dateEpochDay"])]
)
@Serializable
data class PersonalReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lendingId: Long,
    val dateEpochDay: Long,
    val amount: Long,
    val method: String = "CASH",
    val notes: String = "",
    val createdAt: Long = 0L
)
