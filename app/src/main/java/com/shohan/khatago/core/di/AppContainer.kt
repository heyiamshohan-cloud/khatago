package com.shohan.khatago.core.di

import android.content.Context
import com.shohan.khatago.data.backup.BackupRepository
import com.shohan.khatago.data.export.ExportRepository
import com.shohan.khatago.data.local.db.KhataGoDatabase
import com.shohan.khatago.data.local.db.dao.EmiDao
import com.shohan.khatago.data.local.db.dao.LoanDao
import com.shohan.khatago.data.local.db.dao.PersonalDao
import com.shohan.khatago.data.local.db.dao.ReminderDao
import com.shohan.khatago.data.local.db.dao.ShopDao
import com.shohan.khatago.data.repository.DashboardRepository
import com.shohan.khatago.data.repository.EmiRepository
import com.shohan.khatago.data.repository.LedgerRepository
import com.shohan.khatago.data.repository.LoanRepository
import com.shohan.khatago.data.repository.PersonalRepository
import com.shohan.khatago.data.repository.ReportsRepository
import com.shohan.khatago.data.repository.SearchRepository
import com.shohan.khatago.data.repository.SettingsRepository
import com.shohan.khatago.data.repository.ShopRepository
import com.shohan.khatago.core.time.TodayProvider
import com.shohan.khatago.work.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * A tiny, explicit dependency container.
 *
 * KhataGo has no DI framework: the graph is small, declaration is cheap to read,
 * and construction order is obvious (database -> daos -> repositories).
 */
class AppContainer(applicationContext: Context) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: KhataGoDatabase = KhataGoDatabase.build(applicationContext)

    val shopDao: ShopDao get() = database.shopDao()
    val loanDao: LoanDao get() = database.loanDao()
    val emiDao: EmiDao get() = database.emiDao()
    val personalDao: PersonalDao get() = database.personalDao()
    val reminderDao: ReminderDao get() = database.reminderDao()

    val todayProvider = TodayProvider(appScope)

    val settingsRepository = SettingsRepository(applicationContext)

    val ledgerRepository = LedgerRepository(database.ledgerDao(), database)

    val shopRepository = ShopRepository(database, database.shopDao(), ledgerRepository)
    val loanRepository = LoanRepository(database, database.loanDao(), ledgerRepository)
    val emiRepository = EmiRepository(database, database.emiDao(), ledgerRepository)
    val personalRepository = PersonalRepository(database, database.personalDao(), ledgerRepository)

    val dashboardRepository = DashboardRepository(
        shopDao = database.shopDao(),
        loanDao = database.loanDao(),
        emiDao = database.emiDao(),
        personalDao = database.personalDao(),
        shopRepository = shopRepository,
        loanRepository = loanRepository,
        emiRepository = emiRepository,
        personalRepository = personalRepository,
        ledgerRepository = ledgerRepository,
        todayProvider = todayProvider
    )

    val reportsRepository = ReportsRepository(
        shopRepository = shopRepository,
        loanRepository = loanRepository,
        emiRepository = emiRepository,
        personalRepository = personalRepository,
        ledgerRepository = ledgerRepository,
        todayProvider = todayProvider
    )

    val searchRepository = SearchRepository(database.searchDao())

    val backupRepository = BackupRepository(applicationContext, database, settingsRepository)

    val exportRepository = ExportRepository(applicationContext)

    val reminderScheduler = ReminderScheduler(applicationContext)

    /** Installs the default income and expense categories on first run. */
    suspend fun ensureDefaults() {
        ledgerRepository.ensureDefaultCategories()
    }
}
