package com.aemusic.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aemusic.design.component.AeSurface
import com.aemusic.design.component.AeSurfaceRole
import com.aemusic.design.effect.AeGlassSpec
import com.aemusic.design.icon.AeIcons
import com.aemusic.design.theme.AeSpacing
import com.aemusic.design.theme.LocalAeCanvasPalette
import com.aemusic.design.theme.LocalAeGlassSpec

@Composable
fun GlassSettingsScreen(
    padding: PaddingValues,
    preferences: AppUiPreferences,
    onBack: () -> Unit,
    onGlassUpdated: (AeGlassSpec) -> Unit,
    onArtworkBackgroundUpdated: (Boolean) -> Unit,
    onColorStrengthUpdated: (Float) -> Unit,
) {
    var glassEnabled by remember { mutableStateOf(preferences.glassEnabled) }
    var blurRadius by remember { mutableFloatStateOf(preferences.glassBlurRadius) }
    var clarity by remember { mutableFloatStateOf(preferences.glassClarity) }
    var tintStrength by remember { mutableFloatStateOf(preferences.glassTintStrength) }
    var waterFilm by remember { mutableFloatStateOf(preferences.glassWaterFilm) }
    var refraction by remember { mutableFloatStateOf(preferences.glassRefractionStrength) }
    var borderWidth by remember { mutableFloatStateOf(preferences.glassBorderWidth) }
    var shadowElevation by remember { mutableFloatStateOf(preferences.glassShadowElevation) }

    var artworkBackground by remember { mutableStateOf(preferences.artworkBackground) }
    var colorStrength by remember { mutableFloatStateOf(preferences.colorStrength) }

    fun notifySpecChanged() {
        val spec = AeGlassSpec(
            enabled = glassEnabled,
            blurRadiusDp = blurRadius,
            surfaceOpacity = clarity,
            tintStrength = tintStrength,
            waterFilmStrength = waterFilm,
            refractionStrength = refraction,
            borderWidthDp = borderWidth,
            shadowElevationDp = shadowElevation,
        ).normalized()
        onGlassUpdated(spec)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = AeSpacing.md,
                top = padding.calculateTopPadding() + 16.dp,
                end = AeSpacing.md,
                bottom = padding.calculateBottomPadding() + AeSpacing.lg,
            ),
            verticalArrangement = Arrangement.spacedBy(AeSpacing.lg),
        ) {
            item {
                Row(
                    modifier = Modifier.statusBarsPadding().fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(AeIcons.Down, localized("Back", "返回"))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            localized("Liquid Glass & Material", "液态玻璃与视觉定制"),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            localized(
                                "Tune blur, refraction, water-film, and material hierarchy",
                                "定制液态玻璃模糊、折射反光、水膜流动感与材质层级",
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            // 1. 实时预览区
            item {
                Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.xs)) {
                    Text(
                        localized("Realtime Preview", "材质实时预览"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    val previewPalette = LocalAeCanvasPalette.current
                    val previewBgStops = if (previewPalette.backgroundStops.size >= 3) {
                        listOf(
                            previewPalette.backgroundStops[0],
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                            previewPalette.backgroundStops[1],
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(Brush.horizontalGradient(previewBgStops), shape = MaterialTheme.shapes.extraLarge)
                            .padding(AeSpacing.md),
                        contentAlignment = Alignment.Center,
                    ) {
                        CompositionLocalProvider(
                            LocalAeGlassSpec provides AeGlassSpec(
                                enabled = glassEnabled,
                                blurRadiusDp = blurRadius,
                                surfaceOpacity = clarity,
                                tintStrength = tintStrength,
                                waterFilmStrength = waterFilm,
                                refractionStrength = refraction,
                            ).normalized(),
                        ) {
                            AeSurface(
                                role = AeSurfaceRole.Floating,
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .height(84.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = AeSpacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(AeSpacing.sm),
                                ) {
                                    Icon(
                                        AeIcons.Music,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            if (glassEnabled) localized("Liquid Glass Active", "液态玻璃生效中") else localized("Material Flat Surface", "Material 纯色表面"),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            localized("Edge refraction & fluid sheen preview", "边缘折射与水膜微光实时呈现"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. 全局开关
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column(Modifier.padding(AeSpacing.md), verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    localized("Liquid Glass effect", "启用液态玻璃效果"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    localized(
                                        "Applies liquid glass to floating docks, sheets, and popups. Low-end devices can turn this off for pure Material performance.",
                                        "为底部控制坞、歌词及队列抽屉提供晶莹折射质感；低配或省电可随时关闭，秒切纯净 Material。",
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = glassEnabled,
                                onCheckedChange = {
                                    glassEnabled = it
                                    preferences.glassEnabled = it
                                    notifySpecChanged()
                                },
                            )
                        }
                    }
                }
            }

            // 3. 材质参数微调
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column(Modifier.padding(AeSpacing.md), verticalArrangement = Arrangement.spacedBy(AeSpacing.md)) {
                        Text(
                            localized("Material Presets", "液态玻璃风格预设"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs),
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    clarity = 0.78f
                                    blurRadius = 24f
                                    waterFilm = 0.65f
                                    refraction = 0.70f
                                    tintStrength = 0.12f
                                    borderWidth = 1.2f
                                    shadowElevation = 6f
                                    preferences.glassClarity = clarity
                                    preferences.glassBlurRadius = blurRadius
                                    preferences.glassWaterFilm = waterFilm
                                    preferences.glassRefractionStrength = refraction
                                    preferences.glassTintStrength = tintStrength
                                    preferences.glassBorderWidth = borderWidth
                                    preferences.glassShadowElevation = shadowElevation
                                    notifySpecChanged()
                                },
                                label = { Text(localized("Fluid Sheen (Default)", "水膜流光 (推荐)")) },
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    clarity = 0.58f
                                    blurRadius = 16f
                                    waterFilm = 0.30f
                                    refraction = 0.85f
                                    tintStrength = 0.05f
                                    borderWidth = 1.6f
                                    shadowElevation = 4f
                                    preferences.glassClarity = clarity
                                    preferences.glassBlurRadius = blurRadius
                                    preferences.glassWaterFilm = waterFilm
                                    preferences.glassRefractionStrength = refraction
                                    preferences.glassTintStrength = tintStrength
                                    preferences.glassBorderWidth = borderWidth
                                    preferences.glassShadowElevation = shadowElevation
                                    notifySpecChanged()
                                },
                                label = { Text(localized("Ice Crystal", "冰晶高透")) },
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    clarity = 0.86f
                                    blurRadius = 36f
                                    waterFilm = 0.20f
                                    refraction = 0.35f
                                    tintStrength = 0.10f
                                    borderWidth = 0.8f
                                    shadowElevation = 8f
                                    preferences.glassClarity = clarity
                                    preferences.glassBlurRadius = blurRadius
                                    preferences.glassWaterFilm = waterFilm
                                    preferences.glassRefractionStrength = refraction
                                    preferences.glassTintStrength = tintStrength
                                    preferences.glassBorderWidth = borderWidth
                                    preferences.glassShadowElevation = shadowElevation
                                    notifySpecChanged()
                                },
                                label = { Text(localized("iOS Frosted", "苹果毛玻璃")) },
                            )
                        }

                        Text(
                            localized("Material Tuning", "玻璃材质参数微调"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        // 模糊半径
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(localized("Blur Radius", "模糊半径"), fontWeight = FontWeight.Medium)
                                Text("${blurRadius.toInt()} dp", color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = blurRadius,
                                onValueChange = {
                                    blurRadius = it
                                    preferences.glassBlurRadius = it
                                    notifySpecChanged()
                                },
                                valueRange = 8f..48f,
                                enabled = glassEnabled,
                            )
                        }

                        // 表面清晰度 / 透明度
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(localized("Surface Clarity (Opacity)", "表面透明度与清晰度"), fontWeight = FontWeight.Medium)
                                Text("${(clarity * 100).toInt()}%", color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = clarity,
                                onValueChange = {
                                    clarity = it
                                    preferences.glassClarity = it
                                    notifySpecChanged()
                                },
                                valueRange = 0.50f..0.95f,
                                enabled = glassEnabled,
                            )
                        }

                        // 水膜流动感
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(localized("Water-film Sheen", "水膜流动感 / 漫射微光"), fontWeight = FontWeight.Medium)
                                Text("${(waterFilm * 100).toInt()}%", color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = waterFilm,
                                onValueChange = {
                                    waterFilm = it
                                    preferences.glassWaterFilm = it
                                    notifySpecChanged()
                                },
                                valueRange = 0f..1f,
                                enabled = glassEnabled,
                            )
                        }

                        // 边框反光与折射高光
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(localized("Border Refraction & Highlight", "边框折射反光与高光强度"), fontWeight = FontWeight.Medium)
                                Text("${(refraction * 100).toInt()}%", color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = refraction,
                                onValueChange = {
                                    refraction = it
                                    preferences.glassRefractionStrength = it
                                    notifySpecChanged()
                                },
                                valueRange = 0f..1f,
                                enabled = glassEnabled,
                            )
                        }

                        // 边框微光宽度
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(localized("Border Refraction Width", "边框微光宽度"), fontWeight = FontWeight.Medium)
                                Text("${String.format(java.util.Locale.US, "%.1f", borderWidth)} dp", color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = borderWidth,
                                onValueChange = {
                                    borderWidth = it
                                    preferences.glassBorderWidth = it
                                    notifySpecChanged()
                                },
                                valueRange = 0.5f..3.0f,
                                enabled = glassEnabled,
                            )
                        }

                        // 柔和景深阴影高度
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(localized("Ambient Shadow Elevation", "柔和环境景深阴影"), fontWeight = FontWeight.Medium)
                                Text("${shadowElevation.toInt()} dp", color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = shadowElevation,
                                onValueChange = {
                                    shadowElevation = it
                                    preferences.glassShadowElevation = it
                                    notifySpecChanged()
                                },
                                valueRange = 0f..16f,
                                enabled = glassEnabled,
                            )
                        }

                        // 色调浓度
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(localized("Tint Strength", "色调融合浓度"), fontWeight = FontWeight.Medium)
                                Text("${(tintStrength * 100).toInt()}%", color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = tintStrength,
                                onValueChange = {
                                    tintStrength = it
                                    preferences.glassTintStrength = it
                                    notifySpecChanged()
                                },
                                valueRange = 0f..0.50f,
                                enabled = glassEnabled,
                            )
                        }
                    }
                }
            }

            // 4. 水膜色彩底板与背景
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column(Modifier.padding(AeSpacing.md), verticalArrangement = Arrangement.spacedBy(AeSpacing.md)) {
                        Text(
                            localized("Canvas Water-Film Background", "水膜流光色彩底板"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    localized("Artwork multi-color mesh", "封面多色水膜背景"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    localized(
                                        "Extracts vibrant, muted, and accent tones from artwork to create fluid organic background for glass refraction.",
                                        "提取封面多重色调构建流光水膜，为液态玻璃提供富有深度的折射底板。",
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = artworkBackground,
                                onCheckedChange = {
                                    artworkBackground = it
                                    preferences.artworkBackground = it
                                    onArtworkBackgroundUpdated(it)
                                },
                            )
                        }

                        if (artworkBackground) {
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(localized("Background color strength", "背景色彩强度"), fontWeight = FontWeight.Medium)
                                    Text("%", color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = colorStrength,
                                    onValueChange = {
                                        colorStrength = it
                                        preferences.colorStrength = it
                                        onColorStrengthUpdated(it)
                                    },
                                    valueRange = 0f..1f,
                                )
                            }
                        }
                    }
                }
            }

            // 5. 重置默认
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(
                        onClick = {
                            glassEnabled = true
                            blurRadius = 24f
                            clarity = 0.78f
                            tintStrength = 0.12f
                            waterFilm = 0.55f
                            refraction = 0.65f
                            preferences.glassEnabled = true
                            preferences.glassBlurRadius = 24f
                            preferences.glassClarity = 0.78f
                            preferences.glassTintStrength = 0.12f
                            preferences.glassWaterFilm = 0.55f
                            preferences.glassRefractionStrength = 0.65f
                            notifySpecChanged()
                        },
                    ) {
                        Text(localized("Reset to Apple Recommended Preset", "恢复官方推荐预设"))
                    }
                }
            }
        }
    }
}
