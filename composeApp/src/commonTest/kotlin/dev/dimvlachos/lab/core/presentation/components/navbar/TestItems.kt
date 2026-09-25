package dev.dimvlachos.lab.core.presentation.components.navbar

import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.ic_home
import dev.dimvlachos.lab.resources.ic_home_filled
import dev.dimvlachos.lab.resources.ic_profile
import dev.dimvlachos.lab.resources.ic_profile_filled
import dev.dimvlachos.lab.resources.ic_saved
import dev.dimvlachos.lab.resources.ic_saved_filled
import dev.dimvlachos.lab.resources.ic_search

internal val testItems: List<NavItem> =
    listOf(
        NavItem("Home", Res.drawable.ic_home, Res.drawable.ic_home_filled),
        NavItem("Search", Res.drawable.ic_search, Res.drawable.ic_search),
        NavItem("Saved", Res.drawable.ic_saved, Res.drawable.ic_saved_filled),
        NavItem("Profile", Res.drawable.ic_profile, Res.drawable.ic_profile_filled),
    )
