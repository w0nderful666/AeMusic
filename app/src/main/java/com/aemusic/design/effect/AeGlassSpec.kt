package com.aemusic.design.effect

/**
 * Visual specifications for AeMusic Liquid Glass (inspired by Haze 2.0 and iOS Material Hierarchy).
 *
 * Controls blur radius, clarity/opacity, tint, water-film sheen, and specular refraction highlights.
 */
data class AeGlassSpec(
    val enabled: Boolean = true,
    val blurRadiusDp: Float = 24f,
    val surfaceOpacity: Float = 0.78f,
    val tintStrength: Float = 0.12f,
    val waterFilmStrength: Float = 0.55f,
    val refractionStrength: Float = 0.65f,
    val borderWidthDp: Float = 1.2f,
    val shadowElevationDp: Float = 6f,
) {
    fun normalized(): AeGlassSpec = copy(
        blurRadiusDp = blurRadiusDp.coerceIn(MinBlurRadius, MaxBlurRadius),
        surfaceOpacity = surfaceOpacity.coerceIn(MinSurfaceOpacity, MaxSurfaceOpacity),
        tintStrength = tintStrength.coerceIn(0f, MaxTintStrength),
        waterFilmStrength = waterFilmStrength.coerceIn(0f, 1f),
        refractionStrength = refractionStrength.coerceIn(0f, 1f),
        borderWidthDp = borderWidthDp.coerceIn(MinBorderWidth, MaxBorderWidth),
        shadowElevationDp = shadowElevationDp.coerceIn(0f, MaxShadowElevation),
    )

    companion object {
        const val MinBlurRadius = 8f
        const val MaxBlurRadius = 48f
        const val MinSurfaceOpacity = 0.50f
        const val MaxSurfaceOpacity = 0.95f
        const val MaxTintStrength = 0.50f
        const val MinBorderWidth = 0.5f
        const val MaxBorderWidth = 3.0f
        const val MaxShadowElevation = 16f
        val Default = AeGlassSpec()
    }
}
