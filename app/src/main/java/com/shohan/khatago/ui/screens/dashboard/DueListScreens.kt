package com.shohan.khatago.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.data.repository.DashboardRepository
import com.shohan.khatago.domain.model.AccountKind
import com.shohan.khatago.domain.model.DueState
import com.shohan.khatago.domain.model.UpcomingItem
import com.shohan.khatago.ui.components.KhataGoCard
import com.shohan.khatago.ui.components.KhataGoEmptyState
import com.shohan.khatago.ui.components.KhataGoTopBar
import com.shohan.khatago.ui.components.KhataGoUpcomingRow
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceWhite
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class DueListKind { UPCOMING, OVERDUE }

data class DueListUiState(
    val items: List<UpcomingItem> = emptyList(),
    val total: Long = 0L
)

class DueListViewModel(
    repository: DashboardRepository,
    kind: DueListKind
) : ViewModel() {

    val state: StateFlow<DueListUiState> = repository.observeDashboard()
        .map { snapshot ->
            val items = snapshot.upcoming.filter { item ->
                if (kind == DueListKind.OVERDUE) {
                    item.dueState == DueState.OVERDUE
                } else {
                    item.dueState != DueState.OVERDUE
                }
            }
            DueListUiState(items = items, total = items.sumOf { it.remaining })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DueListUiState())
}

/**
 * The full "what needs paying" list behind the dashboard strip. Every row opens
 * the record it belongs to, so the user can act immediately.
 */
@Composable
fun DueListScreen(
    kind: DueListKind,
    items: List<UpcomingItem>,
    total: Long,
    onBack: () -> Unit,
    onOpen: (AccountKind, Long) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    title = if (kind == DueListKind.OVERDUE) "Overdue" else "Upcoming",
                    subtitle = if (items.isEmpty()) {
                        "Nothing here"
                    } else {
                        "${items.size} · ${Money.format(total)}"
                    },
                    onBack = onBack,
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            if (items.isEmpty()) {
                item {
                    KhataGoCard(
                        containerColor = SurfaceWhite,
                        modifier = Modifier.padding(horizontal = Spacing.Gutter)
                    ) {
                        KhataGoEmptyState(
                            title = if (kind == DueListKind.OVERDUE) "No overdue payments" else "Nothing due soon",
                            message = if (kind == DueListKind.OVERDUE) {
                                "You're all caught up. Nice work."
                            } else {
                                "Payments due in the coming days will appear here."
                            }
                        )
                    }
                }
            } else {
                item {
                    KhataGoCard(
                        containerColor = SurfaceWhite,
                        modifier = Modifier.padding(horizontal = Spacing.Gutter)
                    ) {
                        Column {
                            items.forEachIndexed { index, item ->
                                KhataGoUpcomingRow(
                                    dateLabel = KhataGoTime.formatRelativeDate(item.dueDate),
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    amount = item.remaining,
                                    dueState = item.dueState,
                                    onClick = { onOpen(item.kind, item.refId) }
                                )
                                if (index < items.size - 1) {
                                    com.shohan.khatago.ui.components.KhataGoDivider(
                                        modifier = Modifier.padding(horizontal = Spacing.L)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Tap any payment to open its record.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = InkSecondary,
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }
        }
    }
}
