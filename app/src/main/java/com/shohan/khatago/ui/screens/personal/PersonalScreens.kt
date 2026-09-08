package com.shohan.khatago.ui.screens.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.data.local.db.entity.PersonalDebtEntity
import com.shohan.khatago.data.local.db.entity.PersonalLendingEntity
import com.shohan.khatago.data.local.db.entity.PersonalRepaymentEntity
import com.shohan.khatago.data.local.db.entity.PersonalReturnEntity
import com.shohan.khatago.data.repository.BorrowedDetail
import com.shohan.khatago.data.repository.BorrowedInput
import com.shohan.khatago.data.repository.LentDetail
import com.shohan.khatago.data.repository.LentInput
import com.shohan.khatago.data.repository.PaymentResult
import com.shohan.khatago.data.repository.PersonalRepository
import com.shohan.khatago.domain.model.DueState
import com.shohan.khatago.domain.model.PaymentMethod
import com.shohan.khatago.ui.components.BadgeTone
import com.shohan.khatago.ui.components.KhataGoAmountField
import com.shohan.khatago.ui.components.KhataGoCard
import com.shohan.khatago.ui.components.KhataGoChip
import com.shohan.khatago.ui.components.KhataGoConfirmDialog
import com.shohan.khatago.ui.components.KhataGoDateField
import com.shohan.khatago.ui.components.KhataGoEmptyState
import com.shohan.khatago.ui.components.KhataGoFormScaffold
import com.shohan.khatago.ui.components.KhataGoFormSection
import com.shohan.khatago.ui.components.KhataGoIconButton
import com.shohan.khatago.ui.components.KhataGoKeyValueRow
import com.shohan.khatago.ui.components.KhataGoOptionalDateField
import com.shohan.khatago.ui.components.KhataGoProgress
import com.shohan.khatago.ui.components.KhataGoSectionHeader
import com.shohan.khatago.ui.components.KhataGoSelectField
import com.shohan.khatago.ui.components.KhataGoStatusBadge
import com.shohan.khatago.ui.components.KhataGoSummaryStrip
import com.shohan.khatago.ui.components.KhataGoTextField
import com.shohan.khatago.ui.components.KhataGoTopBar
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkPrimary
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.InkTertiary
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.KhataGoGreenSoft
import com.shohan.khatago.ui.theme.Negative
import com.shohan.khatago.ui.theme.OnHero
import com.shohan.khatago.ui.theme.Positive
import com.shohan.khatago.ui.theme.ShapeTokens
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceWhite
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

// ----------------------------------------------------------------- borrowed

class BorrowedDetailViewModel(
    private val repository: PersonalRepository,
    private val debtId: Long
) : ViewModel() {

    val detail: StateFlow<BorrowedDetail> = repository.observeBorrowedDetail(debtId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BorrowedDetail())

    fun deleteDebt(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteDebt(debtId)
            onDone()
        }
    }

    fun deleteRepayment(repaymentId: Long) {
        viewModelScope.launch { repository.deleteRepayment(repaymentId) }
    }
}

class LentDetailViewModel(
    private val repository: PersonalRepository,
    private val lendingId: Long
) : ViewModel() {

    val detail: StateFlow<LentDetail> = repository.observeLentDetail(lendingId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LentDetail())

    fun deleteLending(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteLending(lendingId)
            onDone()
        }
    }

    fun deleteReturn(returnId: Long) {
        viewModelScope.launch { repository.deleteReturn(returnId) }
    }
}

/**
 * Personal record detail. Borrowed money is money the user must pay back; lent
 * money is money the user expects back. Both support many partial payments.
 */
@Composable
fun BorrowedDetailScreen(
    detail: BorrowedDetail,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddRepayment: () -> Unit,
    onEditRepayment: (Long) -> Unit,
    onDeleteRepayment: (Long) -> Unit,
    onDeleteRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    PersonalDetailScaffold(
        isLent = false,
        title = detail.person?.name ?: "Borrowed",
        subtitle = "Money Borrowed",
        remaining = detail.remaining,
        progressPercent = detail.progressPercent,
        dueState = detail.dueState,
        rows = listOf(
            "Borrowed amount" to Money.format(detail.debt?.amount ?: 0L),
            "Paid back" to Money.format(detail.paidTotal),
            "Borrowed on" to detail.debt?.let {
                KhataGoTime.formatDate(LocalDate.ofEpochDay(it.borrowedDateEpochDay))
            }.orEmpty(),
            "Expected return" to detail.debt?.expectedReturnDateEpochDay?.let {
                KhataGoTime.formatDate(LocalDate.ofEpochDay(it))
            }.orEmpty()
        ),
        payments = detail.repayments.map {
            PaymentLine(it.id, it.amount, it.dateEpochDay, it.method, it.notes)
        },
        emptyTitle = "No repayments yet",
        emptyMessage = "Add the first amount you paid back.",
        historyTitle = "Repayments",
        onBack = onBack,
        onEdit = onEdit,
        onAddPayment = onAddRepayment,
        onEditPayment = onEditRepayment,
        onDeletePayment = onDeleteRepayment,
        onDeleteRecord = onDeleteRecord,
        modifier = modifier
    )
}

