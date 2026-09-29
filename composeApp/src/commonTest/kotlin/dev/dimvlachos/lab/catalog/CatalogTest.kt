@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.catalog

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.FakeController
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_morph_app
import dev.dimvlachos.lab.resources.demo_navbar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class CatalogTest {
    @Test
    fun keepsTheFinishedDemoOfEachComponent() {
        assertEquals(listOf("navbar.all", "morph.app"), Catalog.demos.map { it.id })
        assertEquals(
            listOf(Res.string.demo_navbar, Res.string.demo_morph_app),
            Catalog.demos.map { it.title },
        )
    }

    @Test
    fun unknownOrMissingIdsFindNothing() {
        assertNull(Catalog.find(null))
        assertNull(Catalog.find("navbar.nope"))
    }

    @Test
    fun platformDemosAreFoundAlongsideTheSharedOnes() {
        val extra =
            Demo("platform.only", Res.string.demo_navbar, demoScript {}, interactive = true) {}
        val demos = Catalog.demos + extra

        assertSame(extra, Catalog.find("platform.only", demos))
        assertEquals("morph.app", Catalog.find("morph.app", demos)?.id)
        assertNull(Catalog.find("platform.only"))
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
}
