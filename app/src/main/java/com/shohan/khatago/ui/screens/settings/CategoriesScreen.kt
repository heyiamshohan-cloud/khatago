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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import Icons.Outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.material.icons.outlined.Delete
import com.shohan.khatago.data.local.db.entity.CategoryEntity
import com.shohan.khatago.data.repository.LedgerRepository
import com.shohan.khatago.ui.components.GroupDivider
import com.shohan.khatago.ui.components.KhataGoCard
import com.shohan.khatago.ui.components.KhataGoEmptyState
import com.shohan.khatago.ui.components.KhataGoPrimaryButton
import com.shohan.khatago.ui.components.KhataGoResultBanner
import com.shohan.khatago.ui.components.KhataGoSegmentedControl
import com.shohan.khatago.ui.components.KhataGoTextField
import com.shohan.khatago.ui.components.KhataGoTopBar
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkPrimary
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.InkTertiary
import com.shohan.khatago.ui.theme.Negative
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceWhite
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CategoryTab(val label: String, val type: String) {
    INCOME("Income", "INCOME"),
    EXPENSE("Expense", "EXPENSE")
}

class CategoriesViewModel(private val repository: LedgerRepository) : ViewModel() {

    private val tab = kotlinx.coroutines.flow.MutableStateFlow(CategoryTab.EXPENSE)

    val currentTab: StateFlow<CategoryTab> = tab
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryTab.EXPENSE)

    val categories: StateFlow<List<CategoryEntity>> = tab
        .flatMapLatest { repository.observeCategories(it.type) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun select(value: CategoryTab) {
        tab.value = value
    }

    fun add(name: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(repository.addCategory(name, tab.value.type))
        }
    }

    fun remove(category: CategoryEntity) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }
}

@Composable
fun CategoriesScreen(
    categories: List<CategoryEntity>,
    tab: CategoryTab,
    onSelectTab: (CategoryTab) -> Unit,
    onAdd: (String, (Boolean) -> Unit) -> Unit,
    onDelete: (CategoryEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

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
                    title = "Categories",
                    onBack = onBack,
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    KhataGoSegmentedControl(
                        options = CategoryTab.entries,
                        selected = tab,
                        onSelected = onSelectTab,
                        label = { it.label }
                    )
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    Text(
                        text = "Add a category",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSecondary,
                        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                    )
                    KhataGoCard(containerColor = SurfaceWhite) {
                        Column(modifier = Modifier.padding(Spacing.CardInner)) {
                            KhataGoTextField(
                                value = draft,
                                onValueChange = { draft = it },
                                label = "Name",
                                placeholder = "e.g. Rent"
                            )
                            Spacer(Modifier.height(Spacing.M))
                            KhataGoPrimaryButton(
                                text = "Add",
                                onClick = {
                                    onAdd(draft) { ok ->
                                        if (ok) {
                                            draft = ""
                                            message = "Category added."
                                            isError = false
                                        } else {
                                            message = "That category already exists."
                                            isError = true
                                        }
                                    }
                                },
                                enabled = draft.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            if (message != null) {
                item {
                    KhataGoResultBanner(
                        message = message ?: "",
                        isError = isError,
                        modifier = Modifier.padding(horizontal = Spacing.Gutter)
                    )
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    Text(
                        text = "${tab.label} categories",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSecondary,
                        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                    )
                }
            }

            if (categories.isEmpty()) {
                item {
                    KhataGoCard(
                        containerColor = SurfaceWhite,
                        modifier = Modifier.padding(horizontal = Spacing.Gutter)
                    ) {
                        KhataGoEmptyState(
                            title = "No categories",
                            message = "Add one to keep your records tidy."
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
                            categories.forEachIndexed { index, category ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Spacing.L, vertical = Spacing.M),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = InkPrimary
                                        )
                                        Text(
                                            text = if (category.isCustom) "Custom" else "Built-in",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = InkTertiary
                                        )
                                    }
                                    IconButton(onClick = { onDelete(category) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Delete ${category.name}",
                                            tint = Negative
                                        )
                                    }
                                }
                                if (index < categories.size - 1) {
                                    GroupDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
