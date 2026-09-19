package com.aemusic.feature.shell

/** A single active destination prevents settings overlays from stacking. */
internal enum class SettingsPage {
    Root,
    Sources,
    Developer,
    Glass,
    Audio,
    Appearance,
    CloudSync,
    Backup,
}

internal fun SettingsPage.parent(fourTabs: Boolean): SettingsPage? = when (this) {
    SettingsPage.Root -> null
    SettingsPage.Glass -> SettingsPage.Appearance
    else -> if (fourTabs) null else SettingsPage.Root
}
