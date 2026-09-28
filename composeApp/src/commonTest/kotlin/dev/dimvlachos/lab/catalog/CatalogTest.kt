@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.catalog

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class CatalogTest {
    @Test
    fun containsTheTwelveDemosInCatalogOrderWithUniqueIds() {
        val ids = Catalog.demos.map { it.id }
        assertEquals(
            listOf(
                "navbar.indicator",
                "navbar.icons",
                "navbar.scroll",
                "navbar.cutout",
                "navbar.cutoutmorph",
                "navbar.action",
                "navbar.all",
                "navbar.recompositions",
                "morph.bounds",
                "morph.corners",
                "morph.chrome",
                "morph.all",
            ),
            ids,
        )
        assertEquals(ids.size, ids.toSet().size)
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
}
