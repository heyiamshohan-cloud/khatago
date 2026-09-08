package com.shohan.khatago.data.repository

import androidx.room.withTransaction
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.data.local.db.KhataGoDatabase
import com.shohan.khatago.data.local.db.dao.ShopDao
import com.shohan.khatago.data.local.db.entity.ShopCreditEntity
import com.shohan.khatago.data.local.db.entity.ShopCreditItemEntity
import com.shohan.khatago.data.local.db.entity.ShopEntity
import com.shohan.khatago.data.local.db.entity.ShopPaymentEntity
import com.shohan.khatago.data.local.db.entity.TransactionFactory
import com.shohan.khatago.domain.finance.AllocationEngine
import com.shohan.khatago.domain.finance.BalanceEngine
import com.shohan.khatago.domain.finance.PaymentValidator
import com.shohan.khatago.domain.model.RelatedType
import com.shohan.khatago.domain.model.ShopAccount
import com.shohan.khatago.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** One line of a shop credit purchase. */
data class CreditItemInput(
    val id: Long = 0L,
    val name: String = "",
    val quantityMilli: Long = 1_000L,
    val unit: String = "",
    val unitPrice: Long = 0L,
    val lineTotal: Long = 0L,
    val sortOrder: Int = 0
)

/** A merged purchase/payment line for the shop ledger, with its running balance. */
data class ShopLedgerLine(
    val id: Long,
    val kind: LineKind,
    val date: LocalDate,
    val amount: Long,
    val title: String,
    val subtitle: String,
    val dueDate: LocalDate? = null,
    val items: List<ShopCreditItemEntity> = emptyList(),
    val runningBalance: Long = 0L,
    val settled: Boolean = false
)

enum class LineKind { PURCHASE, PAYMENT }

data class ShopLedger(
    val shop: ShopEntity?,
    val lines: List<ShopLedgerLine>,
    val totalCredit: Long,
    val totalPaid: Long,
    val remaining: Long
)

/**
 * Shop credit: shops, multi-item purchases and payments.
 *
 * Every mutation runs inside a Room transaction and rewrites the central ledger
 * row for that record, so balances, history and reports always agree.
 */
