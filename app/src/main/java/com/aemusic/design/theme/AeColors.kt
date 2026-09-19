package com.aemusic.design.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/**
 * AeMusic V2 visual foundation palette.
 *
 * The values are intentionally a first design candidate, not a permanent brand contract.  What is
 * stable here is the *role* each color plays.  Screens should consume Material color roles instead
 * of scattering raw color literals.
 */
private object AePalette {
    val Violet = Color(0xFF7057F5)
    val VioletLight = Color(0xFFC9BEFF)
    val VioletContainer = Color(0xFFE8E2FF)
    val VioletContainerDark = Color(0xFF3B2A8B)

    val Ink = Color(0xFF19171D)
    val Paper = Color(0xFFFAF8FC)

    val DarkCanvas = Color(0xFF111014)
    val DarkSurface = Color(0xFF19171D)
    val DarkSurfaceRaised = Color(0xFF232027)
    val DarkOutline = Color(0xFF928D98)

    val LightSurface = Color(0xFFFFFBFF)
    val LightSurfaceRaised = Color(0xFFF1EDF4)
    val LightOutline = Color(0xFF7A757F)
}

internal val AeLightColorScheme = lightColorScheme(
    primary = AePalette.Violet,
    onPrimary = Color.White,
    primaryContainer = AePalette.VioletContainer,
    onPrimaryContainer = Color(0xFF20105D),
    background = AePalette.Paper,
    onBackground = AePalette.Ink,
    surface = AePalette.LightSurface,
    onSurface = AePalette.Ink,
    surfaceVariant = AePalette.LightSurfaceRaised,
    onSurfaceVariant = Color(0xFF625E67),
    outline = AePalette.LightOutline,
    outlineVariant = Color(0xFFCCC6D0),
)

internal val AeDarkColorScheme = darkColorScheme(
    primary = AePalette.VioletLight,
    onPrimary = Color(0xFF2A176D),
    primaryContainer = AePalette.VioletContainerDark,
    onPrimaryContainer = Color(0xFFE8E2FF),
    background = AePalette.DarkCanvas,
    onBackground = Color(0xFFE9E4EC),
    surface = AePalette.DarkSurface,
    onSurface = Color(0xFFE9E4EC),
    surfaceVariant = AePalette.DarkSurfaceRaised,
    onSurfaceVariant = Color(0xFFCCC5D0),
    outline = AePalette.DarkOutline,
    outlineVariant = Color(0xFF49454D),
)

internal val AeOledColorScheme = AeDarkColorScheme.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF080808),
    surfaceContainer = Color(0xFF0D0D0F),
    surfaceContainerHigh = Color(0xFF141416),
    surfaceContainerHighest = Color(0xFF1B1B1E),
)
