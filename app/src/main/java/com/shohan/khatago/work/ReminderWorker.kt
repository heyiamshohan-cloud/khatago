package com.shohan.khatago.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.shohan.khatago.KhataGoApplication
import com.shohan.khatago.MainActivity
import com.shohan.khatago.R
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.data.local.db.dao.EmiDao
import com.shohan.khatago.data.local.db.dao.LoanDao
import com.shohan.khatago.data.local.db.dao.PersonalDao
import com.shohan.khatago.data.local.db.dao.ReminderDao
import com.shohan.khatago.data.local.db.dao.ShopDao
import com.shohan.khatago.data.local.db.entity.ReminderEntity
import com.shohan.khatago.data.repository.SettingsRepository
import com.shohan.khatago.domain.finance.AllocationEngine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Daily local scan for payment reminders.
 *
 * Rules:
 *  - reminders only fire while the setting is on;
 *  - a fully paid obligation never produces a reminder;
 *  - each obligation is announced at most once per day (recorded in the reminders
 *    table), so notifications are never duplicated;
 *  - lock-screen notifications are private and never expose amounts.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
    private val loanDao: LoanDao,
    private val emiDao: EmiDao,
    private val personalDao: PersonalDao,
    private val shopDao: ShopDao,
    private val reminderDao: ReminderDao,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, params) {

    /** Constructor used by WorkManager's default factory. */
    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        container(appContext).loanDao,
        container(appContext).emiDao,
        container(appContext).personalDao,
        container(appContext).shopDao,
        container(appContext).reminderDao,
        container(appContext).settingsRepository
    )

    override suspend fun doWork(): Result {
        val settings = settingsRepository.settings.first()
        if (!settings.remindersEnabled) return Result.success()
        if (!hasNotificationPermission()) return Result.success()

        val today = KhataGoTime.today()
        val horizon = today.plusDays(settings.reminderDaysBefore.toLong()).toEpochDay()

        val obligations = ArrayList<Obligation>()

        loanDao.upcomingInstallments(horizon, 50).forEach { row ->
            if (row.remainingAmount > 0L) {
                obligations += Obligation(
                    refType = REF_LOAN,
                    refId = row.refId,
                    due = LocalDate.ofEpochDay(row.dueDateEpochDay),
                    title = row.title,
                    action = "payment"
                )
            }
        }
        emiDao.upcomingInstallments(horizon, 50).forEach { row ->
            if (row.remainingAmount > 0L) {
                obligations += Obligation(
                    refType = REF_EMI,
                    refId = row.refId,
                    due = LocalDate.ofEpochDay(row.dueDateEpochDay),
                    title = row.title,
                    action = "payment"
                )
            }
        }
        personalDao.upcomingBorrowed(horizon, 50).forEach { row ->
            if (row.remainingAmount > 0L) {
                obligations += Obligation(
                    refType = REF_BORROWED,
                    refId = row.refId,
                    due = LocalDate.ofEpochDay(row.dueDateEpochDay),
                    title = row.title,
                    action = "repayment"
                )
            }
        }
        shopDao.creditsWithDueDate().forEach { row ->
            val remaining = AllocationEngine.creditRemaining(
                creditTotal = row.totalAmount,
                olderCreditTotal = row.cumulativeCredit - row.totalAmount,
                shopPaid = row.shopPaid
            )
            val due = LocalDate.ofEpochDay(row.dueDateEpochDay)
            if (remaining > 0L && due.toEpochDay() <= horizon) {
                obligations += Obligation(
                    refType = REF_SHOP,
                    refId = row.id,
                    due = due,
                    title = row.shopName,
                    action = "payment"
                )
            }
        }

        if (obligations.isEmpty()) return Result.success()

        // Skip anything already announced today; drop entries that are settled.
        val due = ArrayList<Obligation>()
        obligations.forEach { obligation ->
            val existing = reminderDao.find(obligation.refType, obligation.refId)
            if (existing == null) {
                reminderDao.insert(
                    ReminderEntity(
                        refType = obligation.refType,
                        refId = obligation.refId,
                        dueDateEpochDay = obligation.due.toEpochDay(),
                        createdAt = System.currentTimeMillis()
                    )
                )
            } else if (existing.lastNotifiedEpochDay == today.toEpochDay()) {
                return@forEach
            }
            due += obligation
        }

        if (due.isEmpty()) return Result.success()

        createChannelIfNeeded()

        val overdue = due.filter { it.due.isBefore(today) }
        val dueToday = due.filter { it.due.isEqual(today) }
        val soon = due.filter { it.due.isAfter(today) }

        notifyGroup(ID_OVERDUE, overdue, "payments are overdue", "payment is overdue", "overdue")
        notifyGroup(ID_DUE_TODAY, dueToday, "payments due today", "payment due today", "due today")
        notifyGroup(ID_SOON, soon, "payments coming up", "payment coming up", "due soon")

        val notifiedToday = today.toEpochDay()
        due.forEach { obligation ->
            val existing = reminderDao.find(obligation.refType, obligation.refId)
            if (existing != null) {
                reminderDao.update(
                    existing.copy(
                        dueDateEpochDay = obligation.due.toEpochDay(),
                        lastNotifiedEpochDay = notifiedToday
                    )
                )
            }
        }

        return Result.success()
    }

    private fun notifyGroup(
        notificationId: Int,
        items: List<Obligation>,
        pluralTitle: String,
        singularTitle: String,
        suffix: String
    ) {
        if (items.isEmpty()) return
        val title = if (items.size == 1) {
            "${items.first().title} · $singularTitle"
        } else {
            "${items.size} $pluralTitle"
        }
        val text = if (items.size == 1) {
            "${items.first().title} — ${items.first().action} $suffix"
        } else {
            items.take(3).joinToString("\n") { "${it.title} — ${it.action} $suffix" } +
                if (items.size > 3) "\n+${items.size - 3} more" else ""
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("KhataGo")
                    .setContentText("You have a payment reminder.")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()
            )
            .build()

        if (!hasNotificationPermission()) return
        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
        }
    }

    private fun hasNotificationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    private fun createChannelIfNeeded() {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (manager?.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Payment reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders for upcoming, due and overdue payments."
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }
        manager?.createNotificationChannel(channel)
    }

    private data class Obligation(
        val refType: String,
        val refId: Long,
        val due: LocalDate,
        val title: String,
        val action: String
    )

    private companion object {
        const val CHANNEL_ID = "khatago_payment_reminders"
        const val ID_OVERDUE = 2001
        const val ID_DUE_TODAY = 2002
        const val ID_SOON = 2003

        const val REF_LOAN = "LOAN_INSTALLMENT"
        const val REF_EMI = "EMI_INSTALLMENT"
        const val REF_BORROWED = "PERSONAL_DEBT"
        const val REF_SHOP = "SHOP_CREDIT"

        fun container(context: Context) =
            (context.applicationContext as KhataGoApplication).container
    }
}

/** Schedules the daily local reminder scan. */
class ReminderScheduler(context: Context) {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun scheduleDailyReminderScan() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(2, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    /** Turns reminders on: (re)schedules the daily scan immediately. */
    fun enable() {
        scheduleDailyReminderScan()
    }

    /** Turns reminders off: no further scans are scheduled. */
    fun disable() {
        cancel()
    }

    private companion object {
        const val UNIQUE_WORK_NAME = "khatago_payment_reminders"
    }
}
