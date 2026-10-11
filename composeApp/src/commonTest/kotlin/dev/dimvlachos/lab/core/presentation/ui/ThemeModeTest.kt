package dev.dimvlachos.lab.core.presentation.ui

import dev.dimvlachos.lab.core.platform.InMemoryThemeModeStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeModeTest {
    @Test
    fun theModesCycleSystemLightDark() {
        assertEquals(ThemeMode.Light, ThemeMode.System.next())
        assertEquals(ThemeMode.Dark, ThemeMode.Light.next())
        assertEquals(ThemeMode.System, ThemeMode.Dark.next())
    }

    @Test
    fun systemFollowsTheSystemAndTheOthersDoNot() {
        assertTrue(ThemeMode.System.isDark(systemDark = true))
        assertFalse(ThemeMode.System.isDark(systemDark = false))
        assertFalse(ThemeMode.Light.isDark(systemDark = true))
        assertTrue(ThemeMode.Dark.isDark(systemDark = false))
    }

    @Test
    fun recordingIsAlwaysDark() {
        for (mode in ThemeMode.entries) {
            assertTrue(useDarkTheme(mode, systemDark = false, record = true), mode.name)
        }
        assertFalse(useDarkTheme(ThemeMode.Light, systemDark = true, record = false))
        assertFalse(useDarkTheme(ThemeMode.System, systemDark = false, record = false))
    }

    @Test
    fun eachModeComesBackFromItsKey() {
        for (mode in ThemeMode.entries) assertEquals(mode, ThemeMode.fromKey(mode.key))
        assertEquals(listOf("system", "light", "dark"), ThemeMode.entries.map { it.key })
        assertEquals(ThemeMode.System, ThemeMode.fromKey(null))
        assertEquals(ThemeMode.System, ThemeMode.fromKey("sepia"))
    }

    @Test
    fun theControllerStartsFromTheStoreAndSavesEachCycle() {
        val store = InMemoryThemeModeStore(ThemeMode.Dark)
        val theme = ThemeController(store)
        assertEquals(ThemeMode.Dark, theme.mode)
        theme.cycle()
        assertEquals(ThemeMode.System, theme.mode)
        assertEquals(ThemeMode.System, store.load())
        theme.cycle()
        assertEquals(ThemeMode.Light, ThemeController(store).mode)
    }

    @Test
    fun anEmptyStoreStartsOnSystem() {
        val store = InMemoryThemeModeStore()
        assertEquals(ThemeMode.System, store.load())
        assertEquals(ThemeMode.System, ThemeController(store).mode)
    }
}
