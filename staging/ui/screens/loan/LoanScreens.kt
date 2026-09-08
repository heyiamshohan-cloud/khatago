package com.shohan.khatago.ui.screens.loan

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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.core.time.TodayProvider
import com.shohan.khatago.data.local.db.entity.LoanEntity
import com.shohan.khatago.data.local.db.entity.LoanPaymentEntity
import com.shohan.khatago.data.repository.LoanDetail
import com.shohan.khatago.data.repository.LoanInput
import com.shohan.khatago.data.repository.LoanRepository
import com.shohan.khatago.data.repository.PaymentResult
import com.shohan.khatago.domain.finance.ScheduleGenerator
import com.shohan.khatago.domain.model.Frequency
import com.shohan.khatago.domain.model.InstallmentStatus
import com.shohan.khatago.domain.model.InstallmentView
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
import com.shohan.khatago.ui.components.KhataGoProgress
import com.shohan.khatago.ui.components.KhataGoSecondaryButton
import com.shohan.khatago.ui.components.KhataGoSectionHeader
import com.shohan.khatago.ui.components.KhataGoSegmentedControl
import com.shohan.khatago.ui.components.KhataGoSelectField
import com.shohan.khatago.ui.components.KhataGoStatusBadge
import com.shohan.khatago.ui.components.KhataGoSummaryStrip
import com.shohan.khatago.ui.components.KhataGoTextField
import com.shohan.khatago.ui.components.KhataGoTopBar
import com.shohan.khatago.ui.components.GroupDivider
import com.shohan.khatago.ui.components.badgeForInstallment
import com.shohan.khatago.ui.components.sanitizeAmount
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

// ----------------------------------------------------------------- detail

class LoanDetailViewModel(
    private val repository: LoanRepository,
    private val todayProvider: TodayProvider,
    private val loanId: Long
) : ViewModel() {

    val detail: StateFlow<LoanDetail> = todayProvider.epochDay
        .flatMapLatest { today -> repository.observeDetail(loanId, today) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoanDetail())

    fun deleteLoan(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteLoan(loanId)
            onDone()
        }
    }

    fun setArchived(archived: Boolean) {
        viewModelScope.launch { repository.setArchived(loanId, archived) }
    }

    fun deletePayment(paymentId: Long) {
        viewModelScope.launch { repository.deletePayment(paymentId) }
    }
}

