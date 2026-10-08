package dev.dimvlachos.lab.magnetdemo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.dimvlachos.lab.core.presentation.components.magnet.MagnetPhoto
import dev.dimvlachos.lab.core.presentation.components.magnet.MagnetState
import dev.dimvlachos.lab.core.presentation.components.magnet.MagnetTag
import dev.dimvlachos.lab.core.presentation.components.magnet.rememberMagnetState
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.magnet_tag_boats
import dev.dimvlachos.lab.resources.magnet_tag_cliffs
import dev.dimvlachos.lab.resources.magnet_tag_sea
import dev.dimvlachos.lab.resources.magnet_tag_sunset
import dev.dimvlachos.lab.resources.magnet_tag_village
import dev.dimvlachos.lab.resources.photo_corfu
import dev.dimvlachos.lab.resources.photo_corfu_title
import dev.dimvlachos.lab.resources.photo_crete
import dev.dimvlachos.lab.resources.photo_crete_title
import dev.dimvlachos.lab.resources.photo_folegandros
import dev.dimvlachos.lab.resources.photo_folegandros_title
import dev.dimvlachos.lab.resources.photo_hydra
import dev.dimvlachos.lab.resources.photo_hydra_title
import dev.dimvlachos.lab.resources.photo_kefalonia
import dev.dimvlachos.lab.resources.photo_kefalonia_title
import dev.dimvlachos.lab.resources.photo_milos
import dev.dimvlachos.lab.resources.photo_milos_title
import dev.dimvlachos.lab.resources.photo_mykonos
import dev.dimvlachos.lab.resources.photo_mykonos_title
import dev.dimvlachos.lab.resources.photo_naxos
import dev.dimvlachos.lab.resources.photo_naxos_title
import dev.dimvlachos.lab.resources.photo_paxos
import dev.dimvlachos.lab.resources.photo_paxos_title
import dev.dimvlachos.lab.resources.photo_rhodes
import dev.dimvlachos.lab.resources.photo_rhodes_title
import dev.dimvlachos.lab.resources.photo_santorini
import dev.dimvlachos.lab.resources.photo_santorini_title
import dev.dimvlachos.lab.resources.photo_zakynthos
import dev.dimvlachos.lab.resources.photo_zakynthos_title
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal class IslandTag(val id: String, val label: StringResource)

internal class IslandPhoto(
    val id: String,
    val image: DrawableResource,
    val title: StringResource,
    val strengths: Map<String, Float>,
)

/**
 * The twelve bundled island photos and how well each matches the five tags, read off what each
 * shows, by hand. Each tag pulls three to six strongly and a couple weakly; Sunset and Sea share
 * Corfu and Naxos, Boats and Village share Paxos and Hydra, to show two magnets combined.
 */
internal object IslandTags {
    const val Sunset = "sunset"
    const val Sea = "sea"
    const val Boats = "boats"
    const val Cliffs = "cliffs"
    const val Village = "village"

    val tags =
        listOf(
            IslandTag(Sunset, Res.string.magnet_tag_sunset),
            IslandTag(Sea, Res.string.magnet_tag_sea),
            IslandTag(Boats, Res.string.magnet_tag_boats),
            IslandTag(Cliffs, Res.string.magnet_tag_cliffs),
            IslandTag(Village, Res.string.magnet_tag_village),
        )

    val photos =
        listOf(
            IslandPhoto(
                "corfu",
                Res.drawable.photo_corfu,
                Res.string.photo_corfu_title,
                mapOf(Sunset to 0.9f, Sea to 0.6f, Cliffs to 0.3f),
            ),
            IslandPhoto(
                "paxos",
                Res.drawable.photo_paxos,
                Res.string.photo_paxos_title,
                mapOf(Village to 0.9f, Boats to 0.7f, Sunset to 0.4f, Sea to 0.3f),
            ),
            IslandPhoto(
                "santorini",
                Res.drawable.photo_santorini,
                Res.string.photo_santorini_title,
                mapOf(Sunset to 0.9f, Village to 0.8f, Cliffs to 0.4f, Sea to 0.3f),
            ),
            IslandPhoto(
                "milos",
                Res.drawable.photo_milos,
                Res.string.photo_milos_title,
                mapOf(Cliffs to 1f, Sea to 0.9f, Boats to 0.7f),
            ),
            IslandPhoto(
                "naxos",
                Res.drawable.photo_naxos,
                Res.string.photo_naxos_title,
                mapOf(Sunset to 1f, Sea to 0.5f),
            ),
            IslandPhoto(
                "hydra",
                Res.drawable.photo_hydra,
                Res.string.photo_hydra_title,
                mapOf(Boats to 1f, Village to 0.6f, Sea to 0.4f),
            ),
            IslandPhoto(
                "mykonos",
                Res.drawable.photo_mykonos,
                Res.string.photo_mykonos_title,
                mapOf(Village to 0.9f, Sea to 0.4f, Boats to 0.3f, Sunset to 0.3f),
            ),
            IslandPhoto(
                "crete",
                Res.drawable.photo_crete,
                Res.string.photo_crete_title,
                mapOf(Sea to 1f, Boats to 0.8f, Village to 0.2f),
            ),
            IslandPhoto(
                "rhodes",
                Res.drawable.photo_rhodes,
                Res.string.photo_rhodes_title,
                mapOf(Sea to 0.9f, Cliffs to 0.8f),
            ),
            IslandPhoto(
                "zakynthos",
                Res.drawable.photo_zakynthos,
                Res.string.photo_zakynthos_title,
                mapOf(Cliffs to 0.9f, Sea to 0.8f, Boats to 0.4f),
            ),
            IslandPhoto(
                "kefalonia",
                Res.drawable.photo_kefalonia,
                Res.string.photo_kefalonia_title,
                mapOf(Village to 0.8f, Sea to 0.4f),
            ),
            IslandPhoto(
                "folegandros",
                Res.drawable.photo_folegandros,
                Res.string.photo_folegandros_title,
                mapOf(Sunset to 0.9f, Cliffs to 0.8f, Village to 0.3f),
            ),
        )
}

/** A [MagnetState] for the twelve islands and the five tags, in the reader's language. */
@Composable
internal fun rememberIslandMagnetState(): MagnetState {
    val titles = IslandTags.photos.map { stringResource(it.title) }
    val labels = IslandTags.tags.map { stringResource(it.label) }
    val photos =
        remember(titles) {
            IslandTags.photos.mapIndexed { i, p ->
                MagnetPhoto(p.id, p.image, titles[i], p.strengths)
            }
        }
    val tags =
        remember(labels) { IslandTags.tags.mapIndexed { i, t -> MagnetTag(t.id, labels[i]) } }
    return rememberMagnetState(photos, tags)
}
