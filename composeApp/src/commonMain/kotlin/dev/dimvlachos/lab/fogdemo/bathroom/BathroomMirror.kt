package dev.dimvlachos.lab.fogdemo.bathroom

import androidx.compose.runtime.Composable
import dev.dimvlachos.lab.core.audio.MicAccess
import dev.dimvlachos.lab.core.audio.rememberMicAccess
import dev.dimvlachos.lab.core.camera.CameraAccess
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.fogdemo.presentation.components.FogDemo

/**
 * The fogged mirror of a steamy bathroom: a still reflection, the one the clip is recorded from. It
 * never asks for the camera, only for the microphone that hears a breath.
 */
@Composable
internal fun BathroomMirror(
    state: DemoState,
    micAccess: MicAccess = rememberMicAccess(enabled = !state.recording),
) {
    FogDemo(
        state,
        micAccess = micAccess,
        cameraAccess = CameraAccess.Unavailable,
        reflection = false,
    )
}