@Composable
fun LoanDetailScreen(
    detail: LoanDetail,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddPayment: () -> Unit,
    onEditPayment: (Long) -> Unit,
    onDeletePayment: (Long) -> Unit,
    onDeleteLoan: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingPayment by remember { mutableStateOf<Long?>(null) }
    val targetPayment = pendingPayment
    if (targetPayment != null) {
        KhataGoConfirmDialog(
            title = "Delete this payment?",
            message = "The loan balance will be recalculated without it.",
            confirmLabel = "Delete",
            onConfirm = {
                onDeletePayment(targetPayment)
                pendingPayment = null
            },
            onDismiss = { pendingPayment = null }
        )
    }

    val loan = detail.loan

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
                    title = loan?.name ?: "Loan",
                    subtitle = loan?.institution?.takeIf { it.isNotBlank() } ?: "Loan",
                    onBack = onBack,
                    actions = {
                        KhataGoIconButton(
                            icon = Icons.Outlined.Edit,
                            contentDescription = "Edit loan",
                            onClick = onEdit
                        )
                        KhataGoIconButton(
                            icon = Icons.Outlined.Delete,
                            contentDescription = "Delete loan",
                            onClick = onDeleteLoan
                        )
                    },
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            item {
                LoanSummaryCard(detail = detail, modifier = Modifier.padding(horizontal = Spacing.Gutter))
            }

            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    KhataGoSectionHeader(title = "Payment Schedule")
                }
            }

            if (detail.installments.isEmpty()) {
                item {
                    KhataGoCard(
                        containerColor = SurfaceWhite,
                        modifier = Modifier.padding(horizontal = Spacing.Gutter)
                    ) {
                        KhataGoEmptyState(
                            title = "No schedule yet",
                            message = "Edit the loan to set its installments."
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
                            detail.installments.forEachIndexed { index, installment ->
                                InstallmentRow(installment = installment, onPay = onAddPayment)
                                if (index < detail.installments.size - 1) {
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
                Column(modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    KhataGoSectionHeader(title = "Payment History")
                }
            }

            if (detail.payments.isEmpty()) {
                item {
                    KhataGoCard(
                        containerColor = SurfaceWhite,
                        modifier = Modifier.padding(horizontal = Spacing.Gutter)
                    ) {
                        KhataGoEmptyState(
                            title = "No payments yet",
                            message = "Add the first payment to this loan."
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
                            detail.payments.forEachIndexed { index, payment ->
                                PaymentHistoryRow(
                                    amount = payment.amount,
                                    date = LocalDate.ofEpochDay(payment.dateEpochDay),
                                    method = payment.method,
                                    notes = payment.notes,
                                    installmentNumber = payment.installmentId?.let { id ->
                                        detail.installments.firstOrNull { it.id == id }?.number
                                    },
                                    onEdit = { onEditPayment(payment.id) },
                                    onDelete = { pendingPayment = payment.id }
                                )
                                if (index < detail.payments.size - 1) {
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

@Composable
private fun LoanSummaryCard(detail: LoanDetail, modifier: Modifier = Modifier) {
    val loan = detail.loan
    KhataGoCard(modifier = modifier, containerColor = SurfaceWhite) {
        Column(modifier = Modifier.padding(Spacing.CardInner)) {
            Text("Amount due", style = MaterialTheme.typography.labelSmall, color = InkSecondary)
            Text(
                text = Money.format(detail.remaining),
                style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                color = InkPrimary
            )
            Text(
                text = "${detail.progressPercent}% paid",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary
            )
            Spacer(Modifier.height(Spacing.M))
            KhataGoProgress(progress = detail.progressPercent / 100f)
            Spacer(Modifier.height(Spacing.L))
            KhataGoKeyValueRow(label = "Original amount", value = Money.format(loan?.principalAmount ?: 0L))
            KhataGoKeyValueRow(label = "Processing fee", value = Money.format(loan?.processingFee ?: 0L))
            KhataGoKeyValueRow(
                label = "Interest",
                value = Money.format(
                    ((loan?.totalPayable ?: 0L) - (loan?.principalAmount ?: 0L) - (loan?.processingFee ?: 0L))
                        .coerceAtLeast(0L)
                )
            )
            KhataGoKeyValueRow(label = "Total payable", value = Money.format(detail.scheduledTotal))
            KhataGoKeyValueRow(label = "Total paid", value = Money.format(detail.paidTotal), valueColor = Positive)
            KhataGoKeyValueRow(
                label = "Next payment",
                value = detail.nextDueDate?.let { KhataGoTime.formatDate(it) } ?: "—"
            )
            if (detail.overdueCount > 0) {
                KhataGoKeyValueRow(
                    label = "Overdue",
                    value = "${detail.overdueCount} · ${Money.format(detail.overdueTotal)}",
                    valueColor = Negative
                )
            }
            if (detail.dueTodayTotal > 0L) {
                KhataGoKeyValueRow(
                    label = "Due today",
                    value = Money.format(detail.dueTodayTotal),
                    valueColor = com.shohan.khatago.ui.theme.Upcoming
                )
            }
            KhataGoKeyValueRow(label = "Payments remaining", value = "${detail.remainingCount}")
        }
    }
}

@Composable
fun InstallmentRow(installment: InstallmentView, onPay: () -> Unit, modifier: Modifier = Modifier) {
    val (label, tone) = badgeForInstallment(installment.status)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.L, vertical = Spacing.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Installment ${installment.number}",
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary
            )
            Text(
                text = KhataGoTime.formatDate(installment.dueDate),
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = Money.format(installment.scheduledAmount),
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                color = InkPrimary
            )
            if (installment.paidAmount > 0L && installment.remaining > 0L) {
                Text(
                    text = "${Money.format(installment.remaining)} left",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary
                )
            }
        }
        Spacer(Modifier.width(Spacing.S))
        KhataGoStatusBadge(text = label, tone = tone)
        if (installment.status != InstallmentStatus.PAID) {
            KhataGoIconButton(
                icon = Icons.Outlined.Payments,
                contentDescription = "Pay installment ${installment.number}",
                onClick = onPay,
                tint = KhataGoGreen
            )
        }
    }
}

@Composable
fun PaymentHistoryRow(
    amount: Long,
    date: LocalDate,
    method: String,
    notes: String,
    installmentNumber: Int?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
                text = buildString {
                    append(Money.format(amount))
                    if (installmentNumber != null) append(" · installment $installmentNumber")
                },
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(KhataGoTime.formatDate(date))
                    append(" · ")
                    append(com.shohan.khatago.ui.screens.loan.prettyMethod(method))
                    if (notes.isNotBlank()) {
                        append(" · ")
                        append(notes)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        KhataGoIconButton(icon = Icons.Outlined.Edit, contentDescription = "Edit payment", onClick = onEdit)
        KhataGoIconButton(
            icon = Icons.Outlined.Delete,
            contentDescription = "Delete payment",
            onClick = onDelete,
            tint = Negative
        )
    }
}

internal fun prettyMethod(method: String): String =
    method.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")

// ------------------------------------------------------------------- form

class LoanFormViewModel(
    private val repository: LoanRepository,
    private val loanId: Long?
) : ViewModel() {

    val loan: StateFlow<LoanEntity?> =
        (loanId?.let { repository.observeLoan(it) } ?: kotlinx.coroutines.flow.flowOf(null))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(input: LoanInput, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.saveLoan(input)
            onSaved(id)
        }
    }
}

@Composable
fun LoanFormScreen(
    loan: LoanEntity?,
    isEdit: Boolean,
    onBack: () -> Unit,
    onSave: (LoanInput) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(loan?.id) { mutableStateOf(loan?.name ?: "") }
    var institution by remember(loan?.id) { mutableStateOf(loan?.institution ?: "") }
    var principal by remember(loan?.id) {
        mutableStateOf(loan?.principalAmount?.let { Money.toDecimal(it).toPlainString() } ?: "")
    }
    var fee by remember(loan?.id) {
        mutableStateOf(loan?.processingFee?.let { Money.toDecimal(it).toPlainString() } ?: "")
    }
    var rate by remember(loan?.id) {
        mutableStateOf(loan?.interestRateBps?.let { (it / 100.0).toString() } ?: "")
    }
    var totalPayable by remember(loan?.id) {
        mutableStateOf(loan?.totalPayable?.let { Money.toDecimal(it).toPlainString() } ?: "")
    }
    var totalTouched by remember { mutableStateOf(loan != null) }
    var count by remember(loan?.id) { mutableStateOf(loan?.installmentCount?.toString() ?: "") }
    var frequency by remember(loan?.id) { mutableStateOf(Frequency.from(loan?.frequency)) }
    var dateTaken by remember(loan?.id) { mutableStateOf(loan?.let { LocalDate.ofEpochDay(it.dateTakenEpochDay) } ?: LocalDate.now()) }
    var firstDue by remember(loan?.id) {
        mutableStateOf(loan?.let { LocalDate.ofEpochDay(it.firstDueDateEpochDay) } ?: LocalDate.now().plusMonths(1))
    }
    var notes by remember(loan?.id) { mutableStateOf(loan?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    val principalAmount = remember(principal) { Money.parse(principal) ?: 0L }
    val feeAmount = remember(fee) { Money.parse(fee) ?: 0L }
    val rateBps = remember(rate) { ((rate.toDoubleOrNull() ?: 0.0) * 100).toLong() }
    val countValue = count.toIntOrNull() ?: 0

    // Suggested total: principal + processing fee + simple interest.
    val suggestedTotal = Money.safeAdd(
        Money.safeAdd(principalAmount, feeAmount),
        Money.applyRate(principalAmount, rateBps)
    )
    val effectiveTotal = if (totalTouched) (Money.parse(totalPayable) ?: 0L) else suggestedTotal
    val perInstallment = if (countValue > 0) {
        Money.splitEvenly(effectiveTotal, countValue).firstOrNull() ?: 0L
    } else {
        0L
    }

    KhataGoFormScaffold(
        title = if (isEdit) "Edit Loan" else "Add Loan",
        onBack = onBack,
        saveLabel = if (isEdit) "Save" else "Add Loan",
        error = error,
        onSave = {
            when {
                name.isBlank() || principalAmount <= 0L -> error = "Please complete the required fields."
                countValue <= 0 -> error = "Enter how many installments."
                effectiveTotal <= 0L -> error = "Enter a valid amount."
                else -> onSave(
                    LoanInput(
                        id = loan?.id,
                        name = name,
                        institution = institution,
                        principalAmount = principalAmount,
                        processingFee = feeAmount,
                        interestRateBps = rateBps,
                        dateTaken = dateTaken,
                        totalPayable = effectiveTotal,
                        installmentAmount = perInstallment,
                        installmentCount = countValue,
                        frequency = frequency,
                        firstDueDate = firstDue,
                        notes = notes
                    )
                )
            }
        }
    ) {
        KhataGoFormSection(title = "Basic details") {
            KhataGoTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = "Loan name",
                placeholder = "e.g. Home loan"
            )
            Spacer(Modifier.height(Spacing.L))
            KhataGoTextField(
                value = institution,
                onValueChange = { institution = it },
                label = "Institution",
                placeholder = "e.g. Bank name"
            )
            Spacer(Modifier.height(Spacing.L))
            KhataGoDateField(value = dateTaken, onValueChange = { dateTaken = it }, label = "Date taken")
        }

        KhataGoFormSection(title = "Amount") {
            KhataGoAmountField(
                value = principal,
                onValueChange = { principal = it; error = null },
                label = "Loan amount"
            )
            Spacer(Modifier.height(Spacing.L))
            KhataGoTextField(
                value = rate,
                onValueChange = { rate = sanitizeAmount(it) },
                label = "Interest rate (%)",
                placeholder = "0",
                keyboardType = KeyboardType.Decimal
            )
            Spacer(Modifier.height(Spacing.L))
            KhataGoAmountField(
                value = fee,
                onValueChange = { fee = it },
                label = "Processing fee"
            )
            Spacer(Modifier.height(Spacing.L))
            KhataGoAmountField(
                value = if (totalTouched) totalPayable else Money.toDecimal(suggestedTotal).toPlainString(),
                onValueChange = {
                    totalPayable = it
                    totalTouched = true
                },
                label = "Total payable",
                hint = "Suggested from amount, fee and interest."
            )
        }

        KhataGoFormSection(title = "Schedule") {
            KhataGoTextField(
                value = count,
                onValueChange = { count = it.filter { ch -> ch.isDigit() } },
                label = "Number of installments",
                placeholder = "12",
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.height(Spacing.L))
            KhataGoSegmentedControl(
                options = Frequency.entries,
                selected = frequency,
                onSelected = { frequency = it },
                label = { it.label }
            )
            Spacer(Modifier.height(Spacing.L))
            KhataGoDateField(value = firstDue, onValueChange = { firstDue = it }, label = "First due date")
            Spacer(Modifier.height(Spacing.L))
            KhataGoSummaryStrip(
                items = listOf(
                    "Installment" to Money.format(perInstallment),
                    "Total payable" to Money.format(effectiveTotal),
                    "Ends" to if (countValue > 0) {
                        KhataGoTime.formatDate(
                            ScheduleGenerator.maturityDate(firstDue, countValue, frequency)
                        )
                    } else {
                        "—"
                    }
                )
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

// --------------------------------------------------------------- payment

class LoanPaymentFormViewModel(
    private val repository: LoanRepository,
    private val loanId: Long,
    private val paymentId: Long?
) : ViewModel() {

    suspend fun remaining(): Long = repository.remaining(loanId)

    suspend fun nextInstallmentRemaining(): Long =
        repository.nextUnsettled(loanId)?.let { it.scheduledAmount - it.paidAmount } ?: 0L

    suspend fun load(): Triple<Long, LocalDate, String>? {
        val payment = paymentId?.let { repository.getPayment(it) } ?: return null
        return Triple(payment.amount, LocalDate.ofEpochDay(payment.dateEpochDay), payment.notes)
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
                repository.savePayment(
                    loanId = loanId,
                    paymentId = paymentId,
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
fun LoanPaymentFormScreen(
    isEdit: Boolean,
    remaining: Long,
    nextInstallmentRemaining: Long,
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
        title = if (isEdit) "Edit Payment" else "Add Payment",
        subtitle = "Loan",
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
                "Remaining" to Money.format(remaining),
                "After this payment" to Money.format((remaining - (parsed ?: 0L)).coerceAtLeast(0L))
            )
        )
        Spacer(Modifier.height(Spacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
            if (nextInstallmentRemaining > 0L) {
                KhataGoChip(
                    text = "Next ${Money.format(nextInstallmentRemaining)}",
                    selected = false,
                    onClick = { amount = Money.toDecimal(nextInstallmentRemaining).toPlainString() }
                )
            }
            KhataGoChip(
                text = "Full ${Money.format(remaining)}",
                selected = false,
                onClick = { amount = Money.toDecimal(remaining).toPlainString() }
            )
        }
        Spacer(Modifier.height(Spacing.L))
        KhataGoFormSection(title = "Payment") {
            KhataGoAmountField(
                value = amount,
                onValueChange = { amount = it; error = null },
                label = "Amount",
                hint = "Partial payments are fine.",
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
