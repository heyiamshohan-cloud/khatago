package com.shohan.khatago.ui.screens.emi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.core.time.TodayProvider
import com.shohan.khatago.data.local.db.entity.EmiPurchaseEntity
import com.shohan.khatago.data.repository.EmiDetail
import com.shohan.khatago.data.repository.EmiInput
import com.shohan.khatago.data.repository.EmiRepository
import com.shohan.khatago.data.repository.PaymentResult
import com.shohan.khatago.domain.finance.ScheduleGenerator
import com.shohan.khatago.domain.model.Frequency
import com.shohan.khatago.domain.model.PaymentMethod
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
import com.shohan.khatago.ui.components.KhataGoSectionHeader
import com.shohan.khatago.ui.components.KhataGoSegmentedControl
import com.shohan.khatago.ui.components.KhataGoSelectField
import com.shohan.khatago.ui.components.KhataGoSummaryStrip
import com.shohan.khatago.ui.components.KhataGoTextField
import com.shohan.khatago.ui.components.KhataGoTopBar
import com.shohan.khatago.ui.components.sanitizeAmount
import com.shohan.khatago.ui.screens.loan.InstallmentRow
import com.shohan.khatago.ui.screens.loan.PaymentHistoryRow
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkPrimary
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.Negative
import com.shohan.khatago.ui.theme.OnHero
import com.shohan.khatago.ui.theme.Positive
import com.shohan.khatago.ui.theme.ShapeTokens
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceWhite
import com.shohan.khatago.ui.theme.Upcoming
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

// ----------------------------------------------------------------- detail

class EmiDetailViewModel(
    private val repository: EmiRepository,
    private val todayProvider: TodayProvider,
    private val emiId: Long
) : ViewModel() {

    val detail: StateFlow<EmiDetail> = todayProvider.epochDay
        .flatMapLatest { today -> repository.observeDetail(emiId, today) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EmiDetail())

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteEmi(emiId)
            onDone()
        }
    }

    fun deletePayment(paymentId: Long) {
        viewModelScope.launch { repository.deletePayment(paymentId) }
    }
}

@Composable
fun EmiDetailScreen(
    detail: EmiDetail,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddPayment: () -> Unit,
    onEditPayment: (Long) -> Unit,
    onDeletePayment: (Long) -> Unit,
    onDeleteEmi: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingPayment by remember { mutableStateOf<Long?>(null) }
    val target = pendingPayment
    if (target != null) {
        KhataGoConfirmDialog(
            title = "Delete this payment?",
            message = "The EMI balance will be recalculated without it.",
            confirmLabel = "Delete",
            onConfirm = {
                onDeletePayment(target)
                pendingPayment = null
            },
            onDismiss = { pendingPayment = null }
        )
    }

    val emi = detail.purchase

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
                    title = emi?.productName ?: "EMI",
                    subtitle = emi?.seller?.takeIf { it.isNotBlank() } ?: "Product installment",
                    onBack = onBack,
                    actions = {
                        KhataGoIconButton(
                            icon = Icons.Outlined.Edit,
                            contentDescription = "Edit EMI",
                            onClick = onEdit
                        )
                        KhataGoIconButton(
                            icon = Icons.Outlined.Delete,
                            contentDescription = "Delete EMI",
                            onClick = onDeleteEmi
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
                        KhataGoKeyValueRow(label = "Total price", value = Money.format(emi?.totalPrice ?: 0L))
                        KhataGoKeyValueRow(label = "Down payment", value = Money.format(emi?.downPayment ?: 0L))
                        KhataGoKeyValueRow(label = "Financed", value = Money.format(detail.financedAmount))
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
                                valueColor = Upcoming
                            )
                        }
                        KhataGoKeyValueRow(label = "Installments left", value = "${detail.remainingCount}")
                    }
                }
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
                            message = "Edit this product to set its installments."
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
                            message = "Add the first installment payment."
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

// ------------------------------------------------------------------- form

class EmiFormViewModel(
    private val repository: EmiRepository,
    private val emiId: Long?
) : ViewModel() {

    val emi: StateFlow<EmiPurchaseEntity?> =
        (emiId?.let { repository.observeEmi(it) } ?: kotlinx.coroutines.flow.flowOf(null))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(input: EmiInput, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.saveEmi(input)
            onSaved(id)
        }
    }
}