@Composable
fun LentDetailScreen(
    detail: LentDetail,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddReturn: () -> Unit,
    onEditReturn: (Long) -> Unit,
    onDeleteReturn: (Long) -> Unit,
    onDeleteRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    PersonalDetailScaffold(
        isLent = true,
        title = detail.person?.name ?: "Lent",
        subtitle = "Money Lent",
        remaining = detail.remaining,
        progressPercent = detail.progressPercent,
        dueState = detail.dueState,
        rows = listOf(
            "Lent amount" to Money.format(detail.lending?.amount ?: 0L),
            "Received back" to Money.format(detail.receivedTotal),
            "Lent on" to detail.lending?.let {
                KhataGoTime.formatDate(LocalDate.ofEpochDay(it.lentDateEpochDay))
            }.orEmpty(),
            "Expected return" to detail.lending?.expectedReturnDateEpochDay?.let {
                KhataGoTime.formatDate(LocalDate.ofEpochDay(it))
            }.orEmpty()
        ),
        payments = detail.returns.map {
            PaymentLine(it.id, it.amount, it.dateEpochDay, it.method, it.notes)
        },
        emptyTitle = "No returns yet",
        emptyMessage = "Add the first amount you received back.",
        historyTitle = "Returns",
        onBack = onBack,
        onEdit = onEdit,
        onAddPayment = onAddReturn,
        onEditPayment = onEditReturn,
        onDeletePayment = onDeleteReturn,
        onDeleteRecord = onDeleteRecord,
        modifier = modifier
    )
}

internal data class PaymentLine(
    val id: Long,
    val amount: Long,
    val dateEpochDay: Long,
    val method: String,
    val notes: String
)

