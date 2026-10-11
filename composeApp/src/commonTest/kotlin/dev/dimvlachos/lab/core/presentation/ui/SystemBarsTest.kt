package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SystemBarsTest {
    @Test
    fun theIconsAreDarkOnTheLightThemeUnlessAScreenAsks() {
        assertTrue(darkBarIcons(request = null, dark = false))
        assertFalse(darkBarIcons(request = null, dark = true))
        assertTrue(darkBarIcons(request = true, dark = true))
        assertFalse(darkBarIcons(request = false, dark = false))
    }

    @Test
    fun aRequestLastsWhileItsScreenIsShownAndTheOneBeforeComesBack() = runComposeUiTest {
        val requests = SystemBarRequests()
        var outer by mutableStateOf(true)
        var inner by mutableStateOf(true)
        setContent {
            CompositionLocalProvider(LocalSystemBarRequests provides requests) {
                if (outer) {
                    RequestSystemBarIcons(navigationDarkIcons = true)
                    if (inner) RequestSystemBarIcons(navigationDarkIcons = false)
                }
            }
        }
        waitForIdle()
        assertEquals(false, requests.navigationDarkIcons)
        assertNull(requests.statusDarkIcons, "the status bar wasn't asked about")
        inner = false
        waitForIdle()
        assertEquals(true, requests.navigationDarkIcons)
        outer = false
        waitForIdle()
        assertNull(requests.navigationDarkIcons)
    }
}
