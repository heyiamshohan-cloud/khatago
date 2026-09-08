package com.shohan.khatago.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.ShowChart
import com.shohan.khatago.ui.theme.DividerSoft
import com.shohan.khatago.ui.theme.InkPrimary
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.KhataGoGreenSoft
import com.shohan.khatago.ui.theme.OnHero
import com.shohan.khatago.ui.theme.OutlineSoft
import com.shohan.khatago.ui.theme.ShapeTokens
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceSubtle
import com.shohan.khatago.ui.theme.SurfaceWhite

/**
 * KhataGo surfaces.
 *
 * Cards are used deliberately — hero card for the headline number, stat card for
 * a metric, chart card for analytics. Nothing nests a card inside a card.
 */

@Composable
fun KhataGoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceWhite,
    borderColor: Color = OutlineSoft,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(ShapeTokens.Large)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        content = content
    )
}

@Composable
fun KhataGoSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.XS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = InkPrimary
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = KhataGoGreen,
                modifier = Modifier
                    .clip(RoundedCornerShape(ShapeTokens.Small))
                    .clickable { onAction() }
                    .padding(horizontal = Spacing.S, vertical = Spacing.XS)
            )
        }
    }
}

/** Compact, intentional empty state — never a giant blank card. */
@Composable
fun KhataGoEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.ShowChart,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.XL, vertical = Spacing.XXL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.S)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(ShapeTokens.Large))
                .background(KhataGoGreenSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = KhataGoGreen,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = InkPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            KhataGoSecondaryButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.padding(top = Spacing.XS)
            )
        }
    }
}

enum class BadgeTone { POSITIVE, NEGATIVE, WARNING, INFO, NEUTRAL }

/**
 * Status is never communicated by colour alone: every badge carries its own
 * label ("Paid", "Overdue", "Due Today").
 */
@Composable
fun KhataGoStatusBadge(
    text: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier
) {
    val (background, foreground) = when (tone) {
        BadgeTone.POSITIVE -> com.shohan.khatago.ui.theme.PositiveSoft to com.shohan.khatago.ui.theme.OnPositiveSoft
        BadgeTone.NEGATIVE -> com.shohan.khatago.ui.theme.NegativeSoft to com.shohan.khatago.ui.theme.OnNegativeSoft
        BadgeTone.WARNING -> com.shohan.khatago.ui.theme.UpcomingSoft to com.shohan.khatago.ui.theme.OnUpcomingSoft
        BadgeTone.INFO -> com.shohan.khatago.ui.theme.InfoSoft to com.shohan.khatago.ui.theme.OnInfoSoft
        BadgeTone.NEUTRAL -> SurfaceSubtle to InkSecondary
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(ShapeTokens.Small),
        color = background,
        contentColor = foreground
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Spacing.S, vertical = 3.dp)
        )
    }
}

@Composable
fun KhataGoProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = KhataGoGreen,
    trackColor: Color = SurfaceSubtle
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
    }
}

@Composable
fun KhataGoDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DividerSoft)
    )
}

/** Small pill used for hero surfaces where text sits on the green background. */
@Composable
fun HeroPill(text: String) {
    Surface(
        shape = RoundedCornerShape(ShapeTokens.Small),
        color = Color.White.copy(alpha = 0.16f),
        contentColor = OnHero
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Spacing.S, vertical = 3.dp)
        )
    }
}
