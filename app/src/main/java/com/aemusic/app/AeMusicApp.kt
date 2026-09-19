package com.aemusic.app

import androidx.compose.runtime.Composable
import com.aemusic.design.preview.AeThemePreview
import com.aemusic.design.theme.AeTheme
import com.aemusic.design.theme.AeVisualStyle
import com.aemusic.feature.shell.AeMusicShell

@Composable
fun AeMusicApp(container: AppContainer) {
    AeTheme(visualStyle = AeVisualStyle.GLASS) { AeMusicShell(container) }
}

@AeThemePreview
@Composable
private fun AeMusicAppPreview() {
    AeMusicApp(AppContainer(androidx.compose.ui.platform.LocalContext.current))
}
