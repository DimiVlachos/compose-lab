package dev.dimvlachos.lab

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.dimvlachos.lab.core.platform.InMemoryThemeModeStore
import dev.dimvlachos.lab.core.presentation.ui.ThemeMode
import dev.dimvlachos.lab.core.presentation.ui.ThemeRevealState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ThemeRevealTest {
    @Test
    fun theThemeButtonSpreadsTheNewThemeOverTheOld() = runComposeUiTest {
        val store = InMemoryThemeModeStore(ThemeMode.Light)
        val reveal = ThemeRevealState()
        setContent {
            LabApp(
                initialDemoId = null,
                record = false,
                label = false,
                themeStore = store,
                reveal = reveal,
            )
        }
        waitForIdle()
        mainClock.autoAdvance = false
        onNodeWithContentDescription("Theme: light").performClick()
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        assertTrue(reveal.revealing, "the dark theme must spread over the light one")
        assertEquals(ThemeMode.Dark, store.load())
        onNodeWithContentDescription("Theme: dark").assertExists()
        mainClock.advanceTimeBy(600)
        assertFalse(reveal.revealing, "it must have spread over the whole screen")
    }

    @Test
    fun aSecondTapWhileItSpreadsStartsAgainAndStillEnds() = runComposeUiTest {
        val store = InMemoryThemeModeStore(ThemeMode.Light)
        val reveal = ThemeRevealState()
        setContent {
            LabApp(
                initialDemoId = null,
                record = false,
                label = false,
                themeStore = store,
                reveal = reveal,
            )
        }
        waitForIdle()
        mainClock.autoAdvance = false
        onNodeWithContentDescription("Theme: light").performClick()
        mainClock.advanceTimeBy(200)
        onNodeWithContentDescription("Theme: dark").performClick()
        mainClock.advanceTimeBy(200)
        assertTrue(reveal.revealing)
        assertEquals(ThemeMode.System, store.load())
        mainClock.advanceTimeBy(600)
        assertFalse(
            reveal.revealing,
            "the second switch must end, and leave nothing over the screen",
        )
    }
}
