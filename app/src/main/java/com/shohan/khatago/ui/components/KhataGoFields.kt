package com.shohan.khatago.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.ui.theme.DividerSoft
import com.shohan.khatago.ui.theme.InkPrimary
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.InkTertiary
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.KhataGoGreenSoft
import com.shohan.khatago.ui.theme.Negative
import com.shohan.khatago.ui.theme.OutlineSoft
import com.shohan.khatago.ui.theme.OutlineStrong
import com.shohan.khatago.ui.theme.ShapeTokens
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceSubtle
import com.shohan.khatago.ui.theme.SurfaceWhite
import java.time.LocalDate

/**
 * Form fields.
 *
 * Every field has a visible label, the right keyboard for its content and enough
 * height to tap comfortably. Errors are written as plain sentences.
 */

private val fieldShape @Composable get() = RoundedCornerShape(ShapeTokens.Medium)

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = KhataGoGreen,
    unfocusedBorderColor = OutlineSoft,
    disabledBorderColor = OutlineSoft,
    errorBorderColor = Negative,
    focusedContainerColor = SurfaceWhite,
    unfocusedContainerColor = SurfaceWhite,
    disabledContainerColor = SurfaceSubtle,
    errorContainerColor = SurfaceWhite,
    focusedTextColor = InkPrimary,
    unfocusedTextColor = InkPrimary,
    focusedLabelColor = KhataGoGreen,
    unfocusedLabelColor = InkSecondary,
    cursorColor = KhataGoGreen,
    focusedLeadingIconColor = KhataGoGreen,
    unfocusedLeadingIconColor = InkSecondary,
    focusedTrailingIconColor = InkSecondary,
    unfocusedTrailingIconColor = InkTertiary
)

@Composable
fun KhataGoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    errorText: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 4,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    readOnly: Boolean = false,
    filter: ((String) -> String)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = InkSecondary,
            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = { raw -> onValueChange(filter?.invoke(raw) ?: raw) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge,
            label = null,
            placeholder = placeholder?.let {
                { Text(it, style = MaterialTheme.typography.bodyLarge, color = InkTertiary) }
            },
            isError = errorText != null,
            singleLine = singleLine,
            maxLines = maxLines,
            readOnly = readOnly,
            shape = fieldShape,
            colors = fieldColors(),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
                capitalization = if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None
            ),
            keyboardActions = KeyboardActions(
                onAny = { onImeAction?.invoke() }
            ),
            leadingIcon = leadingIcon
        )
        val message = errorText ?: supportingText
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (errorText != null) Negative else InkSecondary,
                modifier = Modifier.padding(top = 6.dp, start = 2.dp)
            )
        }
    }
}

