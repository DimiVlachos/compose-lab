package dev.dimvlachos.lab.agenticdemo.catalog

import androidx.a2ui.compose.runtime.A2uiComponentReference
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.dimvlachos.lab.core.presentation.ui.LabTheme

/**
 * The Basic Catalog's List, laid out inline. Material's version is a LazyColumn, which cannot be
 * measured inside the chat's own scrolling transcript; here the transcript does the scrolling, so a
 * vertical list is a plain Column (a horizontal one still scrolls sideways). Same name and schema,
 * so the agent sees no difference.
 */
object InlineListComponent : A2uiBasicCatalogV1.List {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        children: List<A2uiComponentReference>,
        direction: A2uiBasicCatalogV1.List.Direction,
        align: A2uiBasicCatalogV1.List.Align,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {
        val spacing = Arrangement.spacedBy(LabTheme.spacing.small)
        when (direction) {
            A2uiBasicCatalogV1.List.Direction.Vertical ->
                Column(
                    modifier,
                    verticalArrangement = spacing,
                    horizontalAlignment =
                        when (align) {
                            A2uiBasicCatalogV1.List.Align.Center -> Alignment.CenterHorizontally
                            A2uiBasicCatalogV1.List.Align.End -> Alignment.End
                            else -> Alignment.Start
                        },
                ) {
                    val stretch = align == A2uiBasicCatalogV1.List.Align.Stretch
                    children.forEach {
                        Child(it, if (stretch) Modifier.fillMaxWidth() else Modifier)
                    }
                }
            A2uiBasicCatalogV1.List.Direction.Horizontal ->
                Row(
                    modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = spacing,
                ) {
                    children.forEach { Child(it, Modifier) }
                }
        }
    }

    @Composable
    private fun A2uiComponentScope.Child(reference: A2uiComponentReference, modifier: Modifier) {
        key(reference.id, reference.baseDataPath) {
            val state = observeA2uiComponentState(reference)
            if (state is A2uiComponentState.Success) A2uiComponent(state.component, modifier)
        }
    }
}
