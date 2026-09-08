package com.shohan.khatago.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shohan.khatago.data.local.db.dao.EmiDao
import com.shohan.khatago.data.local.db.dao.LedgerDao
import com.shohan.khatago.data.local.db.dao.LoanDao
import com.shohan.khatago.data.local.db.dao.PersonalDao
import com.shohan.khatago.data.local.db.dao.ReminderDao
import com.shohan.khatago.data.local.db.dao.SearchDao
import com.shohan.khatago.data.local.db.dao.ShopDao
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

/**
 * The single source of truth for KhataGo. Everything is local: no backend, no
 * sync, no cloud.
 */
@Database(
    entities = [
        UserProfileEntity::class,
        CategoryEntity::class,
        ShopEntity::class,
        ShopCreditEntity::class,
        ShopCreditItemEntity::class,
        ShopPaymentEntity::class,
        LoanEntity::class,
        LoanInstallmentEntity::class,
        LoanPaymentEntity::class,
        EmiPurchaseEntity::class,
        EmiInstallmentEntity::class,
        EmiPaymentEntity::class,
        PersonEntity::class,
        PersonalDebtEntity::class,
        PersonalRepaymentEntity::class,
        PersonalLendingEntity::class,
        PersonalReturnEntity::class,
        IncomeEntity::class,
        ExpenseEntity::class,
        TransactionEntity::class,
        ReminderEntity::class
    ],
    version = KhataGoDatabase.VERSION,
    exportSchema = true
)
abstract class KhataGoDatabase : RoomDatabase() {

    abstract fun shopDao(): ShopDao
    abstract fun loanDao(): LoanDao
    abstract fun emiDao(): EmiDao
    abstract fun personalDao(): PersonalDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun searchDao(): SearchDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val VERSION = 1
        const val DATABASE_NAME = "khatago.db"

        fun build(context: Context): KhataGoDatabase =
            Room.databaseBuilder(context, KhataGoDatabase::class.java, DATABASE_NAME)
                // Explicit migrations only — destructive migration is never
                // enabled, so an upgrade can never silently erase records.
                .addMigrations(*KhataGoMigrations.ALL)
                .build()
    }
}

/**
 * Migration strategy.
 *
 * Version 1 is the initial schema, so there is nothing to migrate yet; the list
 * below is where every future change is added, alongside the exported schema
 * JSON in app/schemas. Because destructive migration is disabled, Room will
 * throw (and the app will refuse to open the database) rather than wipe a
 * user's financial history if a migration is ever missing.
 */
object KhataGoMigrations {

    val ALL: Array<Migration> = emptyArray()

    /** Example kept for future revisions — shows the required pattern. */
    fun exampleTemplate(): Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_yearMonth ON transactions(yearMonth)")
        }
    }
}
