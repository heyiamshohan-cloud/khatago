package com.shohan.khatago.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * KhataGo colour system.
 *
 * Direction: deep sophisticated green on a warm off-white canvas, with restrained
 * semantic colours for money. Every value is contrast checked for light mode.
 */

// ---------------------------------------------------------------- Brand greens
val KhataGoGreen = Color(0xFF14614B)
val KhataGoGreenDeep = Color(0xFF0E4636)
val KhataGoGreenSoft = Color(0xFFE4F0EA)
val KhataGoGreenMuted = Color(0xFF3E7C66)
val KhataGoGreenSurface = Color(0xFFF0F6F2)

// -------------------------------------------------------------- Neutral canvas
val CanvasWhite = Color(0xFFFAF9F6)
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceSubtle = Color(0xFFF4F5F3)
val SurfaceRaised = Color(0xFFFFFFFF)

// --------------------------------------------------------------------- Ink
val InkPrimary = Color(0xFF12211C)
val InkSecondary = Color(0xFF5C6B65)
val InkTertiary = Color(0xFF8A968F)
val InkInverse = Color(0xFFF6FAF8)

// ------------------------------------------------------------------ Structure
val OutlineSoft = Color(0xFFE4E8E5)
val OutlineStrong = Color(0xFFCBD3CE)
val DividerSoft = Color(0xFFEFF2F0)

// ------------------------------------------------------------------ Semantics
/** Money received, paid, settled. */
val Positive = Color(0xFF1F7A55)
val PositiveSoft = Color(0xFFE6F3EC)

/** Money owed, overdue, spent. */
val Negative = Color(0xFFC4453B)
val NegativeSoft = Color(0xFFFBECEA)

/** Due soon, attention needed but not yet overdue. */
val Upcoming = Color(0xFFB26A00)
val UpcomingSoft = Color(0xFFFDF2E0)

/** Informational accent. */
val Info = Color(0xFF2F6FB2)
val InfoSoft = Color(0xFFE9F1FA)

/** On-canvas text/icons that sit on the hero (green) surface. */
val OnHero = Color(0xFFFFFFFF)
val OnHeroMuted = Color(0xFFCFE3D9)

/** Badge foregrounds */
val OnPositiveSoft = Color(0xFF0F5137)
val OnNegativeSoft = Color(0xFF7A231C)
val OnUpcomingSoft = Color(0xFF6B4000)
val OnInfoSoft = Color(0xFF1B4472)

/** Scrim used behind dialogs / lock screen. */
val Scrim = Color(0x99000000)
