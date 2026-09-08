package com.shohan.khatago.core.time

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Publishes "today" as epoch days and keeps it current.
 *
 * Due dates, overdue detection and the dashboard all read from here, so the app
 * rolls over at midnight (and after the device has been asleep) without any
 * manual refresh.
 */
class TodayProvider(scope: CoroutineScope) {

    private val mutableEpochDay = MutableStateFlow(LocalDate.now().toEpochDay())
    val epochDay: StateFlow<Long> = mutableEpochDay

    private var job: Job? = null

    init {
        job = scope.launch(Dispatchers.Default) {
            while (isActive) {
                mutableEpochDay.value = LocalDate.now().toEpochDay()
                delay(30_000L)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
