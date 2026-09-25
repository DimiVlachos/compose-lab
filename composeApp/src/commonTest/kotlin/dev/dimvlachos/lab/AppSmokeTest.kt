package dev.dimvlachos.lab

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.dimvlachos.lab.core.platform.platformLabel
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class AppSmokeTest {
    @Test
    fun showsTheCatalogWhenNoDemoIsRequested() = runComposeUiTest {
        setContent { App(initialDemoId = null, record = false, label = false) }
        onNodeWithText("compose-lab").assertExists()
    }

    @Test
    fun unknownDemoFallsBackToTheCatalog() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { App(initialDemoId = "navbar.nope", record = false, label = false) }
        onNodeWithText("compose-lab").assertExists()
    }

    @Test
    fun knownDemoOpensDirectly() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { App(initialDemoId = "navbar.indicator", record = false, label = false) }
        onNodeWithText("compose-lab").assertDoesNotExist()
        onNodeWithText("Morphing indicator").assertExists()
    }

    @Test
    fun labelShowsThePlatformNameOnTheStage() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { App(initialDemoId = "navbar.indicator", record = true, label = true) }
        val expected = runBlocking { getString(platformLabel) }
        onNodeWithText(expected).assertExists()
    }
}
