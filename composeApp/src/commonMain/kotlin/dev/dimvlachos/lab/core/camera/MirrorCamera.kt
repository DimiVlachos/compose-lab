package dev.dimvlachos.lab.core.camera

import androidx.compose.runtime.Composable

/** The front camera, shown as a mirror. */
internal interface MirrorCamera {
    /**
     * Shows the camera in [mirror] until cancelled, which stops it but keeps its last frame, so a
     * pause never flashes something else. Throws if the camera cannot work at all; a camera that
     * drops out for a moment only stops [showing] until it is back.
     */
    suspend fun run()

    /** The camera's latest frame, upright and mirrored. Paints nothing while not [showing]. */
    val mirror: MirrorFramePainter

    /** Whether [mirror] has a frame to show. */
    val showing: Boolean
}

/** Whether this demo may use the camera, and what can be done if not. */
internal sealed interface CameraAccess {
    /** No camera to ask for: not on this platform, no front camera, or not while recording. */
    data object Unavailable : CameraAccess

    class Granted(val camera: MirrorCamera) : CameraAccess

    /**
     * Not granted, and the system will still ask the user when [ask] is called. A refusal in its
     * dialog hands over a new [Askable], so a demo can tell the answer has come.
     */
    class Askable(val ask: () -> Unit) : CameraAccess

    /** Refused for good: the system will not ask again, only [openSettings] can turn it on. */
    class Blocked(val openSettings: () -> Unit) : CameraAccess
}

/**
 * Where the front camera stands for this app; it never asks by itself. Disabled, it is always
 * [CameraAccess.Unavailable].
 */
@Composable internal expect fun rememberCameraAccess(enabled: Boolean): CameraAccess