class ShopRepository(
    private val database: KhataGoDatabase,
    private val dao: ShopDao,
    private val ledger: LedgerRepository
) {

    // ------------------------------------------------------------------- reads

    fun observeAccounts(): Flow<List<ShopAccount>> =
        dao.observeShopAccounts().map { rows ->
            rows.map { row ->
                ShopAccount(
                    id = row.id,
                    name = row.name,
                    ownerName = row.ownerName,
                    phone = row.phone,
                    totalCredit = row.totalCredit,
                    totalPaid = row.totalPaid,
                    remaining = BalanceEngine.remaining(row.totalCredit, row.totalPaid),
                    creditCount = row.creditCount,
                    lastActivity = row.lastActivityEpochDay?.let { LocalDate.ofEpochDay(it) }
                )
            }
        }

    fun observeShop(shopId: Long): Flow<ShopEntity?> = dao.observeShop(shopId)

    suspend fun getShop(shopId: Long): ShopEntity? = dao.getShop(shopId)

    fun observeLedger(shopId: Long): Flow<ShopLedger> = combine(
        dao.observeShop(shopId),
        dao.observeCredits(shopId),
        dao.observePayments(shopId)
    ) { shop, credits, payments ->
        val purchaseLines = credits.map { credit ->
            ShopLedgerLine(
                id = credit.id,
                kind = LineKind.PURCHASE,
                date = LocalDate.ofEpochDay(credit.dateEpochDay),
                amount = credit.totalAmount,
                title = "Purchase",
                subtitle = buildCreditSubtitle(credit),
                dueDate = credit.dueDateEpochDay?.let { LocalDate.ofEpochDay(it) }
            )
        }
        val paymentLines = payments.map { payment ->
            ShopLedgerLine(
                id = payment.id,
                kind = LineKind.PAYMENT,
                date = LocalDate.ofEpochDay(payment.dateEpochDay),
                amount = payment.amount,
                title = "Payment",
                subtitle = payment.notes.ifBlank { prettyMethod(payment.method) }
            )
        }

        val totalCredit = credits.sumOf { it.totalAmount }
        val totalPaid = payments.sumOf { it.amount }

        // FIFO: the oldest purchases are settled first.
        val orderedCredits = credits.sortedWith(compareBy({ it.dateEpochDay }, { it.id }))
        val covered = AllocationEngine.allocateCredits(
            orderedCredits.map { it.id to it.totalAmount },
            totalPaid
        )

        val merged = (purchaseLines + paymentLines)
            .sortedWith(compareBy({ it.date.toEpochDay() }, { it.kind == LineKind.PAYMENT }))
        var running = 0L
        val withBalance = merged.map { line ->
            running = if (line.kind == LineKind.PURCHASE) {
                Money.safeAdd(running, line.amount)
            } else {
                (running - line.amount).coerceAtLeast(0L)
            }
            line.copy(
                runningBalance = running,
                settled = line.kind == LineKind.PURCHASE &&
                    (covered[line.id] ?: 0L) >= line.amount &&
                    line.amount > 0L
            )
        }

        ShopLedger(
            shop = shop,
            lines = withBalance,
            totalCredit = totalCredit,
            totalPaid = totalPaid,
            remaining = BalanceEngine.remaining(totalCredit, totalPaid)
        )
    }

    fun observeCredits(shopId: Long) = dao.observeCredits(shopId)

    fun observeItems(creditId: Long) = dao.observeItems(creditId)

    suspend fun getItems(creditId: Long) = dao.getItems(creditId)

    suspend fun getCredit(creditId: Long) = dao.getCredit(creditId)

    suspend fun getPayment(paymentId: Long) = dao.getPayment(paymentId)

    suspend fun remainingForShop(shopId: Long): Long {
        val credit = dao.totalCredit(shopId)
        val paid = dao.totalPaid(shopId)
        return BalanceEngine.remaining(credit, paid)
    }

    // ------------------------------------------------------------------ writes

    suspend fun saveShop(
        id: Long?,
        name: String,
        ownerName: String,
        phone: String,
        address: String,
        notes: String
    ): Long {
        val now = KhataGoTime.nowMillis()
        val entity = ShopEntity(
            id = id ?: 0L,
            name = name.trim(),
            ownerName = ownerName.trim(),
            phone = phone.trim(),
            address = address.trim(),
            notes = notes.trim(),
            createdAt = now,
            updatedAt = now
        )
        return if (id == null) dao.insertShop(entity) else {
            dao.updateShop(entity)
            id
        }
    }

    suspend fun setArchived(shopId: Long, archived: Boolean) {
        val shop = dao.getShop(shopId) ?: return
        dao.updateShop(shop.copy(archived = archived, updatedAt = KhataGoTime.nowMillis()))
    }

    /** Deletes a shop and, through Room cascades, its purchases, items and payments. */
    suspend fun deleteShop(shopId: Long) = database.withTransaction {
        val shop = dao.getShop(shopId) ?: return@withTransaction
        dao.getCreditsForShop(shopId).forEach { credit ->
            ledger.deleteTransactionByOrigin(TransactionType.SHOP_CREDIT.name, credit.id)
        }
        dao.deleteShop(shop)
    }

    suspend fun saveCredit(
        shopId: Long,
        creditId: Long?,
        date: LocalDate,
        dueDate: LocalDate?,
        items: List<CreditItemInput>,
        notes: String
    ): Long = database.withTransaction {
        val total = items.sumOf { it.lineTotal.coerceAtLeast(0L) }
        val entity = ShopCreditEntity(
            id = creditId ?: 0L,
            shopId = shopId,
            dateEpochDay = date.toEpochDay(),
            dueDateEpochDay = dueDate?.toEpochDay(),
            totalAmount = total,
            notes = notes.trim(),
            createdAt = KhataGoTime.nowMillis()
        )
        val rowId = if (creditId == null) {
            dao.insertCredit(entity)
        } else {
            dao.updateCredit(entity)
            dao.deleteItemsForCredit(creditId)
            creditId
        }
        dao.insertItems(items.mapIndexed { index, item ->
            ShopCreditItemEntity(
                id = if (creditId == null) 0L else item.id,
                creditId = rowId,
                name = item.name.trim(),
                quantityMilli = item.quantityMilli,
                unit = item.unit.trim(),
                unitPrice = item.unitPrice,
                lineTotal = item.lineTotal,
                sortOrder = index
            )
        })

        val shopName = dao.getShop(shopId)?.name.orEmpty()
        ledger.deleteTransactionByOrigin(TransactionType.SHOP_CREDIT.name, rowId)
        ledger.record(
            TransactionFactory.create(
                type = TransactionType.SHOP_CREDIT,
                amount = total,
                date = date,
                category = "Shop Credit",
                relatedType = RelatedType.SHOP,
                relatedId = shopId,
                originType = TransactionType.SHOP_CREDIT.name,
                originId = rowId,
                description = shopName.ifBlank { "Shop credit" },
                notes = items.joinToString(", ") { it.name.trim() }.take(120)
            )
        )
        rowId
    }

    suspend fun deleteCredit(creditId: Long) = database.withTransaction {
        val credit = dao.getCredit(creditId) ?: return@withTransaction
        ledger.deleteTransactionByOrigin(TransactionType.SHOP_CREDIT.name, creditId)
        dao.deleteItemsForCredit(creditId)
        dao.deleteCredit(credit)
    }

    /**
     * Records a shop payment. Partial and multiple payments are supported; the
     * amount may never exceed what the shop is still owed.
     */
    suspend fun savePayment(
        shopId: Long,
        paymentId: Long?,
        date: LocalDate,
        amount: Long,
        method: String,
        notes: String,
        creditId: Long? = null
    ): PaymentResult = database.withTransaction {
        val totalCredit = dao.totalCredit(shopId)
        val paidExcludingThis = dao.totalPaid(shopId) -
            (paymentId?.let { dao.getPayment(it)?.amount ?: 0L } ?: 0L)
        val remaining = BalanceEngine.remaining(totalCredit, paidExcludingThis)

        when (val check = PaymentValidator.validate(amount, remaining)) {
            is PaymentValidator.Check.Valid -> {
                val entity = ShopPaymentEntity(
                    id = paymentId ?: 0L,
                    shopId = shopId,
                    creditId = creditId,
                    dateEpochDay = date.toEpochDay(),
                    amount = check.amount,
                    method = method,
                    notes = notes.trim(),
                    createdAt = KhataGoTime.nowMillis()
                )
                val rowId = if (paymentId == null) dao.insertPayment(entity) else {
                    dao.updatePayment(entity)
                    paymentId
                }
                val shopName = dao.getShop(shopId)?.name.orEmpty()
                ledger.deleteTransactionByOrigin(TransactionType.SHOP_PAYMENT.name, rowId)
                ledger.record(
                    TransactionFactory.create(
                        type = TransactionType.SHOP_PAYMENT,
                        amount = check.amount,
                        date = date,
                        category = "Shop Payment",
                        relatedType = RelatedType.SHOP,
                        relatedId = shopId,
                        originType = TransactionType.SHOP_PAYMENT.name,
                        originId = rowId,
                        description = shopName.ifBlank { "Shop payment" },
                        notes = notes.trim()
                    )
                )
                PaymentResult.Success(rowId)
            }
            else -> PaymentResult.Rejected(PaymentValidator.message(check))
        }
    }

    suspend fun deletePayment(paymentId: Long) = database.withTransaction {
        val payment = dao.getPayment(paymentId) ?: return@withTransaction
        ledger.deleteTransactionByOrigin(TransactionType.SHOP_PAYMENT.name, paymentId)
        dao.deletePayment(payment)
    }

    suspend fun creditTotals(): Pair<Long, Long> {
        val outstanding = dao.creditsWithDueDate().sumOf { row ->
            AllocationEngine.creditRemaining(
                creditTotal = row.totalAmount,
                olderCreditTotal = row.cumulativeCredit - row.totalAmount,
                shopPaid = row.shopPaid
            )
        }
        return outstanding to 0L
    }

    private fun buildCreditSubtitle(credit: com.shohan.khatago.data.local.db.rows.ShopCreditRow): String =
        when {
            credit.notes.isNotBlank() -> credit.notes
            credit.itemCount > 0 -> "${credit.itemCount} " + if (credit.itemCount == 1) "item" else "items"
            else -> ""
        }

    private fun prettyMethod(method: String): String =
        method.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")
}

sealed interface PaymentResult {
    data class Success(val id: Long) : PaymentResult
    data class Rejected(val message: String) : PaymentResult
}
