package com.shohan.khatago.data.local.db.entity

import kotlinx.serialization.Serializable

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A loan with a real installment schedule.
 *
 * [totalPayable] is what the user must repay in total (principal plus interest
 * and processing fee). [installmentAmount] is a convenience default; the
 * authoritative per-installment amounts live in [LoanInstallmentEntity].
 */
@Entity(tableName = "loans", indices = [Index(value = ["name"]), Index(value = ["firstDueDateEpochDay"])])
@Serializable
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val institution: String = "",
    val principalAmount: Long = 0L,
    val processingFee: Long = 0L,
    /** Interest rate in basis points: 12.50% -> 1250. */
    val interestRateBps: Long = 0L,
    val dateTakenEpochDay: Long = 0L,
    val totalPayable: Long = 0L,
    val installmentAmount: Long = 0L,
    val installmentCount: Int = 0,
    val frequency: String = "MONTHLY",
    val firstDueDateEpochDay: Long = 0L,
    val maturityDateEpochDay: Long = 0L,
    val notes: String = "",
    val archived: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Entity(
    tableName = "loan_installments",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["loanId"]),
        Index(value = ["loanId", "number"], unique = true),
        Index(value = ["dueDateEpochDay"])
    ]
)
@Serializable
data class LoanInstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val number: Int,
    val dueDateEpochDay: Long,
    val scheduledAmount: Long,
    val paidAmount: Long = 0L,
    val createdAt: Long = 0L
)

@Entity(
    tableName = "loan_payments",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LoanInstallmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["installmentId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["loanId"]), Index(value = ["installmentId"]), Index(value = ["dateEpochDay"])]
)
@Serializable
data class LoanPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    /** Every payment is traceable to the installment it settled. */
    val installmentId: Long? = null,
    val dateEpochDay: Long,
    val amount: Long,
    val method: String = "CASH",
    val notes: String = "",
    val createdAt: Long = 0L
)
