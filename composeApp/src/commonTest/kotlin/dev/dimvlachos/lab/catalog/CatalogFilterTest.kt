package dev.dimvlachos.lab.catalog

import dev.dimvlachos.lab.core.demo.DemoTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

class CatalogFilterTest {
    // The catalog's own words, read once, so the filter sees what the cards show.
    private suspend fun words(): (StringResource) -> String {
        val resources =
            Catalog.demos.flatMap { listOf(it.title, it.description) } +
                Catalog.entries.filterIsInstance<CatalogEntry.Group>().flatMap {
                    listOf(it.title, it.description)
                } +
                DemoTag.entries.map { it.label }
        val text = resources.associateWith { getString(it) }
        return { text.getValue(it) }
    }

    private suspend fun ids(query: CatalogQuery): List<String> =
        filterCatalog(Catalog.entries, query, words()).map {
            when (it) {
                is CatalogEntry.Single -> it.demo.id
                is CatalogEntry.Group -> it.id
            }
        }

    @Test
    fun anEmptyQueryLeavesTheCatalogAsItIs() = runTest {
        assertFalse(CatalogQuery().isActive)
        assertFalse(CatalogQuery(text = "   ").isActive)
        assertSame(Catalog.entries, filterCatalog(Catalog.entries, CatalogQuery(), words()))
    }

    @Test
    fun aSearchBringsVersionsOutOfTheirFolder() = runTest {
        val hits = filterCatalog(Catalog.entries, CatalogQuery(text = "bath"), words())
        val hit = hits.single() as CatalogEntry.Single
        assertEquals("fog.mirror.bathroom", hit.demo.id)
        assertEquals("fog.mirror", hit.folder?.id)
    }

    @Test
    fun theFoldersOwnWordsFindEachOfItsVersions() = runTest {
        assertEquals(
            listOf("fog.mirror.bathroom", "fog.mirror.camera"),
            ids(CatalogQuery(text = "Fogged MIRROR")),
        )
    }

    @Test
    fun everyWordMustMatchSomewhere() = runTest {
        assertEquals(listOf("chat.plane"), ids(CatalogQuery(text = "paper plane")))
        assertEquals(listOf("lamp.cord"), ids(CatalogQuery(text = "lamp.cord")))
        assertEquals(emptyList(), ids(CatalogQuery(text = "plane lamp")))
        assertEquals(emptyList(), ids(CatalogQuery(text = "zzzz")))
    }

    @Test
    fun aTagsNameFindsItsDemos() = runTest {
        assertEquals(listOf("book.turn", "tour.popup"), ids(CatalogQuery(text = "3d")))
    }

    @Test
    fun tagsWidenEachOtherAndNarrowTheText() = runTest {
        assertTrue(CatalogQuery(tags = setOf(DemoTag.Camera)).isActive)
        assertEquals(
            listOf("navbar.all", "morph.app", "fog.mirror.camera"),
            ids(CatalogQuery(tags = setOf(DemoTag.Camera, DemoTag.Navigation))),
        )
        assertEquals(
            listOf("fog.mirror.camera"),
            ids(CatalogQuery(text = "wipe", tags = setOf(DemoTag.Camera))),
        )
    }
}
