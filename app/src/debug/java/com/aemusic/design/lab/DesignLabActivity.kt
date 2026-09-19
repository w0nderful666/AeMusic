package com.aemusic.design.lab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import com.aemusic.design.component.AeSurface
import com.aemusic.design.component.AeSurfaceRole
import com.aemusic.design.component.AeSectionHeader
import com.aemusic.design.component.AeStatePane
import com.aemusic.design.component.AeStatePaneState
import com.aemusic.design.component.AeTrackRow
import com.aemusic.design.effect.AeGlassSpec
import com.aemusic.design.theme.AeColorMode
import com.aemusic.design.theme.AeSpacing
import com.aemusic.design.theme.AeTheme
import com.aemusic.design.theme.AeVisualStyle
import kotlin.math.roundToInt

/**
 * Debug-only visual workbench.  It never ships in release/internal builds.
 *
 * This screen deliberately uses fake content and owns only temporary local controls.  It is a tool
 * for validating design tokens and whether a customization knob produces a meaningful visual
 * difference before we commit that knob to production settings.
 */
class DesignLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var colorMode by remember { mutableStateOf(AeColorMode.SYSTEM) }
            var visualStyle by remember { mutableStateOf(AeVisualStyle.GLASS) }
            var glassSpec by remember { mutableStateOf(AeGlassSpec.Default) }

            AeTheme(
                colorMode = colorMode,
                visualStyle = visualStyle,
                glassSpec = glassSpec,
            ) {
                DesignLabScreen(
                    colorMode = colorMode,
                    onColorModeChange = { colorMode = it },
                    visualStyle = visualStyle,
                    onVisualStyleChange = { visualStyle = it },
                    glassSpec = glassSpec,
                    onGlassSpecChange = { glassSpec = it },
                )
            }
        }
    }
}

@Composable
private fun DesignLabScreen(
    colorMode: AeColorMode,
    onColorModeChange: (AeColorMode) -> Unit,
    visualStyle: AeVisualStyle,
    onVisualStyleChange: (AeVisualStyle) -> Unit,
    glassSpec: AeGlassSpec,
    onGlassSpecChange: (AeGlassSpec) -> Unit,
) {
    AeSurface(
        role = AeSurfaceRole.Canvas,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AeSpacing.md, vertical = AeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AeSpacing.xl),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.xs)) {
                Text("AeMusic Design Lab", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Visual foundation only — no playback, database or network.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ThemeSection(colorMode, onColorModeChange)
            VisualMaterialSection(visualStyle, onVisualStyleChange)
            LabSection("Surface hierarchy") { AtmosphereDemo() }
            if (visualStyle == AeVisualStyle.GLASS) {
                GlassTuningSection(glassSpec, onGlassSpecChange)
                LabSection("Glass Playground") { GlassPlayground() }
            }
            CoreComponentsSection()
            ColorRolesSection()
            TypographySection()
            ShapeScaleSection()
            SpacingRhythmSection()
            Spacer(Modifier.height(AeSpacing.xl))
        }
    }
}

