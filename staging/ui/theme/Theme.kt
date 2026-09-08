package com.shohan.khatago.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * KhataGo is a LIGHT MODE ONLY product.
 *
 * The device dark theme is intentionally ignored: [isSystemInDarkTheme] is never
 * consulted to switch palettes, so onboarding, dashboard, forms, dialogs, charts
 * and the lock screen stay light even when the system is in dark mode.
 */
private val KhataGoLightColorScheme = lightColorScheme(
    primary = KhataGoGreen,
    onPrimary = OnHero,
    primaryContainer = KhataGoGreenSoft,
    onPrimaryContainer = KhataGoGreenDeep,
    secondary = KhataGoGreenMuted,
    onSecondary = OnHero,
    secondaryContainer = KhataGoGreenSurface,
    onSecondaryContainer = KhataGoGreenDeep,
    tertiary = Info,
    onTertiary = OnHero,
    tertiaryContainer = InfoSoft,
    onTertiaryContainer = OnInfoSoft,
    background = CanvasWhite,
    onBackground = InkPrimary,
    surface = SurfaceWhite,
    onSurface = InkPrimary,
    surfaceVariant = SurfaceSubtle,
    onSurfaceVariant = InkSecondary,
    surfaceTint = KhataGoGreen,
    outline = OutlineSoft,
    outlineVariant = OutlineStrong,
    error = Negative,
    onError = OnHero,
    errorContainer = NegativeSoft,
    onErrorContainer = OnNegativeSoft,
    scrim = Scrim,
    surfaceContainerLowest = SurfaceWhite,
    surfaceContainerLow = SurfaceSubtle,
    surfaceContainer = SurfaceSubtle,
    surfaceContainerHigh = KhataGoGreenSurface,
    surfaceContainerHighest = OutlineSoft
)

@Composable
fun KhataGoTheme(
    // Light mode only — this parameter exists solely so the preview tooling and
    // callers can render consistently; dark mode is never enabled.
    forceLight: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = KhataGoLightColorScheme,
        typography = KhataGoTypography,
        shapes = KhataGoShapes,
        content = content
    )
}
