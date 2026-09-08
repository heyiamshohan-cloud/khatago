package com.shohan.khatago.domain.finance

import com.shohan.khatago.core.money.Money
import com.shohan.khatago.domain.model.Insight
import com.shohan.khatago.domain.model.InsightTone

/**
 * Local, rule-based financial insights.
 *
 * No external AI service, no cloud call and no fabricated statistic: an insight
 * is only produced when real data from the database supports it, and every
 * amount is formatted from stored minor units.
 */
object InsightEngine {

    data class Input(
        val monthIncome: Long = 0L,
        val monthExpense: Long = 0L,
        val lastMonthIncome: Long = 0L,
        val lastMonthExpense: Long = 0L,
        val overdueCount: Int = 0,
        val overdueTotal: Long = 0L,
        val upcomingCount: Int = 0,
        val upcomingTotal: Long = 0L,
        val largestOutstandingName: String? = null,
        val largestOutstanding: Long = 0L
    )

    fun generate(input: Input): List<Insight> {
        val insights = ArrayList<Insight>(4)

        if (input.overdueCount > 0 && input.overdueTotal > 0L) {
            insights += Insight(
                id = "overdue",
                title = "Overdue payments",
                message = if (input.overdueCount == 1) {
                    "You have 1 overdue payment worth ${Money.format(input.overdueTotal)}."
                } else {
                    "You have ${input.overdueCount} overdue payments worth ${Money.format(input.overdueTotal)}."
                },
                tone = InsightTone.NEGATIVE
            )
        }

        if (input.upcomingCount > 0 && input.upcomingTotal > 0L) {
            insights += Insight(
                id = "upcoming",
                title = "Coming up",
                message = if (input.upcomingCount == 1) {
                    "You have 1 payment due in the next 7 days."
                } else {
                    "You have ${input.upcomingCount} payments due in the next 7 days."
                },
                tone = InsightTone.NEUTRAL
            )
        }

        if (input.lastMonthExpense > 0L && input.monthExpense > input.lastMonthExpense) {
            val difference = input.monthExpense - input.lastMonthExpense
            insights += Insight(
                id = "expense-up",
                title = "Spending is up",
                message = "Your expenses are ${Money.format(difference)} higher than last month.",
                tone = InsightTone.NEGATIVE
            )
        } else if (input.lastMonthExpense > 0L && input.monthExpense in 1..<input.lastMonthExpense) {
            val difference = input.lastMonthExpense - input.monthExpense
            insights += Insight(
                id = "expense-down",
                title = "Spending is down",
                message = "Your expenses are ${Money.format(difference)} lower than last month.",
                tone = InsightTone.POSITIVE
            )
        }

        if (input.monthIncome > 0L && input.monthIncome > input.monthExpense) {
            val kept = ((input.monthIncome - input.monthExpense) * 100L) / input.monthIncome
            insights += Insight(
                id = "net-positive",
                title = "Ahead this month",
                message = "Your income is higher than your expenses, and you've kept $kept% of it.",
                tone = InsightTone.POSITIVE
            )
        } else if (input.monthExpense > input.monthIncome && input.monthIncome > 0L) {
            insights += Insight(
                id = "net-negative",
                title = "Spending ahead of income",
                message = "Your expenses are higher than your income this month.",
                tone = InsightTone.NEGATIVE
            )
        }

        if (!input.largestOutstandingName.isNullOrBlank() && input.largestOutstanding > 0L) {
            insights += Insight(
                id = "largest",
                title = "Largest balance",
                message = "Your largest outstanding balance is ${input.largestOutstandingName} at ${Money.format(input.largestOutstanding)}.",
                tone = InsightTone.NEUTRAL
            )
        }

        return insights.take(4)
    }
}
