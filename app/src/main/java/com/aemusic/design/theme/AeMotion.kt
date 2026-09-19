package com.aemusic.design.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Motion timing tokens only.  We intentionally do not invent a custom animation framework.
 */
object AeMotion {
    const val FastMillis = 150
    const val StandardMillis = 250
    const val EmphasizedMillis = 400
    const val ContainerTransformMillis = 420
    const val ColorTransitionMillis = 450

    fun <T> spatialSpring(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    fun <T> expressiveSpring(): SpringSpec<T> = spring(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessMedium,
    )
}
