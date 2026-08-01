package com.improvingmuslim.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Shared semantic palette, matching the iOS app's Brand tokens so the two native
 * apps clearly belong to the same product. Values come from ios/PRODUCT.md.
 */
data class BrandColors(
    val background: Color,
    val surface: Color,
    val strongSurface: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val gold: Color,
    val rose: Color,
)

val LightBrandColors = BrandColors(
    background = Color(0xFFF7F3EC),
    surface = Color(0xFFFFFDF8),
    strongSurface = Color(0xFFFFFFFF),
    ink = Color(0xFF18201B),
    muted = Color(0xFF66706A),
    line = Color(0xFFDED6C8),
    accent = Color(0xFF176B5B),
    gold = Color(0xFFC89B3C),
    rose = Color(0xFFA6484F),
)

val DarkBrandColors = BrandColors(
    background = Color(0xFF101714),
    surface = Color(0xFF17211D),
    strongSurface = Color(0xFF1D2A25),
    ink = Color(0xFFEEF4ED),
    muted = Color(0xFFAAB8B0),
    line = Color(0xFF33433C),
    accent = Color(0xFF6CC4AD),
    gold = Color(0xFFD7B45C),
    rose = Color(0xFFE08C94),
)

val LocalBrandColors = staticCompositionLocalOf { LightBrandColors }

/** Convenience accessor: `Brand.colors.accent` from any composable. */
object Brand {
    val colors: BrandColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBrandColors.current
}