/** Currency-aware input: numeric keypad, ৳ prefix, at most one decimal point. */
@Composable
fun KhataGoAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Amount",
    errorText: String? = null,
    hint: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null
) {
    val parsed = remember(value) { Money.parse(value) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = InkSecondary,
            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = { raw -> onValueChange(sanitizeAmount(raw)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            isError = errorText != null,
            singleLine = true,
            shape = fieldShape,
            colors = fieldColors(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
            leadingIcon = {
                Text(
                    text = Money.CURRENCY_SYMBOL,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (errorText != null) Negative else KhataGoGreen,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        )
        val message = errorText ?: hint ?: parsed?.let { Money.format(it) }
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    errorText != null -> Negative
                    hint != null -> InkSecondary
                    else -> InkSecondary
                },
                modifier = Modifier.padding(top = 6.dp, start = 2.dp)
            )
        }
    }
}

/** Digits plus a single decimal separator, at most two decimals. */
internal fun sanitizeAmount(input: String): String {
    var result = input.filter { it.isDigit() || it == '.' }
    val firstDot = result.indexOf('.')
    if (firstDot >= 0) {
        result = result.substring(0, firstDot + 1) + result.substring(firstDot + 1).replace(".", "")
        val decimals = result.substringAfter('.', "")
        if (decimals.length > 2) result = result.substring(0, firstDot + 3)
    }
    if (result.startsWith(".")) result = "0$result"
    if (result.length > 15) result = result.take(15)
    return result
}

/** Date field that opens the platform date picker in a light theme. */
@Composable
fun KhataGoDateField(
    value: LocalDate,
    onValueChange: (LocalDate) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    allowClear: Boolean = false,
    onClear: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = InkSecondary,
            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(fieldShape)
                .border(1.dp, OutlineSoft, fieldShape)
                .background(SurfaceWhite)
                .clickable {
                    android.app.DatePickerDialog(
                        context,
                        android.R.style.Theme_DeviceDefault_Light_Dialog,
                        { _, year, month, dayOfMonth ->
                            onValueChange(LocalDate.of(year, month + 1, dayOfMonth))
                        },
                        value.year,
                        value.monthValue - 1,
                        value.dayOfMonth
                    ).show()
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = KhataGoGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = KhataGoTime.formatDate(value),
                style = MaterialTheme.typography.bodyLarge,
                color = InkPrimary,
                modifier = Modifier.weight(1f)
            )
            if (allowClear && onClear != null) {
                Text(
                    text = "Clear",
                    style = MaterialTheme.typography.labelMedium,
                    color = KhataGoGreen,
                    modifier = Modifier
                        .clip(RoundedCornerShape(ShapeTokens.Small))
                        .clickable { onClear() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Choose date",
                    tint = InkTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** Optional date: "No date" until the user picks one. */
@Composable
fun KhataGoOptionalDateField(
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "Not set"
) {
    val context = LocalContext.current
    val anchor = value ?: LocalDate.now()
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = InkSecondary,
            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(fieldShape)
                .border(1.dp, OutlineSoft, fieldShape)
                .background(SurfaceWhite)
                .clickable {
                    android.app.DatePickerDialog(
                        context,
                        android.R.style.Theme_DeviceDefault_Light_Dialog,
                        { _, year, month, dayOfMonth ->
                            onValueChange(LocalDate.of(year, month + 1, dayOfMonth))
                        },
                        anchor.year,
                        anchor.monthValue - 1,
                        anchor.dayOfMonth
                    ).show()
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = if (value == null) InkTertiary else KhataGoGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = value?.let { KhataGoTime.formatDate(it) } ?: placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = if (value == null) InkTertiary else InkPrimary,
                modifier = Modifier.weight(1f)
            )
            if (value != null) {
                Text(
                    text = "Clear",
                    style = MaterialTheme.typography.labelMedium,
                    color = KhataGoGreen,
                    modifier = Modifier
                        .clip(RoundedCornerShape(ShapeTokens.Small))
                        .clickable { onValueChange(null) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/** Single-choice field rendered as an option sheet — no experimental menu APIs. */
@Composable
fun <T> KhataGoSelectField(
    value: T?,
    options: List<T>,
    label: String,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select"
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = InkSecondary,
            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(fieldShape)
                .border(1.dp, OutlineSoft, fieldShape)
                .background(SurfaceWhite)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value?.let(optionLabel) ?: placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = if (value == null) InkTertiary else InkPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Choose $label",
                tint = InkTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (expanded) {
        KhataGoOptionSheet(
            title = label,
            options = options,
            optionLabel = optionLabel,
            onSelected = {
                onSelected(it)
                expanded = false
            },
            onDismiss = { expanded = false }
        )
    }
}

@Composable
fun <T> KhataGoOptionSheet(
    title: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    selected: T? = null
) {
    KhataGoBottomSheet(onDismiss = onDismiss, title = title) {
        LazyColumn {
            items(options) { option ->
                val isSelected = option == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(ShapeTokens.Medium))
                        .background(if (isSelected) KhataGoGreenSoft else Color.Transparent)
                        .clickable { onSelected(option) }
                        .padding(horizontal = Spacing.L, vertical = Spacing.M),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = optionLabel(option),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) KhataGoGreen else InkPrimary
                    )
                }
            }
        }
    }
}

/** Segmented control used for frequency, direction and tabs. */
@Composable
fun <T> KhataGoSegmentedControl(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ShapeTokens.Medium))
            .background(SurfaceSubtle)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(ShapeTokens.Small))
                    .background(if (isSelected) SurfaceWhite else Color.Transparent)
                    .clickable { onSelected(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) KhataGoGreen else InkSecondary
                )
            }
        }
    }
}

@Composable
fun KhataGoChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(ShapeTokens.Small))
            .clickable { onClick() },
        shape = RoundedCornerShape(ShapeTokens.Small),
        color = if (selected) KhataGoGreen else SurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) KhataGoGreen else OutlineSoft
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else InkSecondary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun KhataGoSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.S),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = InkPrimary)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = KhataGoGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = OutlineStrong
            )
        )
    }
}

@Composable
fun KhataGoSettingsRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Spacing.L, vertical = Spacing.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = InkPrimary)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun KhataGoGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ShapeTokens.Large),
        color = SurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSoft)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun GroupDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DividerSoft)
    )
}

/**
 * Category picker: a select field backed by a bottom sheet, with an optional
 * "add a new category" affordance so users never get stuck on a preset.
 */
@Composable
fun KhataGoCategorySheet(
    options: List<String>,
    selected: String,
    label: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCreate: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = InkSecondary,
            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(fieldShape)
                .border(1.dp, OutlineSoft, fieldShape)
                .background(SurfaceWhite)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selected.ifBlank { "Select" },
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected.isBlank()) InkTertiary else InkPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Choose $label",
                tint = InkTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (expanded) {
        KhataGoBottomSheet(title = label, onDismiss = { expanded = false }) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(options) { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelected(option)
                                expanded = false
                            }
                            .padding(horizontal = Spacing.XXL, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (option == selected) KhataGoGreen else InkPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (onCreate != null) {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = Spacing.XXL, vertical = Spacing.S)
                        ) {
                            OutlinedTextField(
                                value = draft,
                                onValueChange = { draft = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("New category") },
                                singleLine = true,
                                shape = fieldShape,
                                colors = fieldColors(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (draft.isNotBlank()) {
                                            onCreate(draft.trim())
                                            draft = ""
                                            expanded = false
                                        }
                                    }
                                )
                            )
                            Spacer(Modifier.height(Spacing.XS))
                            KhataGoTextButton(
                                text = "Add category",
                                onClick = {
                                    if (draft.isNotBlank()) {
                                        onCreate(draft.trim())
                                        draft = ""
                                        expanded = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
