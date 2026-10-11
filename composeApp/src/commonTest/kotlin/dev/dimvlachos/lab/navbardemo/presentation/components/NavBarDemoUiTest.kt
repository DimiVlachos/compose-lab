package dev.dimvlachos.lab.navbardemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.navbar.NavBarRowTestTag
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class NavBarDemoUiTest {
    @Test
    fun onAPhoneTurnedOnItsSideTheBarKeepsAPhonesWidthCentredAndOnScreen() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { LabTheme { Box(Modifier.size(800.dp, 300.dp)) { NavBarDemo(DemoState()) } } }
        mainClock.advanceTimeBy(500)
        val bar = onNodeWithTag(NavBarRowTestTag).getUnclippedBoundsInRoot()
        assertTrue(bar.width <= NavBarMaxWidth, "bar $bar is wider than $NavBarMaxWidth")
        // The bar and the action button beside it share a 480 dp space in the middle.
        assertTrue(abs((bar.left - 160.dp).value) < 1f, "bar $bar is off centre")
        assertTrue(bar.bottom <= 300.dp && bar.top >= 0.dp, "bar $bar spills out")
    }

    @Test
    fun onAPhoneTheBarKeepsItsMargins() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { LabTheme { Box(Modifier.size(360.dp, 760.dp)) { NavBarDemo(DemoState()) } } }
        mainClock.advanceTimeBy(500)
        val bar = onNodeWithTag(NavBarRowTestTag).getUnclippedBoundsInRoot()
        assertTrue(abs((bar.left - 20.dp).value) < 1f, "bar $bar")
        assertTrue(abs((bar.bottom - 740.dp).value) < 1f, "bar $bar")
    }
}
