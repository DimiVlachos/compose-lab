@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.catalog

import dev.dimvlachos.lab.core.demo.FakeController
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_frost
import dev.dimvlachos.lab.resources.demo_morph_app
import dev.dimvlachos.lab.resources.demo_navbar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class CatalogTest {
    @Test
    fun keepsTheFinishedDemoOfEachComponent() {
        assertEquals(listOf("navbar.all", "morph.app", "frost.window"), Catalog.demos.map { it.id })
        assertEquals(
            listOf(Res.string.demo_navbar, Res.string.demo_morph_app, Res.string.demo_frost),
            Catalog.demos.map { it.title },
        )
    }

    @Test
    fun unknownOrMissingIdsFindNothing() {
        assertNull(Catalog.find(null))
        assertNull(Catalog.find("navbar.nope"))
    }

    @Test
    fun everyScriptFitsInALinkedInClip() {
        for (demo in Catalog.demos) {
            assertTrue(
                demo.script.nominalDuration <= 40.seconds,
                "${demo.id} runs ${demo.script.nominalDuration}",
            )
        }
    }

    @Test
    fun everyScriptEndsWhereItStartedSoTheClipLoops() = runTest {
        for (demo in Catalog.demos) {
            val controller = FakeController { testScheduler.currentTime }
            demo.script.play(controller)
            assertEquals(0, controller.selectedIndex, "${demo.id} must end on the first tab")
            assertEquals(0f, controller.netScroll, "${demo.id} must scroll back to the top")
        }
    }

    @Test
    fun everyScriptedWipeStaysOnTheStage() = runTest {
        for (demo in Catalog.demos) {
            val controller = FakeController { testScheduler.currentTime }
            demo.script.play(controller)
            for (point in controller.wipes.flatten()) {
                assertTrue(point.x in 0f..1f && point.y in 0f..1f, "${demo.id} wipes off at $point")
            }
        }
    }
}
