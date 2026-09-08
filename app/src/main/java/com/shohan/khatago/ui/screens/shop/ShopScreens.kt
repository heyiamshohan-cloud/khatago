package com.shohan.khatago.ui.screens.shop

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
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.data.local.db.entity.ShopEntity
import com.shohan.khatago.data.repository.CreditItemInput
import com.shohan.khatago.data.repository.LedgerRepository
import com.shohan.khatago.data.repository.PaymentResult
import com.shohan.khatago.data.repository.ShopLedger
import com.shohan.khatago.data.repository.LineKind
import com.shohan.khatago.data.repository.ShopLedgerLine
import com.shohan.khatago.data.repository.ShopRepository
import com.shohan.khatago.domain.model.PaymentMethod
import com.shohan.khatago.ui.components.KhataGoAmountField
import com.shohan.khatago.ui.components.KhataGoCard
import com.shohan.khatago.ui.components.KhataGoConfirmDialog
import com.shohan.khatago.ui.components.KhataGoDateField
import com.shohan.khatago.ui.components.KhataGoEmptyState
import com.shohan.khatago.ui.components.KhataGoFormScaffold
import com.shohan.khatago.ui.components.KhataGoIconButton
import com.shohan.khatago.ui.components.KhataGoOptionalDateField
import com.shohan.khatago.ui.components.KhataGoProgress
import com.shohan.khatago.ui.components.KhataGoSecondaryButton
import com.shohan.khatago.ui.components.KhataGoSectionHeader
import com.shohan.khatago.ui.components.KhataGoSelectField
import com.shohan.khatago.ui.components.KhataGoStatusBadge
import com.shohan.khatago.ui.components.KhataGoSummaryStrip
import com.shohan.khatago.ui.components.KhataGoTextField
import com.shohan.khatago.ui.components.KhataGoTopBar
import com.shohan.khatago.ui.components.GroupDivider
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

// -------------------------------------------------------------------- detail

class ShopDetailViewModel(
    private val repository: ShopRepository,
    private val shopId: Long
) : ViewModel() {

    val ledger: StateFlow<ShopLedger> = repository.observeLedger(shopId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShopLedger(null, emptyList(), 0L, 0L, 0L))

    fun deleteShop(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteShop(shopId)
            onDone()
        }
    }

    fun deleteCredit(id: Long) {
        viewModelScope.launch { repository.deleteCredit(id) }
    }

    fun deletePayment(id: Long) {
        viewModelScope.launch { repository.deletePayment(id) }
    }

    fun setArchived(archived: Boolean) {
        viewModelScope.launch { repository.setArchived(shopId, archived) }
    }
}

/**
 * Shop ledger: purchases, payments and the running balance. Purchases are marked
 * settled oldest-first, so the user can see exactly what a payment covered.
 */
