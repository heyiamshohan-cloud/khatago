package com.shohan.khatago.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Date and time helpers.
 *
 * Schedules are stored as epoch days (a single stable integer per day) and
 * transactions as epoch millis. Everything is computed in the device time zone so
 * "today" always matches what the user sees on their phone.
 */
object KhataGoTime {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun today(): LocalDate = LocalDate.now(zone)
    fun nowMillis(): Long = System.currentTimeMillis()
    fun now(): LocalDateTime = LocalDateTime.now(zone)

    fun LocalDate.toEpochDayLong(): Long = toEpochDay()
    fun Long.toLocalDate(): LocalDate = LocalDate.ofEpochDay(this)
    fun Long.toLocalDateTime(): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zone)

    fun LocalDate.toEpochMillis(): Long = atStartOfDay(zone).toInstant().toEpochMilli()
    fun LocalDate.atTime(time: LocalTime = LocalTime.NOON): Long =
        atTime(time).atZone(zone).toInstant().toEpochMilli()

    // ------------------------------------------------------------------ ranges

    fun startOfDay(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()
    fun endOfDay(date: LocalDate): Long = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    /** Monday-based week start, matching the Reports "This Week" filter. */
    fun startOfWeek(date: LocalDate): LocalDate =
        date.minusDays(((date.dayOfWeek.value + 6) % 7).toLong())

    fun endOfWeek(date: LocalDate): LocalDate = startOfWeek(date).plusDays(6)

    fun startOfMonth(date: LocalDate): LocalDate = date.withDayOfMonth(1)
    fun endOfMonth(date: LocalDate): LocalDate =
        date.withDayOfMonth(date.lengthOfMonth())

    fun startOfYear(date: LocalDate): LocalDate = date.withDayOfYear(1)
    fun endOfYear(date: LocalDate): LocalDate = date.withDayOfYear(date.lengthOfYear())

    fun lastMonthRange(date: LocalDate): Pair<LocalDate, LocalDate> {
        val previous = startOfMonth(date).minusDays(1)
        return startOfMonth(previous) to endOfMonth(previous)
    }

    // -------------------------------------------------------------- formatting

    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val monthYear = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
    private val time12h = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    fun formatDate(date: LocalDate): String = date.format(dayMonthYear)
    fun formatShortDate(date: LocalDate): String = date.format(dayMonth)
    fun formatMonth(date: LocalDate): String = date.format(monthYear)
    fun formatTime(millis: Long): String = millis.toLocalDateTime().format(time12h)
    fun formatDateTime(millis: Long): String =
        "${formatDate(millis.toLocalDateTime().toLocalDate())} · ${formatTime(millis)}"

    /** "Today", "Yesterday", or "12 Sep 2026". */
    fun formatRelativeDate(date: LocalDate, today: LocalDate = today()): String =
        when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            today.plusDays(1) -> "Tomorrow"
            else -> if (date.year == today.year) date.format(dayMonth) else date.format(dayMonthYear)
        }

    /** Timeline header used by the activity history: "Today", "Yesterday", "12 Sep 2026". */
    fun formatTimelineHeader(date: LocalDate, today: LocalDate = today()): String =
        when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> if (date.year == today.year) date.format(dayMonth) else date.format(dayMonthYear)
        }

    fun greeting(dateTime: LocalDateTime = now()): String = when (dateTime.hour) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    /** "Wednesday, 8 September 2026" — dashboard sub-header. */
    private val fullDate = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)
    fun formatFullDate(date: LocalDate = today()): String = date.format(fullDate)

    fun daysBetween(from: LocalDate, to: LocalDate): Long =
        java.time.temporal.ChronoUnit.DAYS.between(from, to)
}
