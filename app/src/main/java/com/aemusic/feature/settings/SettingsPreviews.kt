package com.aemusic.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.aemusic.design.preview.AeThemePreview
import com.aemusic.design.theme.AeTheme
import com.aemusic.design.theme.AeVisualStyle

@AeThemePreview
@Composable
private fun PixelGroupedSettingsPreview() {
    AeTheme(visualStyle = AeVisualStyle.GLASS) {
        SettingsScreen(
            padding = PaddingValues(0.dp),
            state = SettingsUiState(
                fourTabs = true,
                language = AppLanguage.System,
                immersiveTitles = true,
                artworkColors = true,
                artworkBackground = true,
                colorStrength = 0.82f,
                glassEnabled = true,
                glassClarity = 0.82f,
            ),
            onAction = {},
        )
    }
}
