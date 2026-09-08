package com.shohan.khatago.data.local.db.entity

import kotlinx.serialization.Serializable

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A product bought on installments (phone, laptop, refrigerator, ...).
 *
 * financedAmount = [totalPayable] − [downPayment]; it is derived, never stored,
 * so editing the down payment can never leave a stale financed amount behind.
 */
@Entity(
    tableName = "emi_purchases",
    indices = [Index(value = ["productName"]), Index(value = ["firstDueDateEpochDay"])]
)
@Serializable
data class EmiPurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productName: String,
    val seller: String = "",
    val purchaseDateEpochDay: Long = 0L,
    val totalPrice: Long = 0L,
    val downPayment: Long = 0L,
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
    tableName = "emi_installments",
    foreignKeys = [
        ForeignKey(
            entity = EmiPurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["emiId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["emiId"]),
        Index(value = ["emiId", "number"], unique = true),
        Index(value = ["dueDateEpochDay"])
    ]
)
@Serializable
data class EmiInstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val emiId: Long,
    val number: Int,
    val dueDateEpochDay: Long,
    val scheduledAmount: Long,
    val paidAmount: Long = 0L,
    val createdAt: Long = 0L
)

@Entity(
    tableName = "emi_payments",
    foreignKeys = [
        ForeignKey(
            entity = EmiPurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["emiId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EmiInstallmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["installmentId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["emiId"]), Index(value = ["installmentId"]), Index(value = ["dateEpochDay"])]
)
@Serializable
data class EmiPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val emiId: Long,
    val installmentId: Long? = null,
    val dateEpochDay: Long,
    val amount: Long,
    val method: String = "CASH",
    val notes: String = "",
    val createdAt: Long = 0L
)
