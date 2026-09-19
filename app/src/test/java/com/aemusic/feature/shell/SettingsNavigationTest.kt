package com.aemusic.feature.shell

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsNavigationTest {
    @Test fun rootClosesSettingsOverlay() {
        assertEquals(null, SettingsPage.Root.parent(fourTabs = true))
    }

    @Test fun glassReturnsToAppearance() {
        assertEquals(SettingsPage.Appearance, SettingsPage.Glass.parent(fourTabs = true))
    }

    @Test fun ordinarySubpagesReturnToSettingsRootWhenSettingsTabIsHidden() {
        val pages = SettingsPage.entries - SettingsPage.Root - SettingsPage.Glass
        pages.forEach { page ->
            assertEquals(SettingsPage.Root, page.parent(fourTabs = false))
        }
    }

    @Test fun ordinarySubpagesReturnToVisibleSettingsTabInFourTabMode() {
        val pages = SettingsPage.entries - SettingsPage.Root - SettingsPage.Glass
        pages.forEach { page ->
            assertEquals(null, page.parent(fourTabs = true))
        }
    }
}
