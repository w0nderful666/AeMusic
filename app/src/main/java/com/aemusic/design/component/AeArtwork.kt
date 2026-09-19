package com.aemusic.design.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.aemusic.design.icon.AeIcons

/** Square local/fake artwork boundary. Network loading is intentionally outside this component. */
@Composable
fun AeArtwork(
    painter: Painter?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val artworkModifier = modifier
        .aspectRatio(1f)
        .clip(MaterialTheme.shapes.medium)

    if (painter != null) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = artworkModifier,
        )
    } else {
        Box(
            modifier = artworkModifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ),
                )
                .semantics {
                    if (contentDescription != null) this.contentDescription = contentDescription
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AeIcons.Music,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.38f),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
