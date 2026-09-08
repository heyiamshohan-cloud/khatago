package com.shohan.khatago

import android.app.Application
import com.shohan.khatago.core.di.AppContainer

/**
 * KhataGo application entry point.
 *
 * Offline-first by design: no analytics SDK, no advertising SDK and no remote
 * backend are initialised here. The only background work scheduled is the local
 * payment reminder scan, which reads the on-device Room database.
 */
class KhataGoApplication : Application() {

    /** Single, process-wide dependency container (see ARCHITECTURE.md). */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.reminderScheduler.scheduleDailyReminderScan()
    }
}
