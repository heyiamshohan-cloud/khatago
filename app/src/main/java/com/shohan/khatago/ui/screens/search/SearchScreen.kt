package com.shohan.khatago.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.data.repository.SearchRepository
import com.shohan.khatago.data.repository.SearchResultItem
import com.shohan.khatago.data.repository.SearchResults
import com.shohan.khatago.ui.components.KhataGoCard
import com.shohan.khatago.ui.components.KhataGoEmptyState
import com.shohan.khatago.ui.components.KhataGoTopBar
import com.shohan.khatago.ui.components.KhataGoTransactionRow
import com.shohan.khatago.ui.components.iconForKind
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkPrimary
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.InkTertiary
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.KhataGoGreenSoft
import com.shohan.khatago.ui.theme.OutlineSoft
import com.shohan.khatago.ui.theme.ShapeTokens
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceWhite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(
    val query: String = "",
    val results: SearchResults = SearchResults("")
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(private val repository: SearchRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    val state: StateFlow<SearchUiState> = query
        .debounce(200)
        .distinctUntilChanged()
        .flatMapLatest { text ->
            if (text.trim().length < 2) {
                flowOf(SearchUiState(text, SearchResults(text.trim())))
            } else {
                kotlinx.coroutines.flow.flow {
                    emit(SearchUiState(text, repository.search(text)))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setQuery(value: String) {
        query.value = value
    }
}

/** Global search across shops, loans, products, people, categories and notes. */
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenAccount: (SearchResultItem) -> Unit,
    onOpenTransaction: (SearchResultItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

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
                    title = "Search",
                    onBack = onBack,
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.Gutter)
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            "Search shops, loans, people, notes...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkTertiary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(ShapeTokens.Medium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KhataGoGreen,
                        unfocusedBorderColor = OutlineSoft,
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite,
                        cursorColor = KhataGoGreen
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = InkTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear",
                                    tint = InkSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                )
            }

            when {
                uiState.query.trim().length < 2 -> {
                    item {
                        KhataGoCard(
                            containerColor = SurfaceWhite,
                            modifier = Modifier.padding(horizontal = Spacing.Gutter)
                        ) {
                            KhataGoEmptyState(
                                title = "Search KhataGo",
                                message = "Type at least two letters to look through shops, loans, " +
                                    "EMIs, people, categories and notes."
                            )
                        }
                    }
                }

                uiState.results.isEmpty() -> {
                    item {
                        KhataGoCard(
                            containerColor = SurfaceWhite,
                            modifier = Modifier.padding(horizontal = Spacing.Gutter)
                        ) {
                            KhataGoEmptyState(
                                title = "No matches",
                                message = "Nothing matches \"${uiState.results.query}\" yet. Try another word."
                            )
                        }
                    }
                }

                else -> {
                    if (uiState.results.accounts.isNotEmpty()) {
                        item {
                            Text(
                                text = "Accounts",
                                style = MaterialTheme.typography.labelMedium,
                                color = InkSecondary,
                                modifier = Modifier.padding(
                                    horizontal = Spacing.Gutter + Spacing.XS,
                                    vertical = Spacing.XS
                                )
                            )
                        }
                        item {
                            KhataGoCard(
                                containerColor = SurfaceWhite,
                                modifier = Modifier.padding(horizontal = Spacing.Gutter)
                            ) {
                                Column {
                                    uiState.results.accounts.forEachIndexed { index, item ->
                                        SearchAccountRow(item = item, onClick = { onOpenAccount(item) })
                                        if (index < uiState.results.accounts.size - 1) {
                                            com.shohan.khatago.ui.components.KhataGoDivider(
                                                modifier = Modifier.padding(horizontal = Spacing.L)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.results.transactions.isNotEmpty()) {
                        item {
                            Text(
                                text = "History",
                                style = MaterialTheme.typography.labelMedium,
                                color = InkSecondary,
                                modifier = Modifier.padding(
                                    horizontal = Spacing.Gutter + Spacing.XS,
                                    vertical = Spacing.XS
                                )
                            )
                        }
                        item {
                            KhataGoCard(
                                containerColor = SurfaceWhite,
                                modifier = Modifier.padding(horizontal = Spacing.Gutter)
                            ) {
                                Column {
                                    uiState.results.transactions.forEachIndexed { index, item ->
                                        SearchHistoryRow(item = item, onClick = { onOpenTransaction(item) })
                                        if (index < uiState.results.transactions.size - 1) {
                                            com.shohan.khatago.ui.components.KhataGoDivider(
                                                modifier = Modifier.padding(horizontal = Spacing.L)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAccountRow(item: SearchResultItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Spacing.L, vertical = Spacing.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(KhataGoGreenSoft, RoundedCornerShape(ShapeTokens.Medium)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconForKind(item.kind),
                contentDescription = null,
                tint = KhataGoGreen,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(Spacing.M))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchHistoryRow(item: SearchResultItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Spacing.L, vertical = Spacing.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(item.subtitle)
                    item.date?.let {
                        if (isNotEmpty()) append(" · ")
                        append(KhataGoTime.formatRelativeDate(it))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        item.amount?.let { amount ->
            Spacer(Modifier.width(Spacing.S))
            Text(
                text = Money.format(amount),
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                color = InkPrimary
            )
        }
    }
}
