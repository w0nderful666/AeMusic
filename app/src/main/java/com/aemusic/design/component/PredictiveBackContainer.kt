package com.aemusic.design.component

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.aemusic.design.theme.AeMotion
import kotlinx.coroutines.CancellationException

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

@Composable
fun PredictiveBackContainer(
    visible: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var backProgress by remember { mutableFloatStateOf(0f) }

    PredictiveBackHandler(enabled = visible) { events ->
        try {
            events.collect { backEvent ->
                backProgress = backEvent.progress.coerceIn(0f, 1f)
            }
            onBack()
            backProgress = 0f
        } catch (_: CancellationException) {
            backProgress = 0f
        }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInHorizontally(
            initialOffsetX = { it / 4 },
            animationSpec = tween(AeMotion.StandardMillis),
        ) + fadeIn(animationSpec = tween(AeMotion.StandardMillis)),
        exit = slideOutHorizontally(
            targetOffsetX = { it / 4 },
            animationSpec = tween(AeMotion.StandardMillis),
        ) + fadeOut(animationSpec = tween(AeMotion.StandardMillis)),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = size.width * 0.08f * backProgress
                    scaleX = 1f - 0.04f * backProgress
                    scaleY = scaleX
                    alpha = 1f - 0.15f * backProgress
                    transformOrigin = TransformOrigin(0f, 0.5f)
                },
            color = MaterialTheme.colorScheme.background,
        ) {
            content()
        }
    }
}
