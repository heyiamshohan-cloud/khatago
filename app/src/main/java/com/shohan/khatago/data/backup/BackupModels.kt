package com.shohan.khatago.data.backup

import com.shohan.khatago.data.local.db.entity.CategoryEntity
import com.shohan.khatago.data.local.db.entity.EmiInstallmentEntity
import com.shohan.khatago.data.local.db.entity.EmiPaymentEntity
import com.shohan.khatago.data.local.db.entity.EmiPurchaseEntity
import com.shohan.khatago.data.local.db.entity.ExpenseEntity
import com.shohan.khatago.data.local.db.entity.IncomeEntity
import com.shohan.khatago.data.local.db.entity.LoanEntity
import com.shohan.khatago.data.local.db.entity.LoanInstallmentEntity
import com.shohan.khatago.data.local.db.entity.LoanPaymentEntity
import com.shohan.khatago.data.local.db.entity.PersonEntity
import com.shohan.khatago.data.local.db.entity.PersonalDebtEntity
import com.shohan.khatago.data.local.db.entity.PersonalLendingEntity
import com.shohan.khatago.data.local.db.entity.PersonalRepaymentEntity
import com.shohan.khatago.data.local.db.entity.PersonalReturnEntity
import com.shohan.khatago.data.local.db.entity.ReminderEntity
import com.shohan.khatago.data.local.db.entity.ShopCreditEntity
import com.shohan.khatago.data.local.db.entity.ShopCreditItemEntity
import com.shohan.khatago.data.local.db.entity.ShopEntity
import com.shohan.khatago.data.local.db.entity.ShopPaymentEntity
import com.shohan.khatago.data.local.db.entity.TransactionEntity
import com.shohan.khatago.data.local.db.entity.UserProfileEntity
import com.shohan.khatago.data.repository.AppSettingsSnapshot
import kotlinx.serialization.Serializable

/**
 * Versioned backup schema.
 *
 * [backupVersion] is bumped when the shape of this file changes;
 * [schemaVersion] records the Room database version that produced it. Both are
 * checked before a single row of the user's current data is touched.
 */
@Serializable
data class BackupPayload(
    val backupVersion: Int = BackupSchema.BACKUP_VERSION,
    val schemaVersion: Int = 1,
    val appVersion: String = "1.0.0",
    val exportedAt: Long = 0L,
    val currencyCode: String = "BDT",

    val userProfile: UserProfileEntity? = null,

    val shops: List<ShopEntity> = emptyList(),
    val shopCredits: List<ShopCreditEntity> = emptyList(),
    val shopItems: List<ShopCreditItemEntity> = emptyList(),
    val shopPayments: List<ShopPaymentEntity> = emptyList(),

    val loans: List<LoanEntity> = emptyList(),
    val loanInstallments: List<LoanInstallmentEntity> = emptyList(),
    val loanPayments: List<LoanPaymentEntity> = emptyList(),

    val emiPurchases: List<EmiPurchaseEntity> = emptyList(),
    val emiInstallments: List<EmiInstallmentEntity> = emptyList(),
    val emiPayments: List<EmiPaymentEntity> = emptyList(),

    val people: List<PersonEntity> = emptyList(),
    val personalDebts: List<PersonalDebtEntity> = emptyList(),
    val personalRepayments: List<PersonalRepaymentEntity> = emptyList(),
    val personalLending: List<PersonalLendingEntity> = emptyList(),
    val personalReturns: List<PersonalReturnEntity> = emptyList(),

    val income: List<IncomeEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),

    val settings: AppSettingsSnapshot = AppSettingsSnapshot()
)

object BackupSchema {
    const val BACKUP_VERSION = 1
    const val MIN_SUPPORTED_BACKUP_VERSION = 1
    const val FILE_EXTENSION = ".khbackup"
    const val MIME_TYPE = "application/json"
}
