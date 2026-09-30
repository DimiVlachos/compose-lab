package dev.dimvlachos.lab.fogdemo.bathroom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.dimvlachos.lab.core.audio.MicAccess
import dev.dimvlachos.lab.core.camera.CameraAccess
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.fogdemo.newFogDemoState
import dev.dimvlachos.lab.fogdemo.presentation.components.FogDemo

/**
 * The fogged mirror of a steamy bathroom: a still reflection to wipe and watch the drops run on,
 * the one the clip is recorded from. It asks for nothing: no camera, no microphone, and no breath
 * on the phone; only the clip breathes over it.
 */
@Composable
internal fun BathroomMirror(state: DemoState, fog: FogState = remember { newFogDemoState() }) {
    FogDemo(
        state,
        fog,
        micAccess = MicAccess.Unavailable,
        cameraAccess = CameraAccess.Unavailable,
        breathing = false,
    )
}
