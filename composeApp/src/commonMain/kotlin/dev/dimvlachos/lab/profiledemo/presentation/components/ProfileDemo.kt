package dev.dimvlachos.lab.profiledemo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.profile.ProfileMorphs
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.portrait
import dev.dimvlachos.lab.resources.profile_name
import dev.dimvlachos.lab.resources.profile_title
import org.jetbrains.compose.resources.stringResource

private const val DemoQuery = "Cor"
private val DemoCandidates = listOf("Corfu", "Corfu Old Town", "Corinth", "Naxos", "Paxos")

// select(1..3) opens a morph; select(0) closes it.
@Composable
internal fun ProfileDemo(state: DemoState) {
    ProfileMorphs(
        title = stringResource(Res.string.profile_title),
        name = stringResource(Res.string.profile_name),
        portrait = Res.drawable.portrait,
        searchQuery = DemoQuery,
        searchCandidates = DemoCandidates,
        openIndex = state.selectedIndex,
        onOpen = state::select,
        onClose = { state.select(0) },
        modifier = Modifier.fillMaxSize().background(LabTheme.colors.background),
    )
}
