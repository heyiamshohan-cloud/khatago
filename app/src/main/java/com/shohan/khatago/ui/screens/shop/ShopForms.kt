package com.shohan.khatago.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.data.repository.CreditItemInput
import com.shohan.khatago.data.repository.PaymentResult
import com.shohan.khatago.data.repository.ShopRepository
import com.shohan.khatago.domain.model.PaymentMethod
import com.shohan.khatago.ui.components.KhataGoAmountField
import com.shohan.khatago.ui.components.KhataGoCard
import com.shohan.khatago.ui.components.KhataGoDateField
import com.shohan.khatago.ui.components.KhataGoFormScaffold
import com.shohan.khatago.ui.components.KhataGoFormSection
import com.shohan.khatago.ui.components.KhataGoOptionalDateField
import com.shohan.khatago.ui.components.KhataGoSecondaryButton
import com.shohan.khatago.ui.components.KhataGoSelectField
import com.shohan.khatago.ui.components.KhataGoSummaryStrip
import com.shohan.khatago.ui.components.KhataGoTextField
import com.shohan.khatago.ui.components.sanitizeAmount
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkPrimary
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.InkTertiary
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.KhataGoGreenSoft
import com.shohan.khatago.ui.theme.OutlineSoft
import com.shohan.khatago.ui.theme.ShapeTokens
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceSubtle
import com.shohan.khatago.ui.theme.SurfaceWhite
import kotlinx.coroutines.launch
import java.time.LocalDate

/** One editable line of a credit purchase. */
data class ItemDraft(
    val id: Long = 0L,
    val name: String = "",
    val quantity: String = "1",
    val unit: String = "",
    val unitPrice: String = ""
) {
    val quantityMilli: Long get() = (Money.parse(quantity) ?: 0L) * 10L
    val unitPriceMinor: Long get() = Money.parse(unitPrice) ?: 0L
    val lineTotal: Long get() = Money.lineTotal(unitPriceMinor, quantityMilli)
}

fun formatQuantity(milli: Long): String {
    val value = milli / 1000.0
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

class CreditFormViewModel(
    private val repository: ShopRepository,
    private val shopId: Long,
    private val creditId: Long?
) : ViewModel() {

    suspend fun load(): Triple<LocalDate, LocalDate?, String>? {
        val credit = creditId?.let { repository.getCredit(it) } ?: return null
        return Triple(
            LocalDate.ofEpochDay(credit.dateEpochDay),
            credit.dueDateEpochDay?.let { LocalDate.ofEpochDay(it) },
            credit.notes
        )
    }

    suspend fun loadItems(): List<ItemDraft> {
        val creditIdValue = creditId ?: return emptyList()
        return repository.getItems(creditIdValue).map { item ->
            ItemDraft(
                id = item.id,
                name = item.name,
                quantity = formatQuantity(item.quantityMilli),
                unit = item.unit,
                unitPrice = Money.toDecimal(item.unitPrice).toPlainString()
            )
        }
    }

    fun save(
        date: LocalDate,
        dueDate: LocalDate?,
        items: List<ItemDraft>,
        notes: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val valid = items.filter { it.name.isNotBlank() && it.lineTotal > 0L }
            if (valid.isEmpty()) {
                onResult(false, "Add at least one item.")
                return@launch
            }
            repository.saveCredit(
                shopId = shopId,
                creditId = creditId,
                date = date,
                dueDate = dueDate,
                items = valid.map {
                    CreditItemInput(
                        id = it.id,
                        name = it.name,
                        quantityMilli = it.quantityMilli,
                        unit = it.unit,
                        unitPrice = it.unitPriceMinor,
                        lineTotal = it.lineTotal
                    )
                },
                notes = notes
            )
            onResult(true, null)
        }
    }
}

/**
 * Add or edit a credit purchase. A single purchase can hold many items, and
 * totals are recalculated from the quantities and prices the user typed.
 */
