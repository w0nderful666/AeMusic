package com.aemusic.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage { System, English, Chinese }

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.English }

@Composable
fun localized(english: String, chinese: String): String =
    if (LocalAppLanguage.current == AppLanguage.Chinese) chinese else english
