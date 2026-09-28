package dev.dimvlachos.lab.imagemorphdemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphLayers
import dev.dimvlachos.lab.imagemorphdemo.presentation.components.ImageMorphDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_morph_bounds
import dev.dimvlachos.lab.resources.demo_morph_chrome
import dev.dimvlachos.lab.resources.demo_morph_corners
import kotlin.time.Duration.Companion.seconds

internal object ImageMorphDemos {
    // Three cards from different corners of the grid, each opened and closed again. An open dwells
    // long enough for the 400 ms morph, the chrome's wait and fade, and a beat at rest.
    private val morphTour = demoScript {
        at(0.8.seconds) { select(2) }
        at(2.6.seconds) { select(0) }
        at(4.0.seconds) { select(5) }
        at(5.8.seconds) { select(0) }
        at(7.2.seconds) { select(3) }
        at(9.0.seconds) { select(0) }
    }

    // The three layers, each on its own, so every clip isolates the defect its fix cures. The
    // finished
    // morph ships inside the profile gallery (morph.app).
    val layers: List<Demo> =
        listOf(
            Demo("morph.bounds", Res.string.demo_morph_bounds, morphTour) {
                ImageMorphDemo(it, MorphLayers())
            },
            Demo("morph.corners", Res.string.demo_morph_corners, morphTour) {
                ImageMorphDemo(it, MorphLayers(remeasure = true, pairedCorners = true))
            },
            Demo("morph.chrome", Res.string.demo_morph_chrome, morphTour) {
                ImageMorphDemo(it, MorphLayers(remeasure = true, stagedChrome = true))
            },
        )
}
