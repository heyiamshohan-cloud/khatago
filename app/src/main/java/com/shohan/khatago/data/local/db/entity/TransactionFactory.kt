package com.shohan.khatago.data.local.db.entity

import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.domain.model.RelatedType
import com.shohan.khatago.domain.model.TransactionType
import java.time.LocalDate

/**
 * Central ledger writer.
 *
 * Every financial event in KhataGo produces exactly one ledger row through this
 * factory, which is why the activity timeline, the dashboard and the reports can
 * never disagree with the underlying accounts.
 *
 * Accounting semantics (see FINANCIAL_LOGIC.md):
 *  - a shop credit purchase, a loan taken and an EMI purchase create a liability,
 *    not income;
 *  - a repayment or payment settles a liability, it is not an expense;
 *  - only INCOME and EXPENSE rows feed the income/expense totals.
 */
object TransactionFactory {

    fun create(
        type: TransactionType,
        amount: Long,
        date: LocalDate,
        category: String = "",
        relatedType: RelatedType = RelatedType.NONE,
        relatedId: Long = 0L,
        originType: String = type.name,
        originId: Long = 0L,
        description: String = "",
        notes: String = ""
    ): TransactionEntity {
        val now = KhataGoTime.nowMillis()
        // Entries recorded for "today" carry the real clock time; entries recorded
        // for another day are pinned to noon of that day so ordering stays stable.
        val timestamp = if (date == KhataGoTime.today()) now else KhataGoTime.startOfDay(date) + 12 * 60 * 60 * 1000L
        return TransactionEntity(
            timestamp = timestamp,
            dateEpochDay = date.toEpochDay(),
            yearMonth = date.year * 100 + date.monthValue,
            amount = amount,
            type = type.name,
            category = category,
            relatedType = relatedType.name,
            relatedId = relatedId,
            originType = originType,
            originId = originId,
            description = description,
            notes = notes,
            createdAt = KhataGoTime.nowMillis()
        )
    }

    /** Keeps the timestamp on the entry's own calendar day, ordered by real time. */
    fun createAt(
        type: TransactionType,
        amount: Long,
        date: LocalDate,
        timestamp: Long,
        category: String = "",
        relatedType: RelatedType = RelatedType.NONE,
        relatedId: Long = 0L,
        originType: String = type.name,
        originId: Long = 0L,
        description: String = "",
        notes: String = ""
    ): TransactionEntity = TransactionEntity(
        timestamp = timestamp,
        dateEpochDay = date.toEpochDay(),
        yearMonth = date.year * 100 + date.monthValue,
        amount = amount,
        type = type.name,
        category = category,
        relatedType = relatedType.name,
        relatedId = relatedId,
        originType = originType,
        originId = originId,
        description = description,
        notes = notes,
        createdAt = KhataGoTime.nowMillis()
    )
}
