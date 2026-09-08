package com.shohan.khatago.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.data.repository.AppSettings
import com.shohan.khatago.data.repository.SettingsRepository
import com.shohan.khatago.ui.components.GroupDivider
import com.shohan.khatago.ui.components.KhataGoGroupCard
import com.shohan.khatago.ui.components.KhataGoSettingsRow
import com.shohan.khatago.ui.components.KhataGoSwitchRow
import com.shohan.khatago.ui.components.KhataGoTopBar
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.InkTertiary
import com.shohan.khatago.ui.theme.Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(repository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
}

/**
 * Settings: grouped, quiet and obvious. Nothing here is a paywall — every
 * feature in KhataGo is free and always will be.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onAppLock: () -> Unit,
    onRemindersChanged: (Boolean) -> Unit,
    onCategories: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(containerColor = CanvasWhite) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.XXXL),
            verticalArrangement = Arrangement.spacedBy(Spacing.XL)
        ) {
            item {
                KhataGoTopBar(
                    title = "Settings",
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            item {
                SettingsGroup(title = "Data & Backup", modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    KhataGoSettingsRow(
                        title = "Create Backup",
                        subtitle = settings.lastBackupAt?.let { millis ->
                            "Last backup ${KhataGoTime.formatRelativeDate(millis.toBackupDate())}"
                        } ?: "Save everything to a file you choose",
                        onClick = onBackup,
                        trailing = { RowIcon(Icons.Outlined.Backup) }
                    )
                    GroupDivider()
                    KhataGoSettingsRow(
                        title = "Restore Backup",
                        subtitle = "Replace current data with a backup file",
                        onClick = onRestore,
                        trailing = { RowIcon(Icons.Outlined.Restore) }
                    )
                }
            }

            item {
                SettingsGroup(title = "Reports & Export", modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    KhataGoSettingsRow(
                        title = "Export CSV",
                        subtitle = "Open your transactions in Excel or Google Sheets",
                        onClick = onExportCsv,
                        trailing = { RowIcon(Icons.Outlined.TableChart) }
                    )
                    GroupDivider()
                    KhataGoSettingsRow(
                        title = "Export PDF",
                        subtitle = "A printable report for the selected period",
                        onClick = onExportPdf,
                        trailing = { RowIcon(Icons.Outlined.Description) }
                    )
                }
            }

            item {
                SettingsGroup(title = "Security", modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    KhataGoSettingsRow(
                        title = "App Lock",
                        subtitle = if (settings.hasPin && settings.appLockEnabled) {
                            if (settings.biometricEnabled) "PIN and biometric on" else "PIN on"
                        } else {
                            "Require a PIN to open KhataGo"
                        },
                        onClick = onAppLock,
                        trailing = { RowIcon(Icons.Outlined.Lock) }
                    )
                }
            }

            item {
                SettingsGroup(title = "Notifications", modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    Column(modifier = Modifier.padding(horizontal = Spacing.L)) {
                        KhataGoSwitchRow(
                            title = "Payment reminders",
                            subtitle = "A daily check for upcoming and overdue payments",
                            checked = settings.remindersEnabled,
                            onCheckedChange = onRemindersChanged
                        )
                    }
                }
            }

            item {
                SettingsGroup(title = "Categories", modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    KhataGoSettingsRow(
                        title = "Manage categories",
                        subtitle = "Add or remove income and expense categories",
                        onClick = onCategories,
                        trailing = { RowIcon(Icons.Outlined.Category) }
                    )
                }
            }

            item {
                SettingsGroup(title = "About KhataGo", modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    KhataGoSettingsRow(
                        title = "About",
                        subtitle = "Version 1.0.0 · Free forever",
                        onClick = onAbout,
                        trailing = { RowIcon(Icons.Outlined.Info) }
                    )
                }
            }

            item {
                Text(
                    text = "KhataGo never shows ads, never sells data and never needs an account. " +
                        "Your records stay on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkTertiary,
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }
        }
    }
}

@Composable
private fun RowIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = InkSecondary,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = InkSecondary,
            modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
        )
        KhataGoGroupCard(content = content)
    }
}

/** Turns the stored backup timestamp into a calendar date for display. */
private fun Long.toBackupDate(): java.time.LocalDate =
    java.time.Instant.ofEpochMilli(this)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
