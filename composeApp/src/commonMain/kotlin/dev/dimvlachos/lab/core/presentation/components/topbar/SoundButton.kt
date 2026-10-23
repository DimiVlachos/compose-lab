package dev.dimvlachos.lab.core.presentation.components.topbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.ic_volume_off
import dev.dimvlachos.lab.resources.ic_volume_up
import dev.dimvlachos.lab.resources.sound_label
import dev.dimvlachos.lab.resources.sound_off
import dev.dimvlachos.lab.resources.sound_on
import org.jetbrains.compose.resources.stringResource

/**
 * A speaker in the bar that shows whether sound is [on] and, tapped, flips it ([onToggle]). It is a
 * switch to TalkBack and VoiceOver: "Sound, on" or "Sound, off".
 */
@Composable
internal fun SoundButton(on: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val state = stringResource(if (on) Res.string.sound_on else Res.string.sound_off)
    LabIconButton(
        if (on) Res.drawable.ic_volume_up else Res.drawable.ic_volume_off,
        contentDescription = stringResource(Res.string.sound_label),
        onClick = onToggle,
        modifier =
            modifier.semantics {
                role = Role.Switch
                toggleableState = ToggleableState(on)
                stateDescription = state
            },
    )
}
