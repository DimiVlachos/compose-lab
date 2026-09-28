package dev.dimvlachos.lab.core.presentation.components.gallery

import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphPhoto
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.photo_corfu
import dev.dimvlachos.lab.resources.photo_hydra
import dev.dimvlachos.lab.resources.photo_milos
import dev.dimvlachos.lab.resources.photo_naxos
import dev.dimvlachos.lab.resources.photo_paxos
import dev.dimvlachos.lab.resources.photo_santorini

internal val IslandTitles = listOf("Corfu", "Paxos", "Santorini", "Milos", "Naxos", "Hydra")

internal fun testIslands(): List<MorphPhoto> =
    listOf(
            Res.drawable.photo_corfu,
            Res.drawable.photo_paxos,
            Res.drawable.photo_santorini,
            Res.drawable.photo_milos,
            Res.drawable.photo_naxos,
            Res.drawable.photo_hydra,
        )
        .mapIndexed { i, image -> MorphPhoto(image, IslandTitles[i], "Caption ${IslandTitles[i]}") }