@Composable
fun CreditFormScreen(
    isEdit: Boolean,
    initialDate: LocalDate,
    initialDueDate: LocalDate?,
    initialNotes: String,
    initialItems: List<ItemDraft>,
    itemsLoaded: Boolean,
    onBack: () -> Unit,
    onSave: (LocalDate, LocalDate?, List<ItemDraft>, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var date by remember { mutableStateOf(initialDate) }
    var dueDate by remember { mutableStateOf(initialDueDate) }
    var notes by remember { mutableStateOf(initialNotes) }
    var error by remember { mutableStateOf<String?>(null) }

    val items = remember(itemsLoaded) { mutableStateListOf<ItemDraft>().apply { addAll(initialItems) } }
    if (items.isEmpty()) {
        items.add(ItemDraft())
    }

    val total = items.sumOf { it.lineTotal }

    KhataGoFormScaffold(
        title = if (isEdit) "Edit Purchase" else "Add Purchase",
        subtitle = "Shop Credit",
        onBack = onBack,
        saveLabel = if (isEdit) "Save" else "Add Purchase",
        error = error,
        onSave = {
            val usable = items.filter { it.name.isNotBlank() && it.lineTotal > 0L }
            if (usable.isEmpty()) {
                error = "Add at least one item."
            } else {
                onSave(date, dueDate, usable, notes)
            }
        }
    ) {
        KhataGoFormSection(title = "Purchase details") {
            KhataGoDateField(value = date, onValueChange = { date = it }, label = "Date")
            Spacer(Modifier.height(Spacing.L))
            KhataGoOptionalDateField(
                value = dueDate,
                onValueChange = { dueDate = it },
                label = "Due date",
                placeholder = "Optional"
            )
        }

        KhataGoFormSection(title = "Items") {
            items.forEachIndexed { index, item ->
                ItemEditor(
                    item = item,
                    index = index,
                    canRemove = items.size > 1,
                    onChange = { updated -> items[index] = updated },
                    onRemove = { items.removeAt(index) }
                )
                Spacer(Modifier.height(Spacing.M))
            }
            KhataGoSecondaryButton(
                text = "Add item",
                onClick = { items.add(ItemDraft()) },
                icon = Icons.Outlined.Add,
                modifier = Modifier.fillMaxWidth()
            )
        }

        KhataGoFormSection(title = "Summary") {
            KhataGoSummaryStrip(
                items = listOf(
                    "Items" to "${items.count { it.name.isNotBlank() }}",
                    "Total" to Money.format(total)
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

@Composable
private fun ItemEditor(
    item: ItemDraft,
    index: Int,
    canRemove: Boolean,
    onChange: (ItemDraft) -> Unit,
    onRemove: () -> Unit
) {
    KhataGoCard(containerColor = SurfaceWhite) {
        Column(modifier = Modifier.padding(Spacing.M)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Item ${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSecondary,
                    modifier = Modifier.weight(1f)
                )
                if (canRemove) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Remove item",
                            tint = InkTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            OutlinedTextField(
                value = item.name,
                onValueChange = { onChange(item.copy(name = it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Item name", color = InkTertiary) },
                singleLine = true,
                shape = RoundedCornerShape(ShapeTokens.Small),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KhataGoGreen,
                    unfocusedBorderColor = OutlineSoft,
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite
                )
            )
            Spacer(Modifier.height(Spacing.S))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
                OutlinedTextField(
                    value = item.quantity,
                    onValueChange = { onChange(item.copy(quantity = sanitizeAmount(it))) },
                    modifier = Modifier.weight(0.8f),
                    label = { Text("Qty", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    shape = RoundedCornerShape(ShapeTokens.Small),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KhataGoGreen,
                        unfocusedBorderColor = OutlineSoft,
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite
                    )
                )
                OutlinedTextField(
                    value = item.unit,
                    onValueChange = { onChange(item.copy(unit = it)) },
                    modifier = Modifier.weight(0.7f),
                    label = { Text("Unit", style = MaterialTheme.typography.labelSmall) },
                    placeholder = { Text("kg", color = InkTertiary) },
                    singleLine = true,
                    shape = RoundedCornerShape(ShapeTokens.Small),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KhataGoGreen,
                        unfocusedBorderColor = OutlineSoft,
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite
                    )
                )
                OutlinedTextField(
                    value = item.unitPrice,
                    onValueChange = { onChange(item.copy(unitPrice = sanitizeAmount(it))) },
                    modifier = Modifier.weight(1.2f),
                    label = { Text("Price ${Money.CURRENCY_SYMBOL}", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    shape = RoundedCornerShape(ShapeTokens.Small),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KhataGoGreen,
                        unfocusedBorderColor = OutlineSoft,
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite
                    )
                )
            }
            Spacer(Modifier.height(Spacing.S))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceSubtle, RoundedCornerShape(ShapeTokens.Small))
                    .padding(horizontal = Spacing.M, vertical = Spacing.S),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Line total",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary
                )
                Text(
                    text = Money.format(item.lineTotal),
                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                    color = InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ------------------------------------------------------------- shop payment

class ShopPaymentFormViewModel(
    private val repository: ShopRepository,
    private val shopId: Long,
    private val paymentId: Long?
) : ViewModel() {

    suspend fun remaining(): Long = repository.remainingForShop(shopId)

    suspend fun load(): Triple<Long, LocalDate, String>? {
        val payment = paymentId?.let { repository.getPayment(it) } ?: return null
        return Triple(
            payment.amount,
            LocalDate.ofEpochDay(payment.dateEpochDay),
            payment.notes
        )
    }

    fun save(
        amount: Long?,
        date: LocalDate,
        method: PaymentMethod,
        notes: String,
        creditId: Long? = null,
        onResult: (PaymentResult) -> Unit
    ) {
        viewModelScope.launch {
            if (amount == null || amount <= 0L) {
                onResult(PaymentResult.Rejected("Enter a valid amount."))
                return@launch
            }
            val result = repository.savePayment(
                shopId = shopId,
                paymentId = paymentId,
                date = date,
                amount = amount,
                method = method.name,
                notes = notes,
                creditId = creditId
            )
            onResult(result)
        }
    }
}

@Composable
fun ShopPaymentFormScreen(
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
        title = if (isEdit) "Edit Payment" else "Add Payment",
        subtitle = "Shop Credit",
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
                "Amount due" to Money.format(remaining),
                "After this payment" to Money.format(
                    (remaining - (parsed ?: 0L)).coerceAtLeast(0L)
                )
            )
        )
        Spacer(Modifier.height(Spacing.L))
        KhataGoFormSection(title = "Payment") {
            KhataGoAmountField(
                value = amount,
                onValueChange = { amount = it; error = null },
                label = "Amount",
                hint = "You can pay part of it now.",
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
