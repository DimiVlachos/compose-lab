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

/** Whether this demo may listen, and what can be done if not. */
sealed interface MicAccess {
    /** No microphone to ask for: not on this platform, or not while recording. */
    data object Unavailable : MicAccess

    class Granted(val microphone: Microphone) : MicAccess

    /**
     * Not granted, and the system will still ask the user when [ask] is called. A refusal in its
     * dialog hands over a new [Askable], so a demo can tell the answer has come.
     */
    class Askable(val ask: () -> Unit) : MicAccess

    /** Refused for good: the system will not ask again, only [openSettings] can turn it on. */
    class Blocked(val openSettings: () -> Unit) : MicAccess
}

/**
 * Where the microphone stands for this app; it never asks by itself. Disabled, it is always
 * [MicAccess.Unavailable].
 */
@Composable expect fun rememberMicAccess(enabled: Boolean): MicAccess
