package com.aemusic.design.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Small in-project outline set with rounded Pixel/Material proportions. */
object AeIcons {
    val Home = outline("Home") {
        moveTo(3f, 11f); lineTo(12f, 3.5f); lineTo(21f, 11f)
        moveTo(5.5f, 9.5f); lineTo(5.5f, 20f); lineTo(18.5f, 20f); lineTo(18.5f, 9.5f)
        moveTo(9.5f, 20f); lineTo(9.5f, 14f); lineTo(14.5f, 14f); lineTo(14.5f, 20f)
    }
    val Search = outline("Search") {
        moveTo(10.5f, 4f); curveTo(6.9f, 4f, 4f, 6.9f, 4f, 10.5f)
        curveTo(4f, 14.1f, 6.9f, 17f, 10.5f, 17f)
        curveTo(14.1f, 17f, 17f, 14.1f, 17f, 10.5f)
        curveTo(17f, 6.9f, 14.1f, 4f, 10.5f, 4f)
        moveTo(15.4f, 15.4f); lineTo(21f, 21f)
    }
    val Library = outline("Library") {
        moveTo(4f, 5f); lineTo(4f, 20f)
        moveTo(9f, 5f); lineTo(9f, 20f)
        moveTo(14f, 5f); lineTo(14f, 20f)
        moveTo(17f, 5.5f); lineTo(21f, 19.5f)
    }
    val Settings = outline("Settings") {
        moveTo(4f, 7f); lineTo(8f, 7f); moveTo(12f, 7f); lineTo(20f, 7f)
        moveTo(4f, 17f); lineTo(12f, 17f); moveTo(16f, 17f); lineTo(20f, 17f)
        moveTo(10f, 4.5f); lineTo(10f, 9.5f); moveTo(14f, 14.5f); lineTo(14f, 19.5f)
    }
    val Play = filled("Play") { moveTo(8f, 5f); lineTo(19f, 12f); lineTo(8f, 19f); close() }
    val Pause = filled("Pause") {
        moveTo(7f, 5f); lineTo(10.5f, 5f); lineTo(10.5f, 19f); lineTo(7f, 19f); close()
        moveTo(13.5f, 5f); lineTo(17f, 5f); lineTo(17f, 19f); lineTo(13.5f, 19f); close()
    }
    val Previous = filled("Previous") {
        moveTo(5f, 5f); lineTo(8f, 5f); lineTo(8f, 19f); lineTo(5f, 19f); close()
        moveTo(19f, 5f); lineTo(9f, 12f); lineTo(19f, 19f); close()
    }
    val Next = filled("Next") {
        moveTo(16f, 5f); lineTo(19f, 5f); lineTo(19f, 19f); lineTo(16f, 19f); close()
        moveTo(5f, 5f); lineTo(15f, 12f); lineTo(5f, 19f); close()
    }
    val Shuffle = outline("Shuffle") {
        moveTo(4f, 7f); lineTo(7f, 7f); curveTo(12f, 7f, 12f, 17f, 17f, 17f); lineTo(20f, 17f)
        moveTo(17f, 14f); lineTo(20f, 17f); lineTo(17f, 20f)
        moveTo(4f, 17f); lineTo(7f, 17f); curveTo(9f, 17f, 10f, 15f, 11f, 13f)
        moveTo(13f, 9f); curveTo(14f, 7f, 15f, 7f, 17f, 7f); lineTo(20f, 7f)
        moveTo(17f, 4f); lineTo(20f, 7f); lineTo(17f, 10f)
    }
    val Repeat = outline("Repeat") {
        moveTo(17f, 3f); lineTo(20f, 6f); lineTo(17f, 9f)
        moveTo(20f, 6f); lineTo(7f, 6f); curveTo(4.8f, 6f, 3f, 7.8f, 3f, 10f)
        moveTo(7f, 21f); lineTo(4f, 18f); lineTo(7f, 15f)
        moveTo(4f, 18f); lineTo(17f, 18f); curveTo(19.2f, 18f, 21f, 16.2f, 21f, 14f)
    }
    val Favorite = outline("Favorite") {
        moveTo(12f, 20f); curveTo(10f, 18f, 4f, 14f, 4f, 9f)
        curveTo(4f, 5f, 9f, 3f, 12f, 7f); curveTo(15f, 3f, 20f, 5f, 20f, 9f)
        curveTo(20f, 14f, 14f, 18f, 12f, 20f)
    }
    val Queue = outline("Queue") {
        moveTo(4f, 6f); lineTo(20f, 6f); moveTo(4f, 12f); lineTo(15f, 12f)
        moveTo(4f, 18f); lineTo(15f, 18f); moveTo(18f, 14f); lineTo(22f, 17f); lineTo(18f, 20f); close()
    }
    val Lyrics = outline("Lyrics") {
        moveTo(5f, 4f); lineTo(19f, 4f); lineTo(19f, 16f); lineTo(12f, 16f); lineTo(7f, 20f); lineTo(7f, 16f); lineTo(5f, 16f); close()
        moveTo(9f, 8f); lineTo(15f, 8f); moveTo(9f, 12f); lineTo(14f, 12f)
    }
    val Down = outline("Down") { moveTo(5f, 9f); lineTo(12f, 16f); lineTo(19f, 9f) }
    val ChevronRight = outline("Chevron right") { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }
    val Check = outline("Check") { moveTo(5f, 12f); lineTo(10f, 17f); lineTo(19f, 7f) }
    val Add = outline("Add") { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) }
    val Delete = outline("Delete") {
        moveTo(5f, 7f); lineTo(19f, 7f); moveTo(9f, 7f); lineTo(9f, 4f); lineTo(15f, 4f); lineTo(15f, 7f)
        moveTo(7f, 7f); lineTo(8f, 20f); lineTo(16f, 20f); lineTo(17f, 7f)
    }
    val Music = outline("Music") {
        moveTo(9f, 18f); curveTo(9f, 20f, 6f, 21f, 4.5f, 19.5f); curveTo(3f, 18f, 4.5f, 15.5f, 7f, 15.5f); lineTo(9f, 15.5f)
        moveTo(9f, 18f); lineTo(9f, 6f); lineTo(19f, 4f); lineTo(19f, 15f)
        moveTo(19f, 15f); curveTo(19f, 17f, 16f, 18f, 14.5f, 16.5f); curveTo(13f, 15f, 14.5f, 12.5f, 17f, 12.5f); lineTo(19f, 12.5f)
    }
    val More = filled("More") {
        listOf(5f, 12f, 19f).forEach { x ->
            moveTo(x, 10.5f); curveTo(x - .8f, 10.5f, x - 1.5f, 11.2f, x - 1.5f, 12f)
            curveTo(x - 1.5f, 12.8f, x - .8f, 13.5f, x, 13.5f)
            curveTo(x + .8f, 13.5f, x + 1.5f, 12.8f, x + 1.5f, 12f)
            curveTo(x + 1.5f, 11.2f, x + .8f, 10.5f, x, 10.5f); close()
        }
    }
    val Back = outline("Back") {
        moveTo(20f, 12f); lineTo(4f, 12f)
        moveTo(11f, 19f); lineTo(4f, 12f); lineTo(11f, 5f)
    }
    val Warning = outline("Warning") {
        moveTo(12f, 4f); lineTo(21f, 19f); lineTo(3f, 19f); close()
        moveTo(12f, 9f); lineTo(12f, 13f)
        moveTo(12f, 16f); lineTo(12.01f, 16f)
    }
    val Account = outline("Account") {
        moveTo(12f, 12f); curveTo(14.21f, 12f, 16f, 10.21f, 16f, 8f); curveTo(16f, 5.79f, 14.21f, 4f, 12f, 4f); curveTo(9.79f, 4f, 8f, 5.79f, 8f, 8f); curveTo(8f, 10.21f, 9.79f, 12f, 12f, 12f)
        moveTo(6f, 20f); curveTo(6f, 16.69f, 8.69f, 14f, 12f, 14f); curveTo(15.31f, 14f, 18f, 16.69f, 18f, 20f)
    }
    val Palette = outline("Palette") {
        moveTo(12f, 3f); curveTo(6.5f, 3f, 2f, 7.5f, 2f, 13f); curveTo(2f, 17f, 5.5f, 20.5f, 9.5f, 20.5f); curveTo(10.5f, 20.5f, 11f, 19.8f, 11f, 19f); curveTo(11f, 18.2f, 10.5f, 17.5f, 10.5f, 16.5f); curveTo(10.5f, 15f, 12f, 13.5f, 13.5f, 13.5f); lineTo(15f, 13.5f); curveTo(18.5f, 13.5f, 22f, 10.5f, 22f, 6.5f); curveTo(22f, 4.5f, 17.5f, 3f, 12f, 3f)
    }
    val Cloud = outline("Cloud") {
        moveTo(19.35f, 10.04f); curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f); curveTo(9.11f, 4f, 6.6f, 5.64f, 5.35f, 8.04f); curveTo(2.34f, 8.36f, 0f, 10.91f, 0f, 14f); curveTo(0f, 17.31f, 2.69f, 20f, 6f, 20f); lineTo(19f, 20f); curveTo(21.76f, 20f, 24f, 17.76f, 24f, 15f); curveTo(24f, 12.36f, 21.95f, 10.22f, 19.35f, 10.04f)
    }
    val Storage = outline("Storage") {
        moveTo(4f, 6f); curveTo(4f, 4.5f, 7.6f, 3f, 12f, 3f); curveTo(16.4f, 3f, 20f, 4.5f, 20f, 6f); curveTo(20f, 7.5f, 16.4f, 9f, 12f, 9f); curveTo(7.6f, 9f, 4f, 7.5f, 4f, 6f)
        moveTo(4f, 6f); lineTo(4f, 18f); curveTo(4f, 19.5f, 7.6f, 21f, 12f, 21f); curveTo(16.4f, 21f, 20f, 19.5f, 20f, 18f); lineTo(20f, 6f)
        moveTo(4f, 12f); curveTo(4f, 13.5f, 7.6f, 15f, 12f, 15f); curveTo(16.4f, 15f, 20f, 13.5f, 20f, 12f)
    }
}

private fun outline(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        )
    }.build()

private fun filled(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black), pathBuilder = block)
    }.build()
