package com.improvingmuslim.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private fun materialSchemeFrom(brand: BrandColors, dark: Boolean) =
    if (dark) {
        darkColorScheme(
            primary = brand.accent,
            onPrimary = brand.background,
            background = brand.background,
            onBackground = brand.ink,
            surface = brand.surface,
            onSurface = brand.ink,
            surfaceVariant = brand.strongSurface,
            outline = brand.line,
            error = brand.rose,
        )
    } else {
        lightColorScheme(
            primary = brand.accent,
            onPrimary = brand.background,
            background = brand.background,
            onBackground = brand.ink,
            surface = brand.surface,
            onSurface = brand.ink,
            surfaceVariant = brand.strongSurface,
            outline = brand.line,
            error = brand.rose,
        )
    }

@Composable
fun ImprovingMuslimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val brand = if (darkTheme) DarkBrandColors else LightBrandColors

    CompositionLocalProvider(LocalBrandColors provides brand) {
        MaterialTheme(
            colorScheme = materialSchemeFrom(brand, darkTheme),
            typography = Typography,
            content = content
        )
    }
}
