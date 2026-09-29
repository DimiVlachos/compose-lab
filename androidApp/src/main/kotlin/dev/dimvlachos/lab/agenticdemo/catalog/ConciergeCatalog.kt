package dev.dimvlachos.lab.agenticdemo.catalog

import android.icu.text.MessageFormat
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.model.catalog.functions.A2uiLocaleProvider
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1

/**
 * The contract the agent builds against: the Material 3 Basic Catalog (Text, Card, List, Tabs,
 * TextField, Slider, ChoicePicker, ...) plus this app's own RatingBar and StatTile, under one
 * catalog id. Video and audio are never offered to the agent in this demo, so they render nothing.
 */
object ConciergeCatalog {
    const val ID = "https://dimvlachos.dev/lab/catalogs/concierge/v1/catalog.json"

    private val basic =
        materialA2uiBasicCatalogV1(
            image = MaterialA2uiBasicCatalogV1Defaults.image(PlaceholderImageRenderer),
            video = MaterialA2uiBasicCatalogV1Defaults.video { _, _, _ -> },
            audioPlayer = MaterialA2uiBasicCatalogV1Defaults.audioPlayer { _, _, _, _ -> },
            urlOpener = {},
            messageFormatter = { pattern, locale, arguments ->
                MessageFormat(pattern, locale).format(arguments)
            },
            localeProvider = A2uiLocaleProvider.Default,
            list = InlineListComponent,
        )

    val catalog: A2uiCatalog =
        A2uiCatalog(
            catalogId = ID,
            components = basic.components + listOf(RatingBarComponent, StatTileComponent),
            functions = basic.functions,
        )
}
