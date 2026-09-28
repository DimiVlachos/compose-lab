@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.catalog

import dev.dimvlachos.lab.core.demo.FakeController
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.section_morph
import dev.dimvlachos.lab.resources.section_navbar
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
                "morph.app",
            ),
            ids,
        )
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun groupsTheDemosIntoNavBarThenImageMorph() {
        assertEquals(
            listOf(Res.string.section_navbar, Res.string.section_morph),
            Catalog.sections.map { it.title },
        )
        assertEquals(
            listOf("morph.bounds", "morph.corners", "morph.chrome", "morph.app"),
            Catalog.sections[1].demos.map { it.id },
        )
        assertEquals(Catalog.demos, Catalog.sections.flatMap { it.demos })
    }

    @Test
    fun eachDemoKnowsItsSection() {
        assertEquals(Catalog.sections[0], Catalog.sectionOf(Catalog.find("navbar.scroll")!!))
        assertEquals(Catalog.sections[1], Catalog.sectionOf(Catalog.find("morph.app")!!))
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
