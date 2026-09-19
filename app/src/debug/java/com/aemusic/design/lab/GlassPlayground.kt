package com.aemusic.design.lab

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import com.aemusic.design.component.AeSurface
import com.aemusic.design.component.AeSurfaceRole
import com.aemusic.design.theme.AeSpacing

private enum class GlassRenderer { Basic, Enhanced }

/** Debug-only experiment. No renderer or state in this file is part of the production UI API. */
@Composable
internal fun GlassPlayground() {
    var renderer by remember { mutableStateOf(GlassRenderer.Enhanced) }
    Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs)) {
            GlassRenderer.entries.forEach { option ->
                FilterChip(
                    selected = renderer == option,
                    onClick = { renderer = option },
                    label = { Text(option.name) },
                )
            }
        }
        Text(
            if (renderer == GlassRenderer.Enhanced) {
                "Enhanced samples the same scrolling backdrop, softens detail and adds a spring lens."
            } else {
                "Basic is the inexpensive translucent fallback."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GlassStage(renderer)
    }
}

@Composable
private fun GlassStage(renderer: GlassRenderer) {
    val scrollState = rememberScrollState()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(StageHeight)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        val dockTop = maxHeight - DockHeight + StagePadding
        GlassBackdrop(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        )

        AeSurface(
            role = AeSurfaceRole.Floating,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(StagePadding),
        ) {
            Text(
                "Scroll behind the glass",
                modifier = Modifier.padding(horizontal = AeSpacing.md, vertical = AeSpacing.sm),
                style = MaterialTheme.typography.titleSmall,
            )
        }

        ExperimentalDock(
            renderer = renderer,
            backdropTop = dockTop,
            scrollState = scrollState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(DockHeight)
                .padding(horizontal = StagePadding, vertical = StagePadding),
        )
    }
}

@Composable
private fun GlassBackdrop(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AeSpacing.sm),
    ) {
        repeat(9) { index ->
            val colors = if (index % 3 == 0) {
                listOf(Color(0xFF7357FF), Color(0xFFE85F9C), Color(0xFFFFB45B))
            } else if (index % 3 == 1) {
                listOf(Color(0xFF00A6A6), Color(0xFF55C271), Color(0xFFD6D45D))
            } else {
                listOf(Color(0xFF2867C7), Color(0xFF7B4CC9), Color(0xFFE1537D))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
                    .background(Brush.linearGradient(colors))
                    .padding(AeSpacing.md),
            ) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text(
                        "Artwork study ${index + 1}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Moving color, detail and readable background text",
                        color = Color.White.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExperimentalDock(
    renderer: GlassRenderer,
    backdropTop: Dp,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.extraLarge
    Surface(
        modifier = modifier,
        shape = shape,
        color = if (renderer == GlassRenderer.Basic) {
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f)
        } else {
            Color.Transparent
        },
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
        shadowElevation = if (renderer == GlassRenderer.Basic) 0.dp else 6.dp,
    ) {
        Box(Modifier.fillMaxSize()) {
            if (renderer == GlassRenderer.Enhanced) {
                GlassBackdrop(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1_100.dp)
                        .graphicsLayer {
                            translationY = -backdropTop.toPx() - scrollState.value
                        }
                        .blur(18.dp),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f)),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.38f)),
                )
            }
            DockContent(enhanced = renderer == GlassRenderer.Enhanced)
        }
    }
}

@Composable
private fun DockContent(enhanced: Boolean) {
    var selected by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AeSpacing.sm, vertical = AeSpacing.xs),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF6947D6), Color(0xFFE2508B))),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("♪", color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(AeSpacing.sm))
            Text(
                "MAZE",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("Ⅱ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(AeSpacing.md))
            Text("›", style = MaterialTheme.typography.headlineSmall)
        }
        LiquidNavigation(selected, enhanced, onSelected = { selected = it })
    }
}

@Composable
private fun LiquidNavigation(
    selected: Int,
    enhanced: Boolean,
    onSelected: (Int) -> Unit,
) {
    val items = listOf("Home" to "⌂", "Library" to "♫", "Search" to "⌕", "You" to "●")
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        val itemWidth = maxWidth / items.size
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selected,
            animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
            label = "glass-dock-selection",
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                .width(itemWidth)
                .height(48.dp)
                .padding(horizontal = if (enhanced) 3.dp else 7.dp, vertical = 2.dp)
                .clip(CircleShape)
                .background(
                    if (enhanced) {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                                Color.White.copy(alpha = 0.18f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.24f),
                            ),
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        )
                    },
                ),
        )
        Row(Modifier.fillMaxSize()) {
            items.forEachIndexed { index, (label, icon) ->
                Column(
                    modifier = Modifier
                        .width(itemWidth)
                        .fillMaxSize()
                        .clip(CircleShape)
                        .clickable { onSelected(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        icon,
                        color = if (selected == index) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        label,
                        color = if (selected == index) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

private val StageHeight = 440.dp
private val DockHeight = 132.dp
private val StagePadding = 12.dp
