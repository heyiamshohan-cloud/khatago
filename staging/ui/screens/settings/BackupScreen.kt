package com.shohan.khatago.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shohan.khatago.core.result.Outcome
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.data.backup.BackupRepository
import com.shohan.khatago.data.backup.BackupSchema
import com.shohan.khatago.data.export.ExportFileNames
import com.shohan.khatago.ui.components.KhataGoCard
import com.shohan.khatago.ui.components.KhataGoConfirmDialog
import com.shohan.khatago.ui.components.KhataGoInfoTile
import com.shohan.khatago.ui.components.KhataGoPrimaryButton
import com.shohan.khatago.ui.components.KhataGoResultBanner
import com.shohan.khatago.ui.components.KhataGoSecondaryButton
import com.shohan.khatago.ui.components.KhataGoTopBar
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkPrimary
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceWhite

/**
 * Backup and restore. Files are written to and read from wherever the user
 * chooses through the Android Storage Access Framework — KhataGo never uploads
 * anything and has no internet permission at all.
 */
@Composable
fun BackupScreen(
    lastBackupAt: Long?,
    onBack: () -> Unit,
    onCreateBackup: (android.net.Uri) -> Unit,
    onRestoreBackup: (android.net.Uri) -> Unit,
    status: String?,
    statusIsError: Boolean,
    isWorking: Boolean,
    modifier: Modifier = Modifier
) {
    var pendingRestore by remember { mutableStateOf<android.net.Uri?>(null) }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupSchema.MIME_TYPE)
    ) { uri ->
        if (uri != null) onCreateBackup(uri)
    }
    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingRestore = uri
    }

    if (pendingRestore != null) {
        KhataGoConfirmDialog(
            title = "Restore this backup?",
            message = "Your current data may be replaced by the backup. This can't be undone.",
            confirmLabel = "Restore",
            onConfirm = {
                onRestoreBackup(pendingRestore!!)
                pendingRestore = null
            },
            onDismiss = { pendingRestore = null }
        )
    }

    Scaffold(containerColor = CanvasWhite) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.XXXL),
            verticalArrangement = Arrangement.spacedBy(Spacing.CardGap)
        ) {
            item {
                KhataGoTopBar(
                    title = "Backup & Restore",
                    onBack = onBack,
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    KhataGoCard(containerColor = SurfaceWhite) {
                        Column(modifier = Modifier.padding(Spacing.CardInner)) {
                            Text(
                                text = "Create Backup",
                                style = MaterialTheme.typography.titleMedium,
                                color = InkPrimary
                            )
                            Spacer(Modifier.height(Spacing.XS))
                            Text(
                                text = "A complete file with every shop, loan, EMI, personal record, " +
                                    "income, expense and payment — saved where you choose.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary
                            )
                            Spacer(Modifier.height(Spacing.M))
                            Text(
                                text = lastBackupAt?.let {
                                    "Last backup ${KhataGoTime.formatRelativeDate(it.toBackupDate())}"
                                } ?: "No backup yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                            Spacer(Modifier.height(Spacing.L))
                            KhataGoPrimaryButton(
                                text = if (isWorking) "Working..." else "Create Backup",
                                onClick = {
                                    createLauncher.launch(ExportFileNames.backup())
                                },
                                enabled = !isWorking,
                                icon = Icons.Outlined.Backup,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    KhataGoCard(containerColor = SurfaceWhite) {
                        Column(modifier = Modifier.padding(Spacing.CardInner)) {
                            Text(
                                text = "Restore Backup",
                                style = MaterialTheme.typography.titleMedium,
                                color = InkPrimary
                            )
                            Spacer(Modifier.height(Spacing.XS))
                            Text(
                                text = "Pick a KhataGo backup file. It is checked in full before " +
                                    "anything is changed, so a damaged file can't harm your data.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary
                            )
                            Spacer(Modifier.height(Spacing.L))
                            KhataGoSecondaryButton(
                                text = "Restore Backup",
                                onClick = { openLauncher.launch(arrayOf("*/*")) },
                                enabled = !isWorking,
                                icon = Icons.Outlined.Restore,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            if (status != null) {
                item {
                    KhataGoResultBanner(
                        message = status,
                        isError = statusIsError,
                        modifier = Modifier.padding(horizontal = Spacing.Gutter)
                    )
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    KhataGoCard(containerColor = SurfaceWhite) {
                        Column(modifier = Modifier.padding(Spacing.CardInner)) {
                            KhataGoInfoTile(
                                icon = Icons.Outlined.Info,
                                title = "Kept on your device",
                                message = "Backups are ordinary files you control. KhataGo has no " +
                                    "account, no cloud and no internet permission."
                            )
                            KhataGoInfoTile(
                                icon = Icons.Outlined.Info,
                                title = "Versioned",
                                message = "Every backup records its schema and app version, so an " +
                                    "incompatible file is refused instead of half-restored."
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Turns the stored backup timestamp into a calendar date for display. */
internal fun Long.toBackupDate(): java.time.LocalDate =
    java.time.Instant.ofEpochMilli(this)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()

/** Message shown when a backup round trip fails. */
internal fun backupFailureMessage(outcome: Outcome<*>): String =
    (outcome as? Outcome.Failure)?.message ?: BackupRepository.MESSAGE_UNREADABLE
