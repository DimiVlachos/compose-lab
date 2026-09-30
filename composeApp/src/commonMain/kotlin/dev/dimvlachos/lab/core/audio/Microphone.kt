package dev.dimvlachos.lab.core.audio

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

/**
 * A microphone as a cold stream of [MicFrameSize]-sample mono frames at [MicSampleRate], in -1..1:
 * collecting starts recording, cancelling stops it. The stream fails if the microphone cannot start
 * or stops working.
 */
interface Microphone {
    val frames: Flow<FloatArray>
}

/** Whether this demo may listen. */
sealed interface MicAccess {
    /** Still asking the user. */
    data object Pending : MicAccess

    /** Denied, not asked (recording), or no microphone on this platform. */
    data object Unavailable : MicAccess

    class Granted(val microphone: Microphone) : MicAccess
}

/**
 * Asks for the microphone the first time it is composed with [enabled], and returns where that
 * stands. Disabled, it never asks and returns [MicAccess.Unavailable].
 */
@Composable expect fun rememberMicAccess(enabled: Boolean): MicAccess
