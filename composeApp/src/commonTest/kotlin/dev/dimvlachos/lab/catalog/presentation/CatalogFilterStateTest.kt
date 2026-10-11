package dev.dimvlachos.lab.catalog.presentation

import androidx.compose.runtime.saveable.SaverScope
import dev.dimvlachos.lab.core.demo.DemoTag
import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogFilterStateTest {
    @Test
    fun theTagsAreSavedButTheSearchTextIsNot() {
        val state = CatalogFilterState(text = "bath", tags = setOf(DemoTag.Camera, DemoTag.Text))
        val saved = with(CatalogFilterState.Saver) { SaverScope { true }.save(state) }!!
        val restored = CatalogFilterState.Saver.restore(saved)!!
        assertEquals("", restored.text, "the search comes back closed, so its text goes")
        assertEquals(setOf(DemoTag.Camera, DemoTag.Text), restored.tags)
    }
}
