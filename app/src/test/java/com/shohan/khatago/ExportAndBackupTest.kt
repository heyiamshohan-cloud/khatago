package com.shohan.khatago

import com.google.common.truth.Truth.assertThat
import com.shohan.khatago.data.backup.BackupPayload
import com.shohan.khatago.data.backup.BackupSchema
import com.shohan.khatago.data.export.CsvExporter
import com.shohan.khatago.data.export.ExportFileNames
import com.shohan.khatago.data.local.db.entity.ExpenseEntity
import com.shohan.khatago.data.local.db.entity.IncomeEntity
import com.shohan.khatago.data.local.db.entity.PersonEntity
import com.shohan.khatago.data.local.db.entity.ShopEntity
import com.shohan.khatago.domain.model.LedgerEntry
import com.shohan.khatago.domain.model.RelatedType
import com.shohan.khatago.domain.model.TransactionType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.time.LocalDate

/**
 * Exports and backups are the only way data leaves the device, so their shape is
 * pinned down here: the CSV a spreadsheet has to open, and the JSON a restore
 * has to accept.
 */
class ExportAndBackupTest {

    private fun entry(
        id: Long,
        type: TransactionType,
        amount: Long,
        description: String,
        notes: String = ""
    ) = LedgerEntry(
        id = id,
        timestamp = 1_773_000_000_000L,
        date = LocalDate.of(2026, 3, 5),
        amount = amount,
        type = type,
        category = "Groceries",
        relatedType = RelatedType.SHOP,
        relatedId = 7L,
        description = description,
        notes = notes
    )

    @Test
    fun csv_startsWithABomAndHeaderRow() {
        val csv = CsvExporter.build(emptyList())
        assertThat(csv.startsWith("﻿")).isTrue()
        assertThat(csv.lines().first()).isEqualTo(
            "﻿Date,Time,Type,Category,Description,Amount (BDT),Amount,Related Account,Status,Notes"
        )
    }

    @Test
    fun csv_writesExactDecimalAmounts() {
        val csv = CsvExporter.build(
            listOf(entry(1L, TransactionType.EXPENSE, 125_050L, "Corner shop"))
        )
        val row = csv.lines()[1]
        assertThat(row).contains("1250.50")
        assertThat(row).contains("Expense")
        assertThat(row).contains("SHOP")
    }

    @Test
    fun csv_escapesCommasQuotesAndNewlines() {
        val csv = CsvExporter.build(
            listOf(entry(2L, TransactionType.INCOME, 100_000L, "Rice, oil \"basmati\""))
        )
        assertThat(csv).contains("\"Rice, oil \"\"basmati\"\"\"")
    }

    @Test
    fun csv_classifiesObligationsAndPayments() {
        val csv = CsvExporter.build(
            listOf(
                entry(3L, TransactionType.SHOP_CREDIT, 50_000L, "Shop credit"),
                entry(4L, TransactionType.LOAN_PAYMENT, 50_000L, "Loan payment")
            )
        )
        assertThat(csv).contains("Obligation")
        assertThat(csv).contains("Payment")
    }

    @Test
    fun fileNames_areReadableAndDated() {
        assertThat(ExportFileNames.csv(LocalDate.of(2026, 3, 5)))
            .isEqualTo("KhataGo-Transactions-2026-03-05.csv")
        assertThat(ExportFileNames.pdf("This Month", LocalDate.of(2026, 3, 5)))
            .isEqualTo("KhataGo-Report-This-Month-2026-03-05.pdf")
        assertThat(ExportFileNames.backup(LocalDate.of(2026, 3, 5)))
            .isEqualTo("KhataGo-Backup-2026-03-05" + BackupSchema.FILE_EXTENSION)
    }

    @Test
    fun backupPayload_roundTripsThroughJson() {
        val payload = BackupPayload(
            appVersion = "1.0.0",
            exportedAt = 1_773_000_000_000L,
            people = listOf(PersonEntity(id = 1L, name = "Rahim")),
            shops = listOf(ShopEntity(id = 2L, name = "Rahman Store")),
            income = listOf(
                IncomeEntity(id = 3L, amount = 500_000L, dateEpochDay = 20_500L, yearMonth = 202603)
            ),
            expenses = listOf(
                ExpenseEntity(id = 4L, amount = 25_050L, dateEpochDay = 20_500L, yearMonth = 202603)
            )
        )

        val json = Json { encodeDefaults = true }
        val text = json.encodeToString(payload)
        val restored = json.decodeFromString<BackupPayload>(text)

        assertThat(restored.backupVersion).isEqualTo(BackupSchema.BACKUP_VERSION)
        assertThat(restored.currencyCode).isEqualTo("BDT")
        assertThat(restored.shops.single().name).isEqualTo("Rahman Store")
        assertThat(restored.expenses.single().amount).isEqualTo(25_050L)
        assertThat(restored.people.single().name).isEqualTo("Rahim")
    }

    @Test
    fun backupPayload_keepsMoneyAsIntegersNeverFloats() {
        val json = Json { encodeDefaults = true }
        val text = json.encodeToString(
            BackupPayload(
                expenses = listOf(
                    ExpenseEntity(id = 1L, amount = 250_50L, dateEpochDay = 20_500L)
                )
            )
        )
        assertThat(text).doesNotContain(".5,")
        assertThat(text).contains("25050")
    }
}
