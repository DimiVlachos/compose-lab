package dev.dimvlachos.lab.catalog.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.dimvlachos.lab.catalog.CatalogQuery
import dev.dimvlachos.lab.core.demo.DemoTag

/**
 * The words in home's search field and the tags picked under it. Only the tags are saved: the
 * search itself isn't, and comes back closed, so its words go with it.
 */
@Stable
internal class CatalogFilterState(text: String = "", tags: Set<DemoTag> = emptySet()) {
    var text by mutableStateOf(text)

    var tags by mutableStateOf(tags)
        private set

    /** What the catalog is filtered by. */
    val query: CatalogQuery
        get() = CatalogQuery(text, tags)

    /** Picks [tag], or puts it back if it was picked. */
    fun toggle(tag: DemoTag) {
        tags = if (tag in tags) tags - tag else tags + tag
    }

    companion object {
        val Saver: Saver<CatalogFilterState, Any> =
            listSaver(
                save = { it.tags.map(DemoTag::name) },
                restore = { saved ->
                    CatalogFilterState(tags = saved.mapNotNull { name -> tagNamed(name) }.toSet())
                },
            )

        private fun tagNamed(name: String): DemoTag? =
            DemoTag.entries.firstOrNull { it.name == name }
    }
}

/** A [CatalogFilterState] that outlives the screens; its tags also outlive the app's process. */
@Composable
internal fun rememberCatalogFilterState(): CatalogFilterState =
    rememberSaveable(saver = CatalogFilterState.Saver) { CatalogFilterState() }
