package com.aemusic.feature.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aemusic.design.theme.AeSpacing

@Composable
fun AppearanceSettingsScreen(
    padding: PaddingValues,
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    onBack: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsSubpageHeader(
                title = localized("Appearance & Visuals", "外观与个性化"),
                onBack = onBack,
            )
            LazyColumn(
                contentPadding = PaddingValues(
                    start = AeSpacing.md,
                    top = AeSpacing.sm,
                    end = AeSpacing.md,
                    bottom = padding.calculateBottomPadding() + AeSpacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(AeSpacing.lg),
            ) {
                // 1. 液态玻璃与材质
                item {
                    SettingsGroup(localized("Liquid Glass & Material", "液态玻璃与视觉定制")) {
                        NavigationRow(
                            title = localized("Liquid Glass & Material Customization", "液态玻璃与视觉参数定制"),
                            description = localized("Tune blur radius, water-film, edge refraction, and low-end mode", "定制模糊半径、水膜流动感、边框折射高光与低配性能模式"),
                            onClick = { onAction(SettingsAction.OpenGlassSettings) },
                        )
                        GroupDivider()
                        ToggleRow(
                            title = localized("Glass surfaces", "快速开关液态玻璃"),
                            description = localized("Enable or disable liquid glass globally", "全局开启或关闭底栏与弹窗的液态玻璃效果"),
                            checked = state.glassEnabled,
                            onChecked = { onAction(SettingsAction.SetGlassEnabled(it)) },
                        )
                    }
                }

                // 2. 封面色彩取色
                item {
                    SettingsGroup(localized("Artwork Colors & Theme", "色彩与封面取色")) {
                        ToggleRow(
                            title = localized("Artwork background", "封面取色背景"),
                            description = localized("Tint the app canvas from the current artwork", "让应用全局背景画布跟随当前歌曲封面智能提取渐变色彩"),
                            checked = state.artworkBackground,
                            onChecked = { onAction(SettingsAction.SetArtworkBackground(it)) },
                        )
                        GroupDivider()
                        ToggleRow(
                            title = localized("Artwork colors in player", "播放器封面动态着色"),
                            description = localized("Use artwork color in player surfaces and accents", "在全屏播放器背景与高亮控件中注入封面提取色"),
                            checked = state.artworkColors,
                            onChecked = { onAction(SettingsAction.SetArtworkColors(it)) },
                        )
                        GroupDivider()
                        SliderRow(
                            title = localized("Background color strength", "背景取色强度"),
                            value = state.colorStrength,
                            enabled = state.artworkBackground,
                            valueLabel = strengthLabel(state.colorStrength),
                            onValueChange = { onAction(SettingsAction.SetColorStrength(it)) },
                        )
                        GroupDivider()
                        ToggleRow(
                            title = localized("Immersive page titles", "沉浸式大标题"),
                            description = localized("Use prominent large display typography on top-level pages", "在首页、曲库等一级页面启用大号标题排版"),
                            checked = state.immersiveTitles,
                            onChecked = { onAction(SettingsAction.SetImmersiveTitles(it)) },
                        )
                    }
                }

                // 3. 导航栏布局
                item {
                    SettingsGroup(localized("Navigation", "导航布局")) {
                        ToggleRow(
                            title = localized("Four bottom destinations", "四个底部入口"),
                            description = localized("Show Settings tab directly in the bottom dock bar", "在底部悬浮栏常驻显示“设置”标签；关闭后可通过曲库右上角进入"),
                            checked = state.fourTabs,
                            onChecked = { onAction(SettingsAction.SetFourTabs(it)) },
                        )
                    }
                }

                // 4. 界面语言
                item {
                    SettingsGroup(localized("Language", "界面语言")) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(AeSpacing.md),
                            horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs),
                        ) {
                            AppLanguage.entries.forEach { option ->
                                FilterChip(
                                    selected = state.language == option,
                                    onClick = { onAction(SettingsAction.SetLanguage(option)) },
                                    label = { Text(languageLabel(option)) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
