package com.shohan.khatago.data.backup

import androidx.room.withTransaction
import android.content.Context
import android.net.Uri
import com.shohan.khatago.core.result.Outcome
import com.shohan.khatago.data.local.db.KhataGoDatabase
import com.shohan.khatago.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

data class RestoreSummary(
    val shops: Int = 0,
    val loans: Int = 0,
    val emis: Int = 0,
    val people: Int = 0,
    val transactions: Int = 0
)

/**
 * Local JSON backup and restore using the Android Storage Access Framework.
 *
 * Nothing is uploaded anywhere: the file is written to the location the user
 * chooses and read back from the file the user picks.
 *
 * A restore validates the whole file BEFORE the current database is modified, so
 * a corrupt or unsupported backup can never leave the user with half a dataset.
 */
class BackupRepository(
    private val context: Context,
    private val database: KhataGoDatabase,
    private val settingsRepository: SettingsRepository
) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun createBackup(appVersion: String): BackupPayload = withContext(Dispatchers.IO) {
        val dao = database.ledgerDao()
        val shopDao = database.shopDao()
        val loanDao = database.loanDao()
        val emiDao = database.emiDao()
        val personalDao = database.personalDao()

        BackupPayload(
            backupVersion = BackupSchema.BACKUP_VERSION,
            schemaVersion = KhataGoDatabase.VERSION,
            appVersion = appVersion,
            exportedAt = System.currentTimeMillis(),
            currencyCode = dao.getProfile()?.currencyCode ?: "BDT",
            userProfile = dao.getProfile(),
            shops = allShops(),
            shopCredits = allShopCredits(),
            shopItems = allShopItems(),
            shopPayments = allShopPayments(),
            loans = loanDao.getAllLoans(),
            loanInstallments = allLoanInstallments(),
            loanPayments = allLoanPayments(),
            emiPurchases = emiDao.getAllEmis(),
            emiInstallments = allEmiInstallments(),
            emiPayments = allEmiPayments(),
            people = personalDao.getAllPeople(),
            personalDebts = personalDao.getAllDebts(),
            personalRepayments = allRepayments(),
            personalLending = personalDao.getAllLending(),
            personalReturns = allReturns(),
            income = dao.getAllIncome(),
            expenses = dao.getAllExpenses(),
            categories = dao.getAllCategories(),
            transactions = dao.getAllTransactions(),
            reminders = database.reminderDao().getAll(),
            settings = settingsRepository.snapshot()
        )
    }

    suspend fun writeTo(uri: Uri, payload: BackupPayload): Outcome<Int> = withContext(Dispatchers.IO) {
        try {
            val text = json.encodeToString(payload)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(text.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: return@withContext Outcome.Failure(MESSAGE_WRITE_FAILED)
            Outcome.Success(text.toByteArray(Charsets.UTF_8).size)
        } catch (_: IOException) {
            Outcome.Failure(MESSAGE_WRITE_FAILED)
        } catch (_: SecurityException) {
            Outcome.Failure(MESSAGE_WRITE_FAILED)
        } catch (_: Exception) {
            Outcome.Failure(MESSAGE_WRITE_FAILED)
        }
    }

    suspend fun readFrom(uri: Uri): Outcome<BackupPayload> = withContext(Dispatchers.IO) {
        val text = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
        if (text.isNullOrBlank()) return@withContext Outcome.Failure(MESSAGE_UNREADABLE)

        val payload = try {
            json.decodeFromString<BackupPayload>(text)
        } catch (_: SerializationException) {
            return@withContext Outcome.Failure(MESSAGE_UNREADABLE)
        } catch (_: IllegalArgumentException) {
            return@withContext Outcome.Failure(MESSAGE_UNREADABLE)
        }

        validate(payload)
    }

    /**
     * Full validation. Any problem — unsupported version, missing fields, invalid
     * values, duplicate ids or broken relationships — rejects the file without
     * touching current data.
     */
    fun validate(payload: BackupPayload): Outcome<BackupPayload> {
        if (payload.backupVersion < BackupSchema.MIN_SUPPORTED_BACKUP_VERSION ||
            payload.backupVersion > BackupSchema.BACKUP_VERSION ||
            payload.schemaVersion > KhataGoDatabase.VERSION
        ) {
            return Outcome.Failure(MESSAGE_NEWER_VERSION)
        }

        val negative = payload.shops.any { it.id < 0L } ||
            payload.shopCredits.any { it.totalAmount < 0L } ||
            payload.shopPayments.any { it.amount < 0L } ||
            payload.loans.any { it.principalAmount < 0L || it.totalPayable < 0L } ||
            payload.loanInstallments.any { it.scheduledAmount < 0L || it.paidAmount < 0L } ||
            payload.loanPayments.any { it.amount < 0L } ||
            payload.emiPurchases.any { it.totalPayable < 0L || it.downPayment < 0L } ||
            payload.emiInstallments.any { it.scheduledAmount < 0L || it.paidAmount < 0L } ||
            payload.emiPayments.any { it.amount < 0L } ||
            payload.personalDebts.any { it.amount < 0L } ||
            payload.personalRepayments.any { it.amount < 0L } ||
            payload.personalLending.any { it.amount < 0L } ||
            payload.personalReturns.any { it.amount < 0L } ||
            payload.income.any { it.amount < 0L } ||
            payload.expenses.any { it.amount < 0L } ||
            payload.transactions.any { it.amount < 0L }
        if (negative) return Outcome.Failure(MESSAGE_UNREADABLE)

        val duplicateIds = hasDuplicates(payload.shops.map { it.id }) ||
            hasDuplicates(payload.shopCredits.map { it.id }) ||
            hasDuplicates(payload.shopItems.map { it.id }) ||
            hasDuplicates(payload.loans.map { it.id }) ||
            hasDuplicates(payload.loanInstallments.map { it.id }) ||
            hasDuplicates(payload.emiPurchases.map { it.id }) ||
            hasDuplicates(payload.emiInstallments.map { it.id }) ||
            hasDuplicates(payload.people.map { it.id }) ||
            hasDuplicates(payload.personalDebts.map { it.id }) ||
            hasDuplicates(payload.personalLending.map { it.id }) ||
            hasDuplicates(payload.transactions.map { it.id })
        if (duplicateIds) return Outcome.Failure(MESSAGE_UNREADABLE)

        val shopIds = payload.shops.map { it.id }.toSet()
        val creditIds = payload.shopCredits.map { it.id }.toSet()
        val loanIds = payload.loans.map { it.id }.toSet()
        val loanInstallmentIds = payload.loanInstallments.map { it.id }.toSet()
        val emiIds = payload.emiPurchases.map { it.id }.toSet()
        val emiInstallmentIds = payload.emiInstallments.map { it.id }.toSet()
        val personIds = payload.people.map { it.id }.toSet()
        val debtIds = payload.personalDebts.map { it.id }.toSet()
        val lendingIds = payload.personalLending.map { it.id }.toSet()

        val brokenRelationships =
            payload.shopCredits.any { it.shopId !in shopIds } ||
                payload.shopItems.any { it.creditId !in creditIds } ||
                payload.shopPayments.any { it.shopId !in shopIds || (it.creditId != null && it.creditId !in creditIds) } ||
                payload.loanInstallments.any { it.loanId !in loanIds } ||
                payload.loanPayments.any { it.loanId !in loanIds || (it.installmentId != null && it.installmentId !in loanInstallmentIds) } ||
                payload.emiInstallments.any { it.emiId !in emiIds } ||
                payload.emiPayments.any { it.emiId !in emiIds || (it.installmentId != null && it.installmentId !in emiInstallmentIds) } ||
                payload.personalDebts.any { it.personId !in personIds } ||
                payload.personalRepayments.any { it.debtId !in debtIds } ||
                payload.personalLending.any { it.personId !in personIds } ||
                payload.personalReturns.any { it.lendingId !in lendingIds }
        if (brokenRelationships) return Outcome.Failure(MESSAGE_UNREADABLE)

        return Outcome.Success(payload)
    }

    suspend fun restore(payload: BackupPayload): Outcome<RestoreSummary> =
        withContext(Dispatchers.IO) {
            try {
                database.withTransaction {
                    database.clearAllTables()

                    payload.userProfile?.let { database.ledgerDao().insertProfile(it) }
                    payload.categories.forEach { database.ledgerDao().insertCategory(it) }

                    payload.shops.forEach { database.shopDao().insertShop(it) }
                    payload.shopCredits.forEach { database.shopDao().insertCredit(it) }
                    payload.shopItems.forEach { database.shopDao().insertItems(listOf(it)) }
                    payload.shopPayments.forEach { database.shopDao().insertPayment(it) }

                    payload.loans.forEach { database.loanDao().insertLoan(it) }
                    payload.loanInstallments.forEach { database.loanDao().insertInstallments(listOf(it)) }
                    payload.loanPayments.forEach { database.loanDao().insertPayment(it) }

                    payload.emiPurchases.forEach { database.emiDao().insertEmi(it) }
                    payload.emiInstallments.forEach { database.emiDao().insertInstallments(listOf(it)) }
                    payload.emiPayments.forEach { database.emiDao().insertPayment(it) }

                    payload.people.forEach { database.personalDao().insertPerson(it) }
                    payload.personalDebts.forEach { database.personalDao().insertDebt(it) }
                    payload.personalRepayments.forEach { database.personalDao().insertRepayment(it) }
                    payload.personalLending.forEach { database.personalDao().insertLending(it) }
                    payload.personalReturns.forEach { database.personalDao().insertReturn(it) }

                    payload.income.forEach { database.ledgerDao().insertIncome(it) }
                    payload.expenses.forEach { database.ledgerDao().insertExpense(it) }
                    payload.transactions.forEach { database.ledgerDao().insertTransaction(it) }
                    payload.reminders.forEach { database.reminderDao().insert(it) }
                }
                settingsRepository.restore(payload.settings)
                Outcome.Success(
                    RestoreSummary(
                        shops = payload.shops.size,
                        loans = payload.loans.size,
                        emis = payload.emiPurchases.size,
                        people = payload.people.size,
                        transactions = payload.transactions.size
                    )
                )
            } catch (_: Exception) {
                Outcome.Failure(MESSAGE_RESTORE_FAILED)
            }
        }

    // ------------------------------------------------------------------ helpers

    private suspend fun allShops(): List<com.shohan.khatago.data.local.db.entity.ShopEntity> {
        // Shops are read through the DAO's live query which excludes archived rows,
        // so archived shops are collected separately to keep the backup complete.
        val active = database.shopDao().observeShops().firstOrNull() ?: emptyList()
        val archived = database.shopDao().getAllShopsIncludingArchived()
        return (active + archived).distinctBy { it.id }
    }

    private suspend fun allShopCredits(): List<com.shohan.khatago.data.local.db.entity.ShopCreditEntity> {
        val ids = allShops().map { it.id }
        return ids.flatMap { database.shopDao().getCreditsForShop(it) }
    }

    private suspend fun allShopItems(): List<com.shohan.khatago.data.local.db.entity.ShopCreditItemEntity> =
        allShopCredits().flatMap { database.shopDao().getItems(it.id) }

    private suspend fun allShopPayments(): List<com.shohan.khatago.data.local.db.entity.ShopPaymentEntity> =
        allShops().flatMap { shop -> database.shopDao().paymentsForShop(shop.id) }

    private suspend fun allLoanInstallments(): List<com.shohan.khatago.data.local.db.entity.LoanInstallmentEntity> =
        database.loanDao().getAllLoans().flatMap { database.loanDao().getInstallments(it.id) }

    private suspend fun allLoanPayments(): List<com.shohan.khatago.data.local.db.entity.LoanPaymentEntity> =
        database.loanDao().getAllLoans().flatMap { database.loanDao().getPayments(it.id) }

    private suspend fun allEmiInstallments(): List<com.shohan.khatago.data.local.db.entity.EmiInstallmentEntity> =
        database.emiDao().getAllEmis().flatMap { database.emiDao().getInstallments(it.id) }

    private suspend fun allEmiPayments(): List<com.shohan.khatago.data.local.db.entity.EmiPaymentEntity> =
        database.emiDao().getAllEmis().flatMap { database.emiDao().getPayments(it.id) }

    private suspend fun allRepayments(): List<com.shohan.khatago.data.local.db.entity.PersonalRepaymentEntity> =
        database.personalDao().getAllDebts().flatMap { database.personalDao().getRepayments(it.id) }

    private suspend fun allReturns(): List<com.shohan.khatago.data.local.db.entity.PersonalReturnEntity> =
        database.personalDao().getAllLending().flatMap { database.personalDao().getReturns(it.id) }

    private fun hasDuplicates(ids: List<Long>): Boolean = ids.size != ids.toSet().size

    companion object {
        const val MESSAGE_UNREADABLE = "This backup file can't be read."
        const val MESSAGE_NEWER_VERSION = "This backup was made by a newer version of KhataGo."
        const val MESSAGE_WRITE_FAILED = "We couldn't save the backup. Try another location."
        const val MESSAGE_RESTORE_FAILED = "We couldn't restore that backup. Your data is unchanged."
    }
}
