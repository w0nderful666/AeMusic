package com.aemusic.design.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.aemusic.design.theme.AeVisualStyle
import com.aemusic.design.theme.LocalAeGlassSpec
import com.aemusic.design.theme.LocalAeVisualStyle

/**
 * Semantic surface hierarchy shared by Material and optional Glass rendering.
 *
 * Screens describe *what kind of surface* they need.  They do not know whether that surface is
 * rendered as opaque Material or optional Glass.  This is the seam that lets AeMusic keep Material
 * as a low-cost default while evolving Glass independently.
 */
enum class AeSurfaceRole {
    Canvas,
    Content,
    Raised,
    Floating,
    Immersive,
}

/**
 * Foundation surface for AeMusic.
 * Implements Apple WWDC & Haze 2.0 Material Hierarchy:
 * - Content Layer: Never glass. Pure readable surface.
 * - Persistent/Transient UI Layer: Liquid Glass with specular edge refraction,
 *   subtle water-film sheen, and dynamic background blur.
 * - Graceful fallback to pure Material for low-spec devices when glass is disabled.
 */
@Composable
fun AeSurface(
    role: AeSurfaceRole,
    modifier: Modifier = Modifier,
    shape: Shape = defaultShape(role),
    content: @Composable BoxScope.() -> Unit,
) {
    val visualStyle = LocalAeVisualStyle.current
    val glassSpec = LocalAeGlassSpec.current
    val supportsGlass = role == AeSurfaceRole.Raised || role == AeSurfaceRole.Floating
    val renderGlass = visualStyle == AeVisualStyle.GLASS && glassSpec.enabled && supportsGlass

    val materialColor = materialColor(role)
    val contentColor = contentColorFor(materialColor)
    val surfaceColor = if (renderGlass) {
        materialColor.copy(alpha = glassSpec.surfaceOpacity)
    } else {
        materialColor
    }

    val border = if (renderGlass && glassSpec.refractionStrength > 0.05f) {
        BorderStroke(
            width = glassSpec.borderWidthDp.dp,
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = (0.32f * glassSpec.refractionStrength).coerceIn(0.04f, 0.45f)),
                    Color.White.copy(alpha = (0.08f * glassSpec.refractionStrength).coerceIn(0.01f, 0.20f)),
                    Color.White.copy(alpha = (0.22f * glassSpec.refractionStrength).coerceIn(0.02f, 0.35f)),
                ),
            ),
        )
    } else {
        null
    }

    val shadowElevation = if (renderGlass) {
        0.dp
    } else {
        when (role) {
            AeSurfaceRole.Raised -> 2.dp
            AeSurfaceRole.Floating -> 4.dp
            else -> 0.dp
        }
    }

    Surface(
        modifier = modifier.clip(shape),
        shape = shape,
        color = surfaceColor,
        contentColor = contentColor,
        border = border,
        shadowElevation = shadowElevation,
    ) {
        Box(modifier = Modifier.clip(shape)) {
            if (renderGlass) {
                // 1. Water film sheen (organic surface gleam)
                if (glassSpec.waterFilmStrength > 0.05f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(shape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f * glassSpec.waterFilmStrength),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                }
                // 2. Primary tint layer
                if (glassSpec.tintStrength > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(shape)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = glassSpec.tintStrength),
                            ),
                    )
                }
            }
            content()
        }
    }
}

private const val GlassEdgeHighlightAlpha = 0.18f

@Composable
private fun materialColor(role: AeSurfaceRole): Color = when (role) {
    AeSurfaceRole.Canvas -> MaterialTheme.colorScheme.background
    AeSurfaceRole.Content -> MaterialTheme.colorScheme.surface
    AeSurfaceRole.Raised -> MaterialTheme.colorScheme.surfaceContainerHigh
    AeSurfaceRole.Floating -> MaterialTheme.colorScheme.surfaceContainer
    AeSurfaceRole.Immersive -> MaterialTheme.colorScheme.surface
}

@Composable
private fun defaultShape(role: AeSurfaceRole): Shape = when (role) {
    AeSurfaceRole.Canvas,
    AeSurfaceRole.Content,
    AeSurfaceRole.Immersive,
    -> RectangleShape

    AeSurfaceRole.Raised -> MaterialTheme.shapes.large
    AeSurfaceRole.Floating -> MaterialTheme.shapes.extraLarge
}
