package com.shohan.khatago.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.domain.model.MonthFlow
import com.shohan.khatago.ui.theme.DividerSoft
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.InkTertiary
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.Negative
import com.shohan.khatago.ui.theme.SizeTokens
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.Upcoming
import kotlin.math.max

/**
 * Charts are drawn with Canvas — no charting dependency, complete control over
 * the restrained KhataGo look, and every value comes from the database.
 */

data class DonutSlice(val label: String, val value: Long, val color: Color)

@Composable
fun KhataGoBarChart(
    data: List<MonthFlow>,
    modifier: Modifier = Modifier,
    showPayments: Boolean = false
) {
    if (data.isEmpty()) return

    val animation = remember { Animatable(0f) }
    LaunchedEffect(data.map { it.period }.joinToString()) {
        animation.snapTo(0f)
        animation.animateTo(1f, tween(durationMillis = 650, easing = FastOutSlowInEasing))
    }

    val peak = data.maxOf { maxOf(it.income, it.expense, if (showPayments) it.payments else 0L) }
        .coerceAtLeast(1L)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(SizeTokens.ChartHeight)
        ) {
            val baseline = size.height - 10.dp.toPx()
            val groupWidth = size.width / data.size
            val seriesCount = if (showPayments) 3 else 2
            val barWidth = (groupWidth * 0.62f) / seriesCount
            val gap = barWidth * 0.22f

            // Baseline
            drawLine(
                color = DividerSoft,
                start = Offset(0f, baseline),
                end = Offset(size.width, baseline),
                strokeWidth = 1.dp.toPx()
            )

            data.forEachIndexed { index, point ->
                val groupStart = index * groupWidth + (groupWidth * 0.19f)
                val values = if (showPayments) {
                    listOf(point.income to KhataGoGreen, point.expense to Negative, point.payments to Upcoming)
                } else {
                    listOf(point.income to KhataGoGreen, point.expense to Negative)
                }
                values.forEachIndexed { seriesIndex, (value, color) ->
                    val ratio = (value.toFloat() / peak.toFloat()) * animation.value
                    val height = (baseline - 8.dp.toPx()) * ratio
                    val left = groupStart + seriesIndex * (barWidth + gap)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left, baseline - height),
                        size = Size(barWidth, height.coerceAtLeast(1f)),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            data.forEach { point ->
                Text(
                    text = point.label.take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = InkTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = Spacing.XS)
                )
            }
        }
    }
}

/** Donut chart with the total in the middle; slices are real amounts. */
@Composable
fun KhataGoDonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    centerLabel: String = "Total",
    centerValue: String = ""
) {
    if (slices.isEmpty()) return
    val total = slices.sumOf { it.value }.coerceAtLeast(1L)

    val animation = remember { Animatable(0f) }
    LaunchedEffect(slices.map { it.label to it.value }.joinToString()) {
        animation.snapTo(0f)
        animation.animateTo(1f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = modifier.size(168.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            var startAngle = -90f
            slices.forEach { slice ->
                val sweep = (slice.value.toFloat() / total.toFloat()) * 360f * animation.value
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweep.coerceAtLeast(0f),
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth)
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = InkSecondary
            )
            Text(
                text = centerValue.ifBlank { Money.formatShort(total) },
                style = MaterialTheme.typography.titleLarge,
                color = com.shohan.khatago.ui.theme.InkPrimary
            )
        }
    }
}

/** Compact horizontal bars used for category and debt breakdowns. */
@Composable
fun KhataGoBarBreakdown(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier
) {
    if (slices.isEmpty()) return
    val total = slices.sumOf { it.value }.coerceAtLeast(1L)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.M)
    ) {
        slices.forEach { slice ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = slice.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = com.shohan.khatago.ui.theme.InkPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = Money.format(slice.value),
                        style = MaterialTheme.typography.titleSmall,
                        color = com.shohan.khatago.ui.theme.InkPrimary
                    )
                }
                KhataGoProgress(
                    progress = slice.value.toFloat() / total.toFloat(),
                    color = slice.color,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
fun KhataGoChartLegend(items: List<Pair<String, Color>>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.L)
    ) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, androidx.compose.foundation.shape.CircleShape)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
