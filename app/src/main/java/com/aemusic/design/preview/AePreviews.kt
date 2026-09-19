package com.aemusic.design.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Renders the same Composable in system Light and Dark configurations.
 */
@Preview(
    name = "Light",
    group = "Theme",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Dark",
    group = "Theme",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class AeThemePreview
