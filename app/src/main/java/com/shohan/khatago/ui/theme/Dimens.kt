package com.shohan.khatago.ui.theme

import androidx.compose.ui.unit.dp

/**
 * KhataGo spacing scale. One source of truth keeps every screen on the same grid,
 * which is what keeps left/right margins visually balanced across the app.
 */
object Spacing {
    val XS = 4.dp
    val S = 8.dp
    val M = 12.dp
    val L = 16.dp
    val XL = 20.dp
    val XXL = 24.dp
    val XXXL = 32.dp

    /** Standard horizontal page gutter. */
    val Gutter = 20.dp

    /** Vertical rhythm between sections inside a scrolling page. */
    val SectionGap = 24.dp

    /** Gap between cards inside one section. */
    val CardGap = 12.dp

    /** Inner padding of a standard card. */
    val CardInner = 16.dp

    /** Space reserved above a header, under the status bar. */
    val SafeTop = 8.dp
}

object ShapeTokens {
    val Small = 10.dp
    val Medium = 14.dp
    val Large = 18.dp
    val XLarge = 24.dp
    val Hero = 28.dp
}

object ElevationTokens {
    val None = 0.dp
    val Soft = 1.dp
    val Card = 2.dp
    val Raised = 4.dp
}

object SizeTokens {
    /** Minimum accessible touch target (above the 48dp platform guideline). */
    val MinTouch = 52.dp
    val IconButton = 40.dp
    val Avatar = 36.dp
    val HeroCardMinHeight = 168.dp
    val QuickActionTile = 92.dp
    val ChartHeight = 148.dp
    val BottomBarHeight = 64.dp
}
