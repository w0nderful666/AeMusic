package com.aemusic.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.aemusic.design.effect.AeGlassSpec

internal val LocalAeVisualStyle = staticCompositionLocalOf { AeVisualStyle.MATERIAL }
internal val LocalAeGlassSpec = staticCompositionLocalOf { AeGlassSpec.Default }
val LocalAeCanvasPalette = staticCompositionLocalOf {
    AeCanvasPalette(listOf(Color.Transparent), Color.Unspecified, Color.Unspecified)
}

/**
 * The single theme entry point for AeMusic UI.
 *
 * Light/Dark and visual material are deliberately separate concerns:
 * - [colorMode] follows the operating system by default and can explicitly select OLED.
 * - [visualStyle] defaults to low-cost Material rendering.
 * - Glass is opt-in and configured centrally instead of being hard-coded into screens.
 */
@Composable
fun AeTheme(
    colorMode: AeColorMode = AeColorMode.SYSTEM,
    visualStyle: AeVisualStyle = AeVisualStyle.MATERIAL,
    glassSpec: AeGlassSpec = AeGlassSpec.Default,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (colorMode) {
        AeColorMode.SYSTEM -> if (systemDark) AeDarkColorScheme else AeLightColorScheme
        AeColorMode.LIGHT -> AeLightColorScheme
        AeColorMode.DARK -> AeDarkColorScheme
        AeColorMode.OLED -> AeOledColorScheme
    }
    CompositionLocalProvider(
        LocalAeVisualStyle provides visualStyle,
        LocalAeGlassSpec provides glassSpec.normalized(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AeTypography,
            shapes = AeShapes,
        ) {
            CompositionLocalProvider(
                LocalAeCanvasPalette provides AeCanvasPalette(
                    backgroundStops = listOf(colorScheme.background),
                    content = colorScheme.onBackground,
                    contentVariant = colorScheme.onSurfaceVariant,
                ),
                content = content,
            )
        }
    }
}
