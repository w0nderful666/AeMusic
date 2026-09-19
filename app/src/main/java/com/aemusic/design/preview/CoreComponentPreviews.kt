package com.aemusic.design.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.aemusic.design.component.AeSectionHeader
import com.aemusic.design.component.AeStatePane
import com.aemusic.design.component.AeStatePaneState
import com.aemusic.design.component.AeSurface
import com.aemusic.design.component.AeSurfaceRole
import com.aemusic.design.component.AeTrackRow
import com.aemusic.design.theme.AeColorMode
import com.aemusic.design.theme.AeTheme

@AeThemePreview
@Composable
private fun CoreComponentsPreview() {
    AeTheme {
        AeSurface(role = AeSurfaceRole.Canvas) {
            Column {
                AeSectionHeader("Recently played", actionLabel = "See all", onAction = {})
                AeTrackRow("MAZE", "i-dle · Lossless", isPlaying = true)
                AeTrackRow(
                    "A deliberately long title that demonstrates compact two-line handling",
                    "Missing artwork · Long metadata",
                )
                AeStatePane(
                    state = AeStatePaneState.Error("Could not load music", "Try again in a moment."),
                    actionLabel = "Retry",
                    onAction = {},
                )
            }
        }
    }
}

@Preview(name = "OLED", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun OledComponentsPreview() {
    AeTheme(colorMode = AeColorMode.OLED) {
        AeSurface(role = AeSurfaceRole.Canvas) {
            Column {
                Text("True black canvas")
                AeTrackRow("OLED preview", "Readable metadata")
            }
        }
    }
}

@Preview(name = "Large font", fontScale = 1.5f, widthDp = 320, showBackground = true)
@Composable
private fun LargeFontTrackRowPreview() {
    AeTheme(colorMode = AeColorMode.LIGHT) {
        AeSurface(role = AeSurfaceRole.Canvas) {
            AeTrackRow(
                "A long track title at a large accessibility font scale",
                "Artist · Album",
            )
        }
    }
}