@Composable
fun ShopDetailScreen(
    ledger: ShopLedger,
    onBack: () -> Unit,
    onEditShop: () -> Unit,
    onAddPurchase: () -> Unit,
    onAddPayment: () -> Unit,
    onEditPurchase: (Long) -> Unit,
    onDeletePurchase: (Long) -> Unit,
    onEditPayment: (Long) -> Unit,
    onDeletePayment: (Long) -> Unit,
    onDeleteShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<ShopLedgerLine?>(null)
    }
    val target = pendingDelete
    val shop = ledger.shop

    if (target != null) {
        KhataGoConfirmDialog(
            title = if (target.kind == LineKind.PURCHASE) {
                "Delete this purchase?"
            } else {
                "Delete this payment?"
            },
            message = "It will be removed from the shop's credit history.",
            confirmLabel = "Delete",
            onConfirm = {
                if (target.kind == LineKind.PURCHASE) {
                    onDeletePurchase(target.id)
                } else {
                    onDeletePayment(target.id)
                }
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
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
                    title = shop?.name ?: "Shop",
                    subtitle = shop?.ownerName?.takeIf { it.isNotBlank() } ?: "Shop Credit",
                    onBack = onBack,
                    actions = {
                        KhataGoIconButton(
                            icon = Icons.Outlined.Edit,
                            contentDescription = "Edit shop",
                            onClick = onEditShop
                        )
                        KhataGoIconButton(
                            icon = Icons.Outlined.Delete,
                            contentDescription = "Delete shop",
                            onClick = onDeleteShop
                        )
                    },
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            item {
                ShopSummaryCard(
                    ledger = ledger,
                    modifier = Modifier.padding(horizontal = Spacing.Gutter)
                )
            }

            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.Gutter)) {
                    KhataGoSectionHeader(
                        title = "History",
                        actionLabel = "Add purchase",
                        onAction = onAddPurchase
                    )
                }
            }

            if (ledger.lines.isEmpty()) {
                item {
                    KhataGoCard(
                        containerColor = SurfaceWhite,
                        modifier = Modifier.padding(horizontal = Spacing.Gutter)
                    ) {
                        KhataGoEmptyState(
                            title = "No purchases yet",
                            message = "Add the first credit purchase at this shop.",
                            icon = Icons.Outlined.ReceiptLong
                        )
                    }
                }
            } else {
                val purchases = ledger.lines.filter { it.kind == LineKind.PURCHASE }
                val payments = ledger.lines.filter { it.kind == LineKind.PAYMENT }
                if (purchases.isNotEmpty()) {
                    item {
                        SectionLabel("Purchases", modifier = Modifier.padding(horizontal = Spacing.Gutter))
                    }
                    items(purchases, key = { "p${it.id}" }) { line ->
                        LedgerLineRow(
                            line = line,
                            onEdit = if (line.kind == LineKind.PURCHASE) {
                                { onEditPurchase(line.id) }
                            } else {
                                { onEditPayment(line.id) }
                            },
                            onDelete = { pendingDelete = line },
                            modifier = Modifier.padding(horizontal = Spacing.Gutter)
                        )
                    }
                }
                if (payments.isNotEmpty()) {
                    item {
                        SectionLabel("Payments", modifier = Modifier.padding(horizontal = Spacing.Gutter))
                    }
                    items(payments, key = { "pay${it.id}" }) { line ->
                        LedgerLineRow(
                            line = line,
                            onEdit = { onEditPayment(line.id) },
                            onDelete = { pendingDelete = line },
                            modifier = Modifier.padding(horizontal = Spacing.Gutter)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = InkSecondary,
        modifier = modifier.padding(start = 2.dp, top = Spacing.S, bottom = Spacing.XS)
    )
}

@Composable
private fun ShopSummaryCard(ledger: ShopLedger, modifier: Modifier = Modifier) {
    KhataGoCard(modifier = modifier, containerColor = SurfaceWhite) {
        Column(modifier = Modifier.padding(Spacing.CardInner)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Amount due", style = MaterialTheme.typography.labelSmall, color = InkSecondary)
                    Text(
                        text = Money.format(ledger.remaining),
                        style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                        color = InkPrimary
                    )
                }
            }
            Spacer(Modifier.height(Spacing.M))
            KhataGoProgress(
                progress = if (ledger.totalCredit == 0L) 0f else
                    ((ledger.totalCredit - ledger.remaining).toFloat() / ledger.totalCredit.toFloat())
                        .coerceIn(0f, 1f)
            )
            Spacer(Modifier.height(Spacing.M))
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryCell("Total credit", Money.format(ledger.totalCredit), Modifier.weight(1f))
                SummaryCell("Paid", Money.format(ledger.totalPaid), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = InkSecondary)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
            color = InkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LedgerLineRow(
    line: ShopLedgerLine,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPurchase = line.kind == LineKind.PURCHASE
    KhataGoCard(modifier = modifier, containerColor = SurfaceWhite) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.L, vertical = Spacing.M),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        if (isPurchase) KhataGoGreenSoft else com.shohan.khatago.ui.theme.PositiveSoft,
                        RoundedCornerShape(ShapeTokens.Medium)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPurchase) Icons.Outlined.Storefront else Icons.Outlined.Payments,
                    contentDescription = null,
                    tint = if (isPurchase) KhataGoGreen else Positive,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(Spacing.M))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = line.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = InkPrimary
                    )
                    if (isPurchase && line.settled) {
                        Spacer(Modifier.width(Spacing.XS))
                        KhataGoStatusBadge(text = "Settled", tone = com.shohan.khatago.ui.components.BadgeTone.POSITIVE)
                    }
                }
                Text(
                    text = buildString {
                        append(KhataGoTime.formatRelativeDate(line.date))
                        if (line.subtitle.isNotBlank()) {
                            append(" · ")
                            append(line.subtitle)
                        }
                        line.dueDate?.let {
                            append(" · due ")
                            append(KhataGoTime.formatShortDate(it))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isPurchase) "+" else "−"}${Money.format(line.amount)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                    color = if (isPurchase) InkPrimary else Positive
                )
                if (isPurchase) {
                    Text(
                        text = "Balance ${Money.format(line.runningBalance)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkTertiary
                    )
                }
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
}

