package com.shohan.khatago

import com.google.common.truth.Truth.assertThat
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.domain.finance.InsightEngine
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Time helpers decide every period boundary in the app (months, weeks, report
 * ranges), and insights are the only derived prose on the dashboard.
 */
class TimeAndInsightTest {

    @Test
    fun monthBoundariesCoverTheWholeMonth() {
        assertThat(KhataGoTime.startOfMonth(LocalDate.of(2026, 9, 8)))
            .isEqualTo(LocalDate.of(2026, 9, 1))
        assertThat(KhataGoTime.endOfMonth(LocalDate.of(2026, 2, 10)))
            .isEqualTo(LocalDate.of(2026, 2, 28))
        assertThat(KhataGoTime.endOfMonth(LocalDate.of(2028, 2, 10)))
            .isEqualTo(LocalDate.of(2028, 2, 29))
    }

    @Test
    fun weekStartsOnMonday() {
        val sunday = LocalDate.of(2026, 9, 6)
        assertThat(KhataGoTime.startOfWeek(sunday)).isEqualTo(LocalDate.of(2026, 8, 31))
        assertThat(KhataGoTime.endOfWeek(sunday)).isEqualTo(LocalDate.of(2026, 9, 6))
    }

    @Test
    fun yearBoundariesCoverTheWholeYear() {
        assertThat(KhataGoTime.startOfYear(LocalDate.of(2026, 9, 8)))
            .isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(KhataGoTime.endOfYear(LocalDate.of(2026, 9, 8)))
            .isEqualTo(LocalDate.of(2026, 12, 31))
    }

    @Test
    fun lastMonthRangeSpansThePreviousCalendarMonth() {
        assertThat(KhataGoTime.lastMonthRange(LocalDate.of(2026, 1, 15)))
            .isEqualTo(LocalDate.of(2025, 12, 1) to LocalDate.of(2025, 12, 31))
    }

    @Test
    fun daysBetweenCountsCalendarDays() {
        assertThat(KhataGoTime.daysBetween(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 8)))
            .isEqualTo(7L)
        assertThat(KhataGoTime.daysBetween(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 1)))
            .isEqualTo(-7L)
    }

    @Test
    fun relativeDatesReadAsWords() {
        val today = LocalDate.of(2026, 9, 8)
        assertThat(KhataGoTime.formatRelativeDate(today, today)).isEqualTo("Today")
        assertThat(KhataGoTime.formatRelativeDate(today.minusDays(1), today)).isEqualTo("Yesterday")
        assertThat(KhataGoTime.formatRelativeDate(today.plusDays(1), today)).isEqualTo("Tomorrow")
        assertThat(KhataGoTime.formatRelativeDate(LocalDate.of(2024, 3, 2), today)).isEqualTo("2 Mar 2024")
    }

    @Test
    fun greetingFollowsTheHour() {
        assertThat(KhataGoTime.greeting(LocalDateTime.of(2026, 9, 8, 9, 0)))
            .isEqualTo("Good morning")
        assertThat(KhataGoTime.greeting(LocalDateTime.of(2026, 9, 8, 14, 0)))
            .isEqualTo("Good afternoon")
        assertThat(KhataGoTime.greeting(LocalDateTime.of(2026, 9, 8, 20, 0)))
            .isEqualTo("Good evening")
    }

    @Test
    fun insightsSurfaceOverduePaymentsFirst() {
        val insights = InsightEngine.generate(
            InsightEngine.Input(
                monthIncome = 500_000L,
                monthExpense = 100_000L,
                overdueCount = 2,
                overdueTotal = 30_000L
            )
        )
        assertThat(insights).isNotEmpty()
        assertThat(insights.first().id).isEqualTo("overdue")
        assertThat(insights.first().message).contains("30,000")
    }

    @Test
    fun insightsStayShort() {
        val insights = InsightEngine.generate(
            InsightEngine.Input(
                monthIncome = 1_000_000L,
                monthExpense = 900_000L,
                lastMonthIncome = 900_000L,
                lastMonthExpense = 400_000L,
                overdueCount = 1,
                overdueTotal = 1_000L,
                upcomingCount = 3,
                upcomingTotal = 50_000L,
                largestOutstandingName = "Rahman Store",
                largestOutstanding = 250_000L
            )
        )
        assertThat(insights.size).isAtMost(4)
        assertThat(insights.map { it.id }).containsNoDuplicates()
    }
}
