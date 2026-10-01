package dev.dimvlachos.lab.fogdemo.reflection

import androidx.compose.runtime.Composable
import dev.dimvlachos.lab.core.camera.rememberCameraAccess
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.fogdemo.presentation.components.FogDemo

/**
 * The fogged mirror with you in it: the front camera behind the fog. Like the bathroom's, the
 * steamy room mists it back over by itself.
 */
@Composable
internal fun ReflectionMirror(state: DemoState) {
    FogDemo(state, cameraAccess = rememberCameraAccess(enabled = !state.recording))
}
