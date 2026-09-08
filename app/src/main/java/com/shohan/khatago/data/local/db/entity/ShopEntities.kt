package com.shohan.khatago.data.local.db.entity

import kotlinx.serialization.Serializable

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A shop the user buys from on credit.
 */
@Entity(tableName = "shops", indices = [Index(value = ["name"])])
@Serializable
data class ShopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val archived: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

/**
 * One credit purchase at a shop. A purchase holds many items (see
 * [ShopCreditItemEntity]) and is settled by one or more [ShopPaymentEntity]s.
 *
 * [totalAmount] is the sum of its items, always rewritten inside the same
 * database transaction as the items, so it can never drift.
 */
@Entity(
    tableName = "shop_credits",
    foreignKeys = [
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["id"],
            childColumns = ["shopId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["shopId"]), Index(value = ["dateEpochDay"]), Index(value = ["dueDateEpochDay"])]
)
@Serializable
data class ShopCreditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val dateEpochDay: Long,
    /** Optional promise-to-pay date; drives due-soon and overdue detection. */
    val dueDateEpochDay: Long? = null,
    val totalAmount: Long = 0L,
    val notes: String = "",
    val createdAt: Long = 0L
)

@Entity(
    tableName = "shop_credit_items",
    foreignKeys = [
        ForeignKey(
            entity = ShopCreditEntity::class,
            parentColumns = ["id"],
            childColumns = ["creditId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["creditId"])]
)
@Serializable
data class ShopCreditItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val creditId: Long,
    val name: String,
    /** Quantity in thousandths: 1.5 kg -> 1500. Never a Double. */
    val quantityMilli: Long = 1_000L,
    val unit: String = "",
    val unitPrice: Long = 0L,
    val lineTotal: Long = 0L,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "shop_payments",
    foreignKeys = [
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["id"],
            childColumns = ["shopId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ShopCreditEntity::class,
            parentColumns = ["id"],
            childColumns = ["creditId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["shopId"]), Index(value = ["creditId"]), Index(value = ["dateEpochDay"])]
)
@Serializable
data class ShopPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    /** Null when the payment settles the shop as a whole rather than one purchase. */
    val creditId: Long? = null,
    val dateEpochDay: Long,
    val amount: Long,
    val method: String = "CASH",
    val notes: String = "",
    val createdAt: Long = 0L
)