@Composable
private fun PersonalDetailScaffold(
    isLent: Boolean,
    title: String,
    subtitle: String,
    remaining: Long,
    progressPercent: Int,
    dueState: DueState,
    rows: List<Pair<String, String>>,
    payments: List<PaymentLine>,
    emptyTitle: String,
    emptyMessage: String,
    historyTitle: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddPayment: () -> Unit,
    onEditPayment: (Long) -> Unit,
    onDeletePayment: (Long) -> Unit,
    onDeleteRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pending by remember { mutableStateOf<Long?>(null) }
    val target = pending
    if (target != null) {
        KhataGoConfirmDialog(
            title = "Delete this payment?",
            message = "The balance will be recalculated without it.",
            confirmLabel = "Delete",
            onConfirm = {
                onDeletePayment(target)
                pending = null
            },
            onDismiss = { pending = null }
        )
    }

    Scaffold(
        containerColor = CanvasWhite,
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = onAddPayment,
                containerColor = KhataGoGreen,
                contentColor = OnHero,
                shape = RoundedCornerShape(ShapeTokens.Medium)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add payment")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.XXXL),
            verticalArrangement = Arrangement.spacedBy(Spacing.CardGap)
        ) {
            item {
                KhataGoTopBar(
                    title = title,
                    subtitle = subtitle,
                    onBack = onBack,
                    actions = {
                        KhataGoIconButton(
                            icon = Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            onClick = onEdit
                        )
                        KhataGoIconButton(
                            icon = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            onClick = onDeleteRecord
                        )
                    },
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            item {
                KhataGoCard(
                    containerColor = SurfaceWhite,
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                ) {
                    Column(modifier = Modifier.padding(Spacing.CardInner)) {
                        Text(
                            text = if (isLent) "Amount to receive" else "Amount to pay",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSecondary
                        )
                        Text(
                            text = Money.format(remaining),
                            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                            color = InkPrimary
                        )
                        Text(
                            text = "$progressPercent% settled",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                        Spacer(Modifier.height(Spacing.M))
                        KhataGoProgress(progress = progressPercent / 100f)
                        Spacer(Modifier.height(Spacing.L))
                        rows.forEach { (label, value) ->
                            if (value.isNotBlank()) {
                                KhataGoKeyValueRow(label = label, value = value)
                            }
                        }
                        if (dueState == DueState.OVERDUE) {
                            KhataGoKeyValueRow(
                                label = "Status",
                                value = "Overdue",
                                valueColor = Negative
                            )
                        } else if (dueState == DueState.DUE_TODAY) {
                            KhataGoKeyValueRow(
                                label = "Status",
                                value = "Due today",
                                valueColor = com.shohan.khatago.ui.theme.Upcoming
                            )
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    KhataGoSectionHeader(title = historyTitle)
                }
            }

            if (payments.isEmpty()) {
                item {
                    KhataGoCard(
                        containerColor = SurfaceWhite,
                        modifier = Modifier.padding(horizontal = Spacing.Gutter)
                    ) {
                        KhataGoEmptyState(title = emptyTitle, message = emptyMessage)
                    }
                }
            } else {
                item {
                    KhataGoCard(
                        containerColor = SurfaceWhite,
                        modifier = Modifier.padding(horizontal = Spacing.Gutter)
                    ) {
                        Column {
                            payments.forEachIndexed { index, payment ->
                                PaymentRow(
                                    payment = payment,
                                    onEdit = { onEditPayment(payment.id) },
                                    onDelete = { pending = payment.id }
                                )
                                if (index < payments.size - 1) {
                                    com.shohan.khatago.ui.components.KhataGoDivider(
                                        modifier = Modifier.padding(horizontal = Spacing.L)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (progressPercent == 100) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.Gutter),
                        contentAlignment = Alignment.Center
                    ) {
                        KhataGoStatusBadge(
                            text = if (isLent) "Fully received" else "Fully repaid",
                            tone = BadgeTone.POSITIVE
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentRow(
    payment: PaymentLine,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.L, vertical = Spacing.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(KhataGoGreenSoft, RoundedCornerShape(ShapeTokens.Medium)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Payments,
                contentDescription = null,
                tint = KhataGoGreen,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(Spacing.M))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = Money.format(payment.amount),
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary
            )
            Text(
                text = buildString {
                    append(KhataGoTime.formatDate(LocalDate.ofEpochDay(payment.dateEpochDay)))
                    append(" · ")
                    append(payment.method.lowercase().replaceFirstChar { it.uppercase() })
                    if (payment.notes.isNotBlank()) {
                        append(" · ")
                        append(payment.notes)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        KhataGoIconButton(icon = Icons.Outlined.Edit, contentDescription = "Edit", onClick = onEdit)
        KhataGoIconButton(
            icon = Icons.Outlined.Delete,
            contentDescription = "Delete",
            onClick = onDelete,
            tint = Negative
        )
    }
}

// ------------------------------------------------------------------- forms

class BorrowedFormViewModel(
    private val repository: PersonalRepository,
    private val debtId: Long?
) : ViewModel() {

    val debt: StateFlow<PersonalDebtEntity?> =
        (debtId?.let { repository.observeDebt(it) } ?: kotlinx.coroutines.flow.flowOf(null))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun personName(): String =
        debtId?.let { repository.getDebt(it) }?.personId?.let { repository.getPerson(it)?.name }.orEmpty()

    fun save(
        name: String,
        amount: Long,
        date: LocalDate,
        expectedReturnDate: LocalDate?,
        notes: String,
        onSaved: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val currentPersonId = debtId?.let { repository.getDebt(it)?.personId }
            val personId = repository.savePerson(currentPersonId, name, "", "", "")
            val id = repository.saveBorrowed(
                BorrowedInput(
                    id = debtId,
                    personId = if (personId == 0L) currentPersonId ?: 0L else personId,
                    amount = amount,
                    borrowedDate = date,
                    expectedReturnDate = expectedReturnDate,
                    notes = notes
                )
            )
            onSaved(id)
        }
    }
}

class LentFormViewModel(
    private val repository: PersonalRepository,
    private val lendingId: Long?
) : ViewModel() {

    val lending: StateFlow<PersonalLendingEntity?> =
        (lendingId?.let { repository.observeLentDetail(it).map { detail -> detail.lending } }
            ?: kotlinx.coroutines.flow.flowOf(null))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun personName(): String =
        lendingId?.let { repository.getLending(it) }?.personId?.let { repository.getPerson(it)?.name }
            .orEmpty()

    fun save(
        name: String,
        amount: Long,
        date: LocalDate,
        expectedReturnDate: LocalDate?,
        notes: String,
        onSaved: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val currentPersonId = lendingId?.let { repository.getLending(it)?.personId }
            val personId = repository.savePerson(currentPersonId, name, "", "", "")
            val id = repository.saveLent(
                LentInput(
                    id = lendingId,
                    personId = if (personId == 0L) currentPersonId ?: 0L else personId,
                    amount = amount,
                    lentDate = date,
                    expectedReturnDate = expectedReturnDate,
                    notes = notes
                )
            )
            onSaved(id)
        }
    }
}

/** Shared form for borrowed and lent records: short, warm and quick to fill. */
@Composable
fun BorrowedFormScreen(
    initialName: String,
    initialAmount: String,
    initialDate: LocalDate,
    initialExpected: LocalDate?,
    initialNotes: String,
    loaded: Boolean,
    isEdit: Boolean,
    onBack: () -> Unit,
    onSave: (String, Long, LocalDate, LocalDate?, String) -> Unit,
    modifier: Modifier = Modifier
) {
    PersonalFormScaffold(
        title = if (isEdit) "Edit Money Borrowed" else "Add Money Borrowed",
        nameLabel = "Who did you borrow from",
        initialName = initialName,
        initialAmount = initialAmount,
        initialDate = initialDate,
        initialExpected = initialExpected,
        initialNotes = initialNotes,
        loaded = loaded,
        isEdit = isEdit,
        saveLabel = if (isEdit) "Save" else "Add Record",
        onBack = onBack,
        onSave = onSave
    )
}

@Composable
fun LentFormScreen(
    initialName: String,
    initialAmount: String,
    initialDate: LocalDate,
    initialExpected: LocalDate?,
    initialNotes: String,
    loaded: Boolean,
    isEdit: Boolean,
    onBack: () -> Unit,
    onSave: (String, Long, LocalDate, LocalDate?, String) -> Unit,
    modifier: Modifier = Modifier
) {
    PersonalFormScaffold(
        title = if (isEdit) "Edit Money Lent" else "Add Money Lent",
        nameLabel = "Who did you lend to",
        initialName = initialName,
        initialAmount = initialAmount,
        initialDate = initialDate,
        initialExpected = initialExpected,
        initialNotes = initialNotes,
        loaded = loaded,
        isEdit = isEdit,
        saveLabel = if (isEdit) "Save" else "Add Record",
        onBack = onBack,
        onSave = onSave
    )
}

@Composable
private fun PersonalFormScaffold(
    title: String,
    nameLabel: String,
    initialName: String,
    initialAmount: String,
    initialDate: LocalDate,
    initialExpected: LocalDate?,
    initialNotes: String,
    loaded: Boolean,
    isEdit: Boolean,
    saveLabel: String,
    onBack: () -> Unit,
    onSave: (String, Long, LocalDate, LocalDate?, String) -> Unit
) {
    var name by remember(loaded) { mutableStateOf(initialName) }
    var amount by remember(loaded) { mutableStateOf(initialAmount) }
    var date by remember(loaded) { mutableStateOf(initialDate) }
    var expected by remember(loaded) { mutableStateOf(initialExpected) }
    var notes by remember(loaded) { mutableStateOf(initialNotes) }
    var error by remember { mutableStateOf<String?>(null) }

    val amountValue = remember(amount) { Money.parse(amount) ?: 0L }

    KhataGoFormScaffold(
        title = title,
        onBack = onBack,
        saveLabel = saveLabel,
        error = error,
        onSave = {
            when {
                name.isBlank() || amountValue <= 0L -> error = "Please complete the required fields."
                else -> onSave(name, amountValue, date, expected, notes)
            }
        }
    ) {
        KhataGoFormSection(title = nameLabel) {
            KhataGoTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = "Name",
                placeholder = "e.g. Rahim"
            )
        }
        KhataGoFormSection(title = "Amount") {
            KhataGoAmountField(
                value = amount,
                onValueChange = { amount = it; error = null },
                label = "Amount"
            )
            Spacer(Modifier.height(Spacing.L))
            KhataGoDateField(value = date, onValueChange = { date = it }, label = "Date")
            Spacer(Modifier.height(Spacing.L))
            KhataGoOptionalDateField(
                value = expected,
                onValueChange = { expected = it },
                label = "Expected return date",
                placeholder = "Optional"
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

// ------------------------------------------------------- repayment / return

class RepaymentFormViewModel(
    private val repository: PersonalRepository,
    private val debtId: Long,
    private val repaymentId: Long?
) : ViewModel() {

    suspend fun remaining(): Long = repository.observeBorrowedDetail(debtId).first().remaining

    suspend fun load(): Triple<Long, LocalDate, String>? {
        val entry = repaymentId?.let { id ->
            repository.observeBorrowedDetail(debtId).first().repayments.firstOrNull { it.id == id }
        } ?: return null
        return Triple(entry.amount, LocalDate.ofEpochDay(entry.dateEpochDay), entry.notes)
    }

    fun save(
        amount: Long?,
        date: LocalDate,
        method: PaymentMethod,
        notes: String,
        onResult: (PaymentResult) -> Unit
    ) {
        viewModelScope.launch {
            if (amount == null || amount <= 0L) {
                onResult(PaymentResult.Rejected("Enter a valid amount."))
                return@launch
            }
            onResult(
                repository.saveRepayment(
                    debtId = debtId,
                    repaymentId = repaymentId,
                    date = date,
                    amount = amount,
                    method = method.name,
                    notes = notes
                )
            )
        }
    }
}

class ReturnFormViewModel(
    private val repository: PersonalRepository,
    private val lendingId: Long,
    private val returnId: Long?
) : ViewModel() {

    suspend fun remaining(): Long = repository.observeLentDetail(lendingId).first().remaining

    suspend fun load(): Triple<Long, LocalDate, String>? {
        val entry = returnId?.let { id ->
            repository.observeLentDetail(lendingId).first().returns.firstOrNull { it.id == id }
        } ?: return null
        return Triple(entry.amount, LocalDate.ofEpochDay(entry.dateEpochDay), entry.notes)
    }

    fun save(
        amount: Long?,
        date: LocalDate,
        method: PaymentMethod,
        notes: String,
        onResult: (PaymentResult) -> Unit
    ) {
        viewModelScope.launch {
            if (amount == null || amount <= 0L) {
                onResult(PaymentResult.Rejected("Enter a valid amount."))
                return@launch
            }
            onResult(
                repository.saveReturn(
                    lendingId = lendingId,
                    returnId = returnId,
                    date = date,
                    amount = amount,
                    method = method.name,
                    notes = notes
                )
            )
        }
    }
}

@Composable
fun RepaymentFormScreen(
    isLent: Boolean,
    isEdit: Boolean,
    remaining: Long,
    initialAmount: String,
    initialNotes: String,
    initialDate: LocalDate,
    onBack: () -> Unit,
    onSave: (Long?, LocalDate, PaymentMethod, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var amount by remember { mutableStateOf(initialAmount) }
    var date by remember { mutableStateOf(initialDate) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var notes by remember { mutableStateOf(initialNotes) }
    var error by remember { mutableStateOf<String?>(null) }

    val parsed = remember(amount) { Money.parse(amount) }

    KhataGoFormScaffold(
        title = if (isEdit) "Edit Payment" else if (isLent) "Add Return" else "Add Repayment",
        subtitle = if (isLent) "Money Lent" else "Money Borrowed",
        onBack = onBack,
        saveLabel = if (isEdit) "Save" else "Add Payment",
        error = error,
        onSave = {
            when {
                parsed == null || parsed <= 0L -> error = "Enter a valid amount."
                remaining <= 0L -> error = "This one is already settled."
                parsed > remaining -> error = "This payment is higher than the amount due."
                else -> onSave(parsed, date, method, notes)
            }
        }
    ) {
        KhataGoSummaryStrip(
            items = listOf(
                if (isLent) "Still to receive" to Money.format(remaining) else "Still to pay" to Money.format(remaining),
                "After this payment" to Money.format((remaining - (parsed ?: 0L)).coerceAtLeast(0L))
            )
        )
        Spacer(Modifier.height(Spacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
            if (!isEdit) {
                KhataGoChip(
                    text = "Full ${Money.format(remaining)}",
                    selected = false,
                    onClick = { amount = Money.toDecimal(remaining).toPlainString() }
                )
            }
        }
        Spacer(Modifier.height(Spacing.L))
        KhataGoFormSection(title = "Payment") {
            KhataGoAmountField(
                value = amount,
                onValueChange = { amount = it; error = null },
                label = "Amount",
                hint = if (isLent) "How much came back." else "How much you paid back.",
                errorText = error
            )
            Spacer(Modifier.height(Spacing.L))
            KhataGoDateField(value = date, onValueChange = { date = it }, label = "Date")
            Spacer(Modifier.height(Spacing.L))
            KhataGoSelectField(
                value = method,
                options = PaymentMethod.entries,
                label = "Payment method",
                optionLabel = { it.label },
                onSelected = { method = it }
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

/** Keeps the colour tokens referenced from this module. */
internal val UnusedInkTertiary = InkTertiary
internal val UnusedPositive = Positive