@Composable
private fun ThemeSection(
    colorMode: AeColorMode,
    onColorModeChange: (AeColorMode) -> Unit,
) {
    LabSection("Theme") {
        ChipRow {
            AeColorMode.entries.forEach { mode ->
                FilterChip(
                    selected = colorMode == mode,
                    onClick = { onColorModeChange(mode) },
                    label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }
    }
}

@Composable
private fun VisualMaterialSection(
    visualStyle: AeVisualStyle,
    onVisualStyleChange: (AeVisualStyle) -> Unit,
) {
    LabSection("Visual material") {
        ChipRow {
            FilterChip(
                selected = visualStyle == AeVisualStyle.MATERIAL,
                onClick = { onVisualStyleChange(AeVisualStyle.MATERIAL) },
                label = { Text("Material · default") },
            )
            FilterChip(
                selected = visualStyle == AeVisualStyle.GLASS,
                onClick = { onVisualStyleChange(AeVisualStyle.GLASS) },
                label = { Text("Glass · flagship") },
            )
        }
        Text(
            "Glass is optional. Content surfaces remain readable and mostly opaque; raised and floating surfaces are the first glass targets.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GlassTuningSection(
    glassSpec: AeGlassSpec,
    onGlassSpecChange: (AeGlassSpec) -> Unit,
) {
    LabSection("Glass tuning · meaningful controls only") {
        GlassSlider(
            title = "Glass clarity",
            value = glassSpec.surfaceOpacity,
            valueRange = AeGlassSpec.MinSurfaceOpacity..AeGlassSpec.MaxSurfaceOpacity,
            onValueChange = { onGlassSpecChange(glassSpec.copy(surfaceOpacity = it)) },
        )
        GlassSlider(
            title = "Music tint",
            value = glassSpec.tintStrength,
            valueRange = 0f..AeGlassSpec.MaxTintStrength,
            onValueChange = { onGlassSpecChange(glassSpec.copy(tintStrength = it)) },
        )
        Text(
            "Edge highlight and elevation are renderer details. Enhanced refraction remains a separate device-profiled experiment.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ColorRolesSection() {
    LabSection("Color roles") {
        ColorSwatch("Primary", MaterialTheme.colorScheme.primary)
        ColorSwatch("Primary container", MaterialTheme.colorScheme.primaryContainer)
        ColorSwatch("Surface", MaterialTheme.colorScheme.surface)
        ColorSwatch("Surface variant", MaterialTheme.colorScheme.surfaceVariant)
        ColorSwatch("Error", MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun TypographySection() {
    LabSection("Typography") {
        Text("Display · AeMusic", style = MaterialTheme.typography.displayLarge)
        Text("Headline · 正在播放", style = MaterialTheme.typography.headlineLarge)
        Text("Title · MAZE", style = MaterialTheme.typography.titleLarge)
        Text("Body · 女团浴室打歌中心", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Metadata · i-dle · 无损音质",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Label · 播放全部", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ShapeScaleSection() {
    LabSection("Shape scale") {
        Row(horizontalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
            ShapeSample("S", MaterialTheme.shapes.small)
            ShapeSample("M", MaterialTheme.shapes.medium)
            ShapeSample("L", MaterialTheme.shapes.large)
            ShapeSample("XL", MaterialTheme.shapes.extraLarge)
        }
    }
}

@Composable
private fun SpacingRhythmSection() {
    LabSection("Spacing rhythm") {
        listOf(
            "4" to AeSpacing.xxs,
            "8" to AeSpacing.xs,
            "12" to AeSpacing.sm,
            "16" to AeSpacing.md,
            "24" to AeSpacing.lg,
            "32" to AeSpacing.xl,
            "48" to AeSpacing.xxl,
        ).forEach { (label, width) ->
            Row(horizontalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
                Text(label, modifier = Modifier.size(width = 32.dp, height = 24.dp))
                Box(
                    modifier = Modifier
                        .size(width = width, height = 16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun AtmosphereDemo() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.secondary,
                    ),
                ),
            )
            .padding(AeSpacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
            AeSurface(role = AeSurfaceRole.Raised, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(AeSpacing.md)) {
                    Text("Raised surface", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Queue sheet / elevated content",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AeSurface(role = AeSurfaceRole.Floating, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(AeSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(AeSpacing.sm),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Column {
                        Text("MAZE", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "i-dle · Lossless",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.xxs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                "${(value * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
        )
    }
}

@Composable
private fun ColorSwatch(label: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(color),
        )
        Column {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                "#${color.toArgb().toUInt().toString(16).padStart(8, '0').uppercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ShapeSample(label: String, shape: androidx.compose.ui.graphics.Shape) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun CoreComponentsSection() {
    LabSection("Core components") {
        AeSectionHeader(title = "Recently played", actionLabel = "See all", onAction = {})
        AeTrackRow(
            title = "MAZE",
            metadata = "i-dle · Lossless",
            isPlaying = true,
            trailingContent = { Text("Ⅱ", color = MaterialTheme.colorScheme.primary) },
        )
        AeTrackRow(
            title = "A very long song title that demonstrates two-line truncation on a compact phone",
            metadata = "Artist name · Album with missing artwork",
            trailingContent = { Text("⋮") },
        )
        AeStatePane(
            state = AeStatePaneState.Empty(
                title = "Nothing here yet",
                message = "Your music will appear here.",
            ),
            actionLabel = "Browse",
            onAction = {},
        )
    }
}

@Composable
private fun LabSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        content()
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(AeSpacing.xs),
        content = { content() },
    )
}
