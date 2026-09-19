package com.aemusic.design.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.aemusic.design.component.AeSurface
import com.aemusic.design.component.AeSurfaceRole
import com.aemusic.design.theme.AeSpacing
import com.aemusic.design.theme.AeTheme
import com.aemusic.design.theme.AeVisualStyle

@AeThemePreview
@Composable
private fun MaterialFoundationPreview() {
    AeTheme(visualStyle = AeVisualStyle.MATERIAL) {
        FoundationSample(title = "Material")
    }
}

@AeThemePreview
@Composable
private fun GlassFoundationPreview() {
    AeTheme(visualStyle = AeVisualStyle.GLASS) {
        FoundationSample(title = "Glass")
    }
}

@Composable
private fun FoundationSample(title: String) {
    AeSurface(
        role = AeSurfaceRole.Canvas,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(AeSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AeSpacing.md),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(
                "Pixel-native behavior, music-first hierarchy, optional Glass material.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                            ),
                        ),
                    )
                    .padding(AeSpacing.md),
            ) {
                AeSurface(
                    role = AeSurfaceRole.Floating,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(AeSpacing.md),
                        horizontalArrangement = Arrangement.spacedBy(AeSpacing.sm),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Column {
                            Text("MAZE", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "i-dle",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