@Composable
fun EmiFormScreen(
    emi: EmiPurchaseEntity?,
    isEdit: Boolean,
    onBack: () -> Unit,
    onSave: (EmiInput) -> Unit,
    modifier: Modifier = Modifier
) {
    var product by remember(emi?.id) { mutableStateOf(emi?.productName ?: "") }
    var seller by remember(emi?.id) { mutableStateOf(emi?.seller ?: "") }
    var totalPrice by remember(emi?.id) {
        mutableStateOf(emi?.totalPrice?.let { Money.toDecimal(it).toPlainString() } ?: "")
    }
    var downPayment by remember(emi?.id) {
        mutableStateOf(emi?.downPayment?.let { Money.toDecimal(it).toPlainString() } ?: "")
    }
    var totalPayable by remember(emi?.id) {
        mutableStateOf(emi?.totalPayable?.let { Money.toDecimal(it).toPlainString() } ?: "")
    }
    var count by remember(emi?.id) { mutableStateOf(emi?.installmentCount?.toString() ?: "") }
    var frequency by remember(emi?.id) { mutableStateOf(Frequency.from(emi?.frequency)) }
    var purchaseDate by remember(emi?.id) {
        mutableStateOf(emi?.let { LocalDate.ofEpochDay(it.purchaseDateEpochDay) } ?: LocalDate.now())
    }
    var firstDue by remember(emi?.id) {
        mutableStateOf(emi?.let { LocalDate.ofEpochDay(it.firstDueDateEpochDay) } ?: LocalDate.now().plusMonths(1))
    }
    var notes by remember(emi?.id) { mutableStateOf(emi?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    val priceAmount = remember(totalPrice) { Money.parse(totalPrice) ?: 0L }
    val downAmount = remember(downPayment) { Money.parse(downPayment) ?: 0L }
    val payableAmount = remember(totalPayable) { Money.parse(totalPayable) ?: 0L }
    val countValue = count.toIntOrNull() ?: 0
    val financed = (payableAmount - downAmount).coerceAtLeast(0L)
    val perInstallment = if (countValue > 0) {
        Money.splitEvenly(financed, countValue).firstOrNull() ?: 0L
    } else {
        0L
    }

    KhataGoFormScaffold(
        title = if (isEdit) "Edit EMI" else "Add EMI",
        onBack = onBack,
        saveLabel = if (isEdit) "Save" else "Add EMI",
        error = error,
        onSave = {
            when {
                product.isBlank() || payableAmount <= 0L -> error = "Please complete the required fields."
                downAmount >= payableAmount -> error = "The down payment can't be more than the total payable."
                countValue <= 0 -> error = "Enter how many installments."
                else -> onSave(
                    EmiInput(
                        id = emi?.id,
                        productName = product,
                        seller = seller,
                        purchaseDate = purchaseDate,
                        totalPrice = priceAmount,
                        downPayment = downAmount,
                        totalPayable = payableAmount,
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
        KhataGoFormSection(title = "Product") {
            KhataGoTextField(
                value = product,
                onValueChange = { product = it; error = null },
                label = "Product name",
                placeholder = "e.g. Laptop"
            )
            Spacer(Modifier.height(Spacing.L))
            KhataGoTextField(
                value = seller,
                onValueChange = { seller = it },
                label = "Seller / Provider",
                placeholder = "Optional"
            )
            Spacer(Modifier.height(Spacing.L))
            KhataGoDateField(value = purchaseDate, onValueChange = { purchaseDate = it }, label = "Purchase date")
        }

        KhataGoFormSection(title = "Amount") {
            KhataGoAmountField(value = totalPrice, onValueChange = { totalPrice = it }, label = "Total price")
            Spacer(Modifier.height(Spacing.L))
            KhataGoAmountField(
                value = downPayment,
                onValueChange = { downPayment = it; error = null },
                label = "Down payment"
            )
            Spacer(Modifier.height(Spacing.L))
            KhataGoAmountField(
                value = totalPayable,
                onValueChange = { totalPayable = it; error = null },
                label = "Total payable",
                hint = "What you pay back in total."
            )
        }

        KhataGoFormSection(title = "Schedule") {
            KhataGoTextField(
                value = count,
                onValueChange = { count = it.filter { ch -> ch.isDigit() } },
                label = "Number of installments",
                placeholder = "10",
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
                    "Financed" to Money.format(financed),
                    "Installment" to Money.format(perInstallment),
                    "Ends" to if (countValue > 0) {
                        KhataGoTime.formatDate(ScheduleGenerator.maturityDate(firstDue, countValue, frequency))
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

class EmiPaymentFormViewModel(
    private val repository: EmiRepository,
    private val emiId: Long,
    private val paymentId: Long?
) : ViewModel() {

    suspend fun remaining(): Long = repository.remaining(emiId)

    suspend fun nextInstallmentRemaining(): Long =
        repository.nextUnsettled(emiId)?.let { it.scheduledAmount - it.paidAmount } ?: 0L

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
                    emiId = emiId,
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
fun EmiPaymentFormScreen(
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
        subtitle = "EMI",
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

/** Unused import guard for the sanitize helper in this module. */
private val UnusedSanitize: (String) -> String = ::sanitizeAmount
