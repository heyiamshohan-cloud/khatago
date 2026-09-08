package com.shohan.khatago.ui.screens.transactions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.domain.model.LedgerEntry
import com.shohan.khatago.core.result.Outcome
import com.shohan.khatago.core.result.onFailure
import com.shohan.khatago.data.local.db.entity.CategoryEntity
import com.shohan.khatago.data.local.db.entity.ExpenseEntity
import com.shohan.khatago.data.local.db.entity.IncomeEntity
import com.shohan.khatago.data.repository.LedgerRepository
import com.shohan.khatago.ui.components.KhataGoAmountField
import com.shohan.khatago.ui.components.KhataGoCategorySheet
import com.shohan.khatago.ui.components.KhataGoDateField
import com.shohan.khatago.ui.components.KhataGoFormScaffold
import com.shohan.khatago.ui.components.KhataGoFormSection
import com.shohan.khatago.ui.components.KhataGoSelectField
import com.shohan.khatago.ui.components.KhataGoTextField
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class IncomeExpenseUiState(
    val income: IncomeEntity? = null,
    val expense: ExpenseEntity? = null,
    val categories: List<CategoryEntity> = emptyList()
)

class IncomeExpenseFormViewModel(
    private val repository: LedgerRepository,
    private val type: String,
    private val entryId: Long?
) : ViewModel() {

    val state: StateFlow<IncomeExpenseUiState> = combine(
        repository.observeCategories(type),
        kotlinx.coroutines.flow.flow {
            val entry = entryId?.let {
                when (type) {
                    "INCOME" -> repository.getIncome(it)
                    else -> repository.getExpense(it)
                }
            }
            emit(entry)
        }
    ) { categories, entry ->
        IncomeExpenseUiState(
            income = entry as? IncomeEntity,
            expense = entry as? ExpenseEntity,
            categories = categories
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IncomeExpenseUiState())

    fun saveIncome(
        amount: Long?,
        date: LocalDate,
        source: String,
        category: String,
        notes: String,
        onResult: (Outcome<Long>) -> Unit
    ) {
        viewModelScope.launch {
            if (amount == null || amount <= 0L) {
                onResult(Outcome.Failure("Enter a valid amount."))
                return@launch
            }
            runCatching {
                repository.saveIncome(entryId, amount, date, source, category, notes)
            }.onSuccess { onResult(Outcome.Success(it)) }
                .onFailure { onResult(Outcome.Failure("Couldn't save. Please try again.")) }
        }
    }

    fun saveExpense(
        amount: Long?,
        date: LocalDate,
        category: String,
        place: String,
        notes: String,
        onResult: (Outcome<Long>) -> Unit
    ) {
        viewModelScope.launch {
            if (amount == null || amount <= 0L) {
                onResult(Outcome.Failure("Enter a valid amount."))
                return@launch
            }
            runCatching {
                repository.saveExpense(entryId, amount, date, category, place, notes)
            }.onSuccess { onResult(Outcome.Success(it)) }
                .onFailure { onResult(Outcome.Failure("Couldn't save. Please try again.")) }
        }
    }
}

/** Add or edit an income entry. */
@Composable
fun AddIncomeScreen(
    income: IncomeEntity?,
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onSave: (Long?, LocalDate, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var amount by remember(income?.id) {
        mutableStateOf(income?.amount?.let { Money.toDecimal(it).toPlainString() } ?: "")
    }
    var date by remember(income?.id) {
        mutableStateOf(income?.let { LocalDate.ofEpochDay(it.dateEpochDay) } ?: LocalDate.now())
    }
    var source by remember(income?.id) { mutableStateOf(income?.source ?: "") }
    var category by remember(income?.id) { mutableStateOf(income?.category ?: "") }
    var notes by remember(income?.id) { mutableStateOf(income?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    val amountValue = remember(amount) { Money.parse(amount) }
    val names = remember(categories) { categories.map { it.name } }

    KhataGoFormScaffold(
        title = if (income == null) "Add Income" else "Edit Income",
        onBack = onBack,
        saveLabel = if (income == null) "Add Income" else "Save",
        error = error,
        onSave = {
            when {
                amountValue == null || amountValue <= 0L -> error = "Enter a valid amount."
                category.isBlank() -> error = "Choose a category."
                else -> onSave(amountValue, date, source, category, notes)
            }
        }
    ) {
        KhataGoFormSection(title = "Amount") {
            KhataGoAmountField(
                value = amount,
                onValueChange = { amount = it; error = null },
                label = "Amount",
                hint = "e.g. 1000"
            )
        }
        KhataGoFormSection(title = "Details") {
            KhataGoDateField(value = date, onValueChange = { date = it }, label = "Date")
            Spacer(Modifier.height(Spacing.L))
            KhataGoTextField(
                value = source,
                onValueChange = { source = it },
                label = "Source",
                placeholder = "e.g. Salary, Shop, Client"
            )
            Spacer(Modifier.height(Spacing.L))
            KhataGoCategorySheet(
                options = names,
                selected = category,
                label = "Category",
                onSelected = { category = it }
            )
        }
        KhataGoFormSection(title = "Notes") {
            KhataGoTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Notes",
                placeholder = "Optional",
                singleLine = false,
                imeAction = ImeAction.Done
            )
        }
    }
}

/** Add or edit an expense entry. */
@Composable
fun AddExpenseScreen(
    expense: ExpenseEntity?,
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onSave: (Long?, LocalDate, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var amount by remember(expense?.id) {
        mutableStateOf(expense?.amount?.let { Money.toDecimal(it).toPlainString() } ?: "")
    }
    var date by remember(expense?.id) {
        mutableStateOf(expense?.let { LocalDate.ofEpochDay(it.dateEpochDay) } ?: LocalDate.now())
    }
    var category by remember(expense?.id) { mutableStateOf(expense?.category ?: "") }
    var place by remember(expense?.id) { mutableStateOf(expense?.place ?: "") }
    var notes by remember(expense?.id) { mutableStateOf(expense?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    val amountValue = remember(amount) { Money.parse(amount) }
    val names = remember(categories) { categories.map { it.name } }

    KhataGoFormScaffold(
        title = if (expense == null) "Add Expense" else "Edit Expense",
        onBack = onBack,
        saveLabel = if (expense == null) "Add Expense" else "Save",
        error = error,
        onSave = {
            when {
                amountValue == null || amountValue <= 0L -> error = "Enter a valid amount."
                category.isBlank() -> error = "Choose a category."
                else -> onSave(amountValue, date, category, place, notes)
            }
        }
    ) {
        KhataGoFormSection(title = "Amount") {
            KhataGoAmountField(
                value = amount,
                onValueChange = { amount = it; error = null },
                label = "Amount",
                hint = "e.g. 250"
            )
        }
        KhataGoFormSection(title = "Details") {
            KhataGoDateField(value = date, onValueChange = { date = it }, label = "Date")
            Spacer(Modifier.height(Spacing.L))
            KhataGoCategorySheet(
                options = names,
                selected = category,
                label = "Category",
                onSelected = { category = it }
            )
            Spacer(Modifier.height(Spacing.L))
            KhataGoTextField(
                value = place,
                onValueChange = { place = it },
                label = "Place / Merchant",
                placeholder = "Optional, e.g. Corner shop"
            )
        }
        KhataGoFormSection(title = "Notes") {
            KhataGoTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Notes",
                placeholder = "Optional",
                singleLine = false,
                imeAction = ImeAction.Done
            )
        }
    }
}

// ------------------------------------------------------------------ detail

/**
 * Loads one ledger line by id. Used for entries whose parent record can no
 * longer be resolved, so a tap on history always shows the user something real.
 */
class TransactionDetailViewModel(
    private val repository: LedgerRepository,
    private val entryId: Long
) : ViewModel() {

    suspend fun load(): LedgerEntry? = repository
        .observeTransactionsBetween(0L, 10_000_000L, limit = 10_000)
        .first()
        .firstOrNull { it.id == entryId }
}

/**
 * Read-only detail for any ledger line, with edit and delete when the entry
 * can be changed. Entries produced by another module (a loan installment, a
 * shop purchase) are managed inside that module instead.
 */
@Composable
fun TransactionDetailScreen(
    title: String,
    subtitle: String,
    amount: Long,
    amountIsIncome: Boolean,
    rows: List<Pair<String, String>>,
    canEdit: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        com.shohan.khatago.ui.components.KhataGoConfirmDialog(
            title = "Delete this entry?",
            message = "It will be removed from your history and reports.",
            confirmLabel = "Delete",
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false }
        )
    }

    Scaffold(containerColor = CanvasWhite) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(horizontal = Spacing.Gutter, vertical = Spacing.L)
        ) {
            com.shohan.khatago.ui.components.KhataGoTopBar(
                title = title,
                subtitle = subtitle,
                onBack = onBack,
                actions = {
                    if (canEdit) {
                        com.shohan.khatago.ui.components.KhataGoTextButton(
                            text = "Edit",
                            onClick = onEdit
                        )
                        com.shohan.khatago.ui.components.KhataGoTextButton(
                            text = "Delete",
                            onClick = { confirmDelete = true },
                            color = com.shohan.khatago.ui.theme.Negative
                        )
                    }
                }
            )
            Spacer(Modifier.height(Spacing.L))
            Text(
                text = Money.format(amount),
                style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                color = if (amountIsIncome) com.shohan.khatago.ui.theme.Positive else com.shohan.khatago.ui.theme.InkPrimary
            )
            Spacer(Modifier.height(Spacing.L))
            com.shohan.khatago.ui.components.KhataGoCard(
                containerColor = com.shohan.khatago.ui.theme.SurfaceWhite
            ) {
                Column(modifier = Modifier.padding(Spacing.CardInner)) {
                    rows.forEach { (label, value) ->
                        com.shohan.khatago.ui.components.KhataGoKeyValueRow(label = label, value = value)
                    }
                }
            }
            if (!canEdit) {
                Spacer(Modifier.height(Spacing.M))
                Text(
                    text = "This entry was created by another record. Open that record to edit or delete it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
        }
    }
}
