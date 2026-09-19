package com.aemusic.design.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp

@Composable
fun AeAlphabetFastScroller(
    sections: List<Char>,
    expanded: Boolean,
    onSection: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (sections.isEmpty()) return
    var height by remember { mutableFloatStateOf(1f) }
    var active by remember { mutableStateOf<Char?>(null) }
    val alpha by animateFloatAsState(if (expanded || active != null) 1f else 0.18f, label = "alphabet-index-alpha")
    Box(modifier, contentAlignment = Alignment.CenterEnd) {
        Column(
            Modifier
                .width(28.dp)
                .fillMaxHeight(.72f)
                .alpha(alpha)
                .onSizeChanged { height = it.height.toFloat().coerceAtLeast(1f) }
                .pointerInput(sections) {
                    fun select(y: Float) {
                        val index = ((y / height) * sections.size).toInt().coerceIn(sections.indices)
                        active = sections[index]
                        onSection(sections[index])
                    }
                    detectVerticalDragGestures(
                        onDragStart = { select(it.y) },
                        onDragEnd = { active = null },
                        onDragCancel = { active = null },
                        onVerticalDrag = { change, _ -> select(change.position.y) },
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            sections.forEach { Text(it.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
        }
        active?.let {
            Box(
                Modifier.padding(end = 42.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape).padding(horizontal = 18.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text(it.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer) }
        }
    }
}