// ----------------------------------------------------------------- shop form

class ShopFormViewModel(
    private val repository: ShopRepository,
    private val shopId: Long?
) : ViewModel() {

    val shop: StateFlow<ShopEntity?> =
        (shopId?.let { repository.observeShop(it) } ?: kotlinx.coroutines.flow.flowOf(null))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(
        name: String,
        ownerName: String,
        phone: String,
        address: String,
        notes: String,
        onSaved: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val id = repository.saveShop(shopId, name, ownerName, phone, address, notes)
            onSaved(id)
        }
    }
}

@Composable
fun ShopFormScreen(
    shop: ShopEntity?,
    isEdit: Boolean,
    onBack: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by androidx.compose.runtime.remember(shop?.id) { androidx.compose.runtime.mutableStateOf(shop?.name ?: "") }
    var owner by androidx.compose.runtime.remember(shop?.id) { androidx.compose.runtime.mutableStateOf(shop?.ownerName ?: "") }
    var phone by androidx.compose.runtime.remember(shop?.id) { androidx.compose.runtime.mutableStateOf(shop?.phone ?: "") }
    var address by androidx.compose.runtime.remember(shop?.id) { androidx.compose.runtime.mutableStateOf(shop?.address ?: "") }
    var notes by androidx.compose.runtime.remember(shop?.id) { androidx.compose.runtime.mutableStateOf(shop?.notes ?: "") }
    var error by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    KhataGoFormScaffold(
        title = if (isEdit) "Edit Shop" else "Add Shop",
        onBack = onBack,
        saveLabel = if (isEdit) "Save" else "Add Shop",
        error = error,
        onSave = {
            if (name.isBlank()) {
                error = "Please complete the required fields."
            } else {
                onSave(name, owner, phone, address, notes)
            }
        }
    ) {
        KhataGoTextField(
            value = name,
            onValueChange = { name = it; error = null },
            label = "Shop name",
            placeholder = "e.g. Rahman Store"
        )
        Spacer(Modifier.height(Spacing.L))
        KhataGoTextField(
            value = owner,
            onValueChange = { owner = it },
            label = "Owner name",
            placeholder = "Optional"
        )
        Spacer(Modifier.height(Spacing.L))
        KhataGoTextField(
            value = phone,
            onValueChange = { phone = it },
            label = "Phone",
            placeholder = "Optional",
            keyboardType = KeyboardType.Phone
        )
        Spacer(Modifier.height(Spacing.L))
        KhataGoTextField(
            value = address,
            onValueChange = { address = it },
            label = "Address",
            placeholder = "Optional"
        )
        Spacer(Modifier.height(Spacing.L))
        KhataGoTextField(
            value = notes,
            onValueChange = { notes = it },
            label = "Notes",
            placeholder = "Optional",
            singleLine = false
        )
    }
}
