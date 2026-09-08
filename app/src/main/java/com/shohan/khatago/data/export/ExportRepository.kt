package com.shohan.khatago.data.export

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.shohan.khatago.core.result.Outcome
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.data.backup.BackupSchema
import com.shohan.khatago.domain.model.LedgerEntry
import com.shohan.khatago.domain.model.ReportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate

object ExportFileNames {
    fun csv(today: LocalDate = KhataGoTime.today()): String =
        "KhataGo-Transactions-${today}.csv"

    fun pdf(range: String, today: LocalDate = KhataGoTime.today()): String =
        "KhataGo-Report-${range.replace(" ", "-")}-${today}.pdf"

    fun backup(today: LocalDate = KhataGoTime.today()): String =
        "KhataGo-Backup-${today}${BackupSchema.FILE_EXTENSION}"
}

/**
 * Writes CSV and PDF exports to a location the user chose through the Android
 * Storage Access Framework. Everything happens locally on the device.
 */
class ExportRepository(private val context: Context) {

    suspend fun writeCsv(uri: Uri, entries: List<LedgerEntry>): Outcome<Int> =
        withContext(Dispatchers.IO) {
            try {
                val text = CsvExporter.build(entries)
                val bytes = text.toByteArray(Charsets.UTF_8)
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(bytes)
                    stream.flush()
                } ?: return@withContext Outcome.Failure(MESSAGE_FAILED)
                Outcome.Success(bytes.size)
            } catch (_: IOException) {
                Outcome.Failure(MESSAGE_FAILED)
            } catch (_: SecurityException) {
                Outcome.Failure(MESSAGE_FAILED)
            } catch (_: Exception) {
                Outcome.Failure(MESSAGE_FAILED)
            }
        }

    suspend fun writePdf(uri: Uri, report: ReportData): Outcome<Int> =
        withContext(Dispatchers.IO) {
            var document: PdfDocument? = null
            try {
                document = PdfReportRenderer.render(report)
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    document.writeTo(stream)
                    stream.flush()
                } ?: return@withContext Outcome.Failure(MESSAGE_FAILED)
                Outcome.Success(1)
            } catch (_: IOException) {
                Outcome.Failure(MESSAGE_FAILED)
            } catch (_: SecurityException) {
                Outcome.Failure(MESSAGE_FAILED)
            } catch (_: Exception) {
                Outcome.Failure(MESSAGE_FAILED)
            } finally {
                document?.close()
            }
        }

    companion object {
        const val MESSAGE_FAILED = "We couldn't save the file. Try another location."
    }
}
