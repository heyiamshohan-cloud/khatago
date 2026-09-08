package com.shohan.khatago.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.core.time.KhataGoTime
import com.shohan.khatago.domain.model.AccountKind
import com.shohan.khatago.domain.model.DueState
import com.shohan.khatago.domain.model.InstallmentStatus
import com.shohan.khatago.domain.model.LedgerEntry
import com.shohan.khatago.domain.model.TransactionType
import com.shohan.khatago.ui.theme.InkPrimary
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.Info
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.KhataGoGreenSoft
import com.shohan.khatago.ui.theme.Negative
import com.shohan.khatago.ui.theme.Positive
import com.shohan.khatago.ui.theme.ShapeTokens
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceSubtle
import com.shohan.khatago.ui.theme.Upcoming

/**
 * Rows and tiles: the building blocks of every list in KhataGo.
 */

@Composable
fun KhataGoQuickActionTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(ShapeTokens.Large))
            .clickable { onClick() }
            .padding(vertical = Spacing.M, horizontal = Spacing.S),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(KhataGoGreenSoft, RoundedCornerShape(ShapeTokens.Medium)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = KhataGoGreen,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = InkPrimary,
            modifier = Modifier.padding(top = Spacing.S)
        )
    }
}

/** One line in the activity timeline and the Transactions screen. */
@Composable
fun KhataGoTransactionRow(
    entry: LedgerEntry,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (icon, tint, background) = iconFor(entry.type)
    val amountColor = when {
        entry.type.countsAsIncome -> Positive
        entry.type.isCashOut -> Negative
        else -> InkPrimary
    }
    val sign = when {
        entry.type.countsAsIncome -> "+"
        entry.type.isCashOut -> "−"
        else -> ""
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = Spacing.L, vertical = Spacing.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(background, RoundedCornerShape(ShapeTokens.Medium)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(Spacing.M))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.description.ifBlank { entry.type.label },
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(KhataGoTime.formatRelativeDate(entry.date))
                    append(" · ")
                    append(KhataGoTime.formatTime(entry.timestamp))
                    if (entry.category.isNotBlank()) {
                        append(" · ")
                        append(entry.category)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(Spacing.S))
        Text(
            text = "$sign${Money.format(entry.amount)}",
            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
            color = amountColor
        )
    }
}

@Composable
private fun iconFor(type: TransactionType): Triple<ImageVector, Color, Color> = when (type) {
    TransactionType.INCOME -> Triple(
        Icons.Outlined.TrendingUp,
        Positive,
        com.shohan.khatago.ui.theme.PositiveSoft
    )
    TransactionType.EXPENSE -> Triple(
        Icons.Outlined.TrendingDown,
        Negative,
        com.shohan.khatago.ui.theme.NegativeSoft
    )
    TransactionType.SHOP_CREDIT, TransactionType.SHOP_PAYMENT -> Triple(
        Icons.Outlined.Storefront,
        KhataGoGreen,
        KhataGoGreenSoft
    )
    TransactionType.LOAN, TransactionType.LOAN_PAYMENT -> Triple(
        Icons.Outlined.AccountBalance,
        KhataGoGreen,
        KhataGoGreenSoft
    )
    TransactionType.EMI_PURCHASE, TransactionType.EMI_PAYMENT -> Triple(
        Icons.Outlined.CreditCard,
        Upcoming,
        com.shohan.khatago.ui.theme.UpcomingSoft
    )
    else -> Triple(
        Icons.Outlined.Person,
        Info,
        com.shohan.khatago.ui.theme.InfoSoft
    )
}

@Composable
fun KhataGoAccountRow(
    title: String,
    subtitle: String,
    remaining: Long,
    total: Long,
    kind: AccountKind,
    dueState: DueState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    nextDueLabel: String? = null
) {
    val badge = badgeFor(dueState)
    KhataGoCard(
        modifier = modifier,
        onClick = onClick,
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.padding(Spacing.CardInner)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (kind == AccountKind.SHOP_CREDIT) KhataGoGreenSoft else SurfaceSubtle,
                            RoundedCornerShape(ShapeTokens.Medium)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconForKind(kind),
                        contentDescription = null,
                        tint = KhataGoGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.M))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (badge != null) {
                    KhataGoStatusBadge(text = badge.first, tone = badge.second)
                }
            }

            Spacer(modifier = Modifier.size(Spacing.M))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Amount due",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSecondary
                    )
                    Text(
                        text = Money.format(remaining),
                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                        color = InkPrimary
                    )
                }
                if (nextDueLabel != null) {
                    Text(
                        text = nextDueLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }
            }

            if (total > 0L) {
                KhataGoProgress(
                    progress = ((total - remaining).toFloat() / total.toFloat()).coerceIn(0f, 1f),
                    modifier = Modifier.padding(top = Spacing.M)
                )
            }
        }
    }
}

@Composable
fun iconForKind(kind: AccountKind): ImageVector = when (kind) {
    AccountKind.SHOP_CREDIT -> Icons.Outlined.Storefront
    AccountKind.LOAN -> Icons.Outlined.AccountBalance
    AccountKind.EMI -> Icons.Outlined.CreditCard
    AccountKind.PERSONAL_DEBT -> Icons.Outlined.Person
}

@Composable
fun badgeFor(state: DueState): Pair<String, BadgeTone>? = when (state) {
    DueState.OVERDUE -> "Overdue" to BadgeTone.NEGATIVE
    DueState.DUE_TODAY -> "Due Today" to BadgeTone.WARNING
    DueState.DUE_SOON -> "Due Soon" to BadgeTone.INFO
    DueState.SETTLED -> "Paid" to BadgeTone.POSITIVE
    DueState.SCHEDULED -> null
    DueState.UNSCHEDULED -> null
}

@Composable
fun badgeForInstallment(status: InstallmentStatus): Pair<String, BadgeTone> = when (status) {
    InstallmentStatus.PAID -> "Paid" to BadgeTone.POSITIVE
    InstallmentStatus.PARTIALLY_PAID -> "Partially Paid" to BadgeTone.INFO
    InstallmentStatus.OVERDUE -> "Overdue" to BadgeTone.NEGATIVE
    InstallmentStatus.DUE_TODAY -> "Due Today" to BadgeTone.WARNING
    InstallmentStatus.UPCOMING -> "Upcoming" to BadgeTone.NEUTRAL
}

/** A row in Upcoming Payments. */
@Composable
fun KhataGoUpcomingRow(
    dateLabel: String,
    title: String,
    subtitle: String,
    amount: Long,
    dueState: DueState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = Spacing.L, vertical = Spacing.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(72.dp)) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (dueState == DueState.OVERDUE) Negative else InkPrimary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(Spacing.S))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = Money.format(amount),
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                color = InkPrimary
            )
            badgeFor(dueState)?.let { (text, tone) ->
                KhataGoStatusBadge(text = text, tone = tone, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun KhataGoKeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = InkPrimary,
    emphasize: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary
        )
        Text(
            text = value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
            color = valueColor
        )
    }
}
