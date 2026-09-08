package com.shohan.khatago.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shohan.khatago.ui.components.KhataGoPrimaryButton
import com.shohan.khatago.ui.components.KhataGoTextButton
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkPrimary
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.KhataGoGreenSoft
import com.shohan.khatago.ui.theme.Negative
import com.shohan.khatago.ui.theme.OutlineSoft
import com.shohan.khatago.ui.theme.Positive
import com.shohan.khatago.ui.theme.ShapeTokens
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.theme.SurfaceWhite
import com.shohan.khatago.ui.theme.Upcoming
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val message: String,
    val art: @Composable () -> Unit
)

/**
 * Onboarding: four calm, purposeful screens. Copy stays human and short, and the
 * artwork is drawn — no stock illustrations, no external assets.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = onboardingPages()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .padding(horizontal = Spacing.Gutter)
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Spacer(Modifier.height(Spacing.M))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BrandLockup()
            if (pagerState.currentPage < pages.size - 1) {
                KhataGoTextButton(text = "Skip", onClick = onSkip)
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { index ->
            val page = pages[index]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = Spacing.XXL),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                page.art()
                Spacer(Modifier.height(Spacing.XXXL))
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(Spacing.M))
                Text(
                    text = page.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Spacing.S)
                )
            }
        }

        PageIndicator(
            count = pages.size,
            selected = pagerState.currentPage,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(Spacing.XL))

        if (pagerState.currentPage == pages.size - 1) {
            KhataGoPrimaryButton(
                text = "Get Started",
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            KhataGoPrimaryButton(
                text = "Continue",
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(Spacing.L))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun BrandLockup() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(KhataGoGreen, RoundedCornerShape(ShapeTokens.Small)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "K",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
        Spacer(Modifier.width(Spacing.S))
        Text(
            text = "KhataGo",
            style = MaterialTheme.typography.titleLarge,
            color = InkPrimary
        )
    }
}

@Composable
private fun PageIndicator(count: Int, selected: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.XS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val width by animateFloatAsState(
                targetValue = if (index == selected) 20f else 6f,
                animationSpec = tween(250),
                label = "indicator"
            )
            Box(
                modifier = Modifier
                    .size(width = width.dp, height = 6.dp)
                    .background(
                        if (index == selected) KhataGoGreen else OutlineSoft,
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun onboardingPages(): List<OnboardingPage> = listOf(
    OnboardingPage(
        title = "All your finances,\nin one place.",
        message = "Shop credit, loans, EMIs, personal debt, income and expenses — one calm place to see all of it.",
        art = { ArtOverview() }
    ),
    OnboardingPage(
        title = "Stay on top of\nwhat you owe.",
        message = "Track shop credit, loans, EMIs and personal debt in one place, with real payment schedules.",
        art = { ArtObligations() }
    ),
    OnboardingPage(
        title = "See where your\nmoney goes.",
        message = "Track income, expenses, payments and financial trends without a spreadsheet.",
        art = { ArtTrends() }
    ),
    OnboardingPage(
        title = "Private by design.",
        message = "Your financial records stay on your device. No account, no ads, no tracking.",
        art = { ArtPrivacy() }
    )
)

// ------------------------------------------------------------------ artwork

@Composable
private fun ArtOverview() {
    OnboardingCanvasFrame { width, height ->
        val cardWidth = width * 0.62f
        val cardHeight = height * 0.58f
        val left = (width - cardWidth) / 2f
        drawRoundRect(
            color = KhataGoGreen,
            topLeft = Offset(left, height * 0.14f),
            size = Size(cardWidth, cardHeight),
            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.16f),
            topLeft = Offset(left + 18.dp.toPx(), height * 0.14f + 26.dp.toPx()),
            size = Size(cardWidth * 0.42f, 10.dp.toPx()),
            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left + 18.dp.toPx(), height * 0.14f + 52.dp.toPx()),
            size = Size(cardWidth * 0.62f, 22.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        // Accent chips
        drawRoundRect(
            color = Positive,
            topLeft = Offset(left - 22.dp.toPx(), height * 0.52f),
            size = Size(74.dp.toPx(), 34.dp.toPx()),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        drawRoundRect(
            color = Upcoming,
            topLeft = Offset(left + cardWidth - 52.dp.toPx(), height * 0.62f),
            size = Size(84.dp.toPx(), 34.dp.toPx()),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
    }
}

@Composable
private fun ArtObligations() {
    OnboardingCanvasFrame { width, height ->
        val barWidth = width * 0.11f
        val gap = width * 0.05f
        val totalWidth = barWidth * 4 + gap * 3
        var x = (width - totalWidth) / 2f
        val values = listOf(0.45f, 0.8f, 0.35f, 0.62f)
        values.forEachIndexed { index, value ->
            val barHeight = height * 0.5f * value
            drawRoundRect(
                color = if (index == 1) KhataGoGreen else KhataGoGreenSoft,
                topLeft = Offset(x, height * 0.72f - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
            x += barWidth + gap
        }
        drawLine(
            color = OutlineSoft,
            start = Offset(width * 0.12f, height * 0.72f),
            end = Offset(width * 0.88f, height * 0.72f),
            strokeWidth = 2.dp.toPx()
        )
        drawRoundRect(
            color = Negative,
            topLeft = Offset(width * 0.12f, height * 0.2f),
            size = Size(width * 0.3f, 12.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
    }
}

@Composable
private fun ArtTrends() {
    OnboardingCanvasFrame { width, height ->
        val startX = width * 0.1f
        val endX = width * 0.9f
        val baseline = height * 0.74f
        drawLine(
            color = OutlineSoft,
            start = Offset(startX, baseline),
            end = Offset(endX, baseline),
            strokeWidth = 2.dp.toPx()
        )
        val points = listOf(0.2f, 0.42f, 0.3f, 0.66f, 0.52f, 0.86f)
        val step = (endX - startX) / (points.size - 1)
        points.forEachIndexed { index, value ->
            val x = startX + step * index
            val barHeight = height * 0.5f * value
            drawRoundRect(
                color = if (index % 2 == 0) KhataGoGreen else KhataGoGreenSoft,
                topLeft = Offset(x - step * 0.22f, baseline - barHeight),
                size = Size(step * 0.44f, barHeight),
                cornerRadius = CornerRadius(step * 0.22f, step * 0.22f)
            )
        }
        drawRoundRect(
            color = SurfaceWhite,
            topLeft = Offset(width * 0.58f, height * 0.16f),
            size = Size(width * 0.32f, height * 0.16f),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        drawRoundRect(
            color = Positive,
            topLeft = Offset(width * 0.62f, height * 0.21f),
            size = Size(width * 0.16f, 8.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
    }
}

@Composable
private fun ArtPrivacy() {
    OnboardingCanvasFrame { width, height ->
        val size = width * 0.46f
        val left = (width - size) / 2f
        val top = height * 0.18f
        drawRoundRect(
            color = KhataGoGreen,
            topLeft = Offset(left, top),
            size = Size(size, size),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left + size * 0.34f, top + size * 0.2f),
            size = Size(size * 0.32f, size * 0.26f),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left + size * 0.28f, top + size * 0.46f),
            size = Size(size * 0.44f, size * 0.3f),
            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
        )
        drawCircle(
            color = KhataGoGreenSoft,
            radius = size * 0.72f,
            center = Offset(width / 2f, top + size / 2f),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
private fun OnboardingCanvasFrame(
    draw: androidx.compose.ui.graphics.drawscope.DrawScope.(width: Float, height: Float) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        draw(size.width, size.height)
    }
}
