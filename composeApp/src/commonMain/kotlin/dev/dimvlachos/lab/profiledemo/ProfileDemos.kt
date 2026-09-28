package dev.dimvlachos.lab.profiledemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.profiledemo.presentation.components.ProfileDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_morph_avatar
import dev.dimvlachos.lab.resources.demo_morph_fab
import dev.dimvlachos.lab.resources.demo_morph_profile
import dev.dimvlachos.lab.resources.demo_morph_search
import kotlin.time.Duration.Companion.seconds

internal object ProfileDemos {
    private fun twice(index: Int, openFor: Double) = demoScript {
        at(0.8.seconds) { select(index) }
        at((0.8 + openFor).seconds) { select(0) }
        at((2.0 + openFor).seconds) { select(index) }
        at((2.0 + 2 * openFor).seconds) { select(0) }
    }

    // Search stays open longer: the pill lands, then the query types and the results come in.
    private val avatarTour = twice(index = 1, openFor = 2.0)
    private val searchTour = twice(index = 2, openFor = 2.6)
    private val fabTour = twice(index = 3, openFor = 2.0)

    private val profileTour = demoScript {
        at(0.8.seconds) { select(1) }
        at(2.6.seconds) { select(0) }
        at(3.6.seconds) { select(2) }
        at(6.2.seconds) { select(0) }
        at(7.2.seconds) { select(3) }
        at(9.0.seconds) { select(0) }
    }

    val all: List<Demo> =
        listOf(
            Demo("morph.avatar", Res.string.demo_morph_avatar, avatarTour) { ProfileDemo(it) },
            Demo("morph.search", Res.string.demo_morph_search, searchTour) { ProfileDemo(it) },
            Demo("morph.fab", Res.string.demo_morph_fab, fabTour) { ProfileDemo(it) },
            Demo("morph.profile", Res.string.demo_morph_profile, profileTour) { ProfileDemo(it) },
        )
}
