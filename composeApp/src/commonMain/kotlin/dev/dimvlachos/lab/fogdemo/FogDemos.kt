package dev.dimvlachos.lab.fogdemo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.fogdemo.presentation.components.FogDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_fog
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object FogDemos {
    /** The flat of a finger, wide enough for the scrub's rows to run together. */
    val ScrubBrush = 36.dp

    // The oval the fingertip traces, in fractions of the clip's frame; the brush's width around it
    // makes the clear oval.
    val PortholeCentre = Offset(0.5f, 0.44f)
    val PortholeRadii = Offset(0.33f, 0.18f)

    // A hand clearing a porthole to look through: thirteen quick sweeps from the top of the oval to
    // the bottom, none quite like the last. Thirteen, and bows this shallow, so the rows overlap
    // everywhere, down into the oval's lower corners.
    val porthole =
        ovalScrub(
            PortholeCentre,
            PortholeRadii,
            feel =
                listOf(
                    SweepFeel(bow = 0.004f, duration = 350.milliseconds),
                    SweepFeel(bow = 0.005f, duration = 400.milliseconds, reach = 0.97f),
                    SweepFeel(bow = 0.004f, duration = 430.milliseconds, reach = 1.03f),
                    SweepFeel(bow = 0.006f, duration = 450.milliseconds),
                    SweepFeel(bow = 0.005f, duration = 470.milliseconds, reach = 0.98f),
                    SweepFeel(bow = 0.005f, duration = 470.milliseconds, reach = 1.02f),
                    SweepFeel(bow = 0.004f, duration = 480.milliseconds, reach = 0.99f),
                    SweepFeel(bow = 0.005f, duration = 470.milliseconds, reach = 1.01f),
                    SweepFeel(bow = 0.004f, duration = 470.milliseconds, reach = 0.98f),
                    SweepFeel(bow = 0.006f, duration = 450.milliseconds, reach = 1.02f),
                    SweepFeel(bow = 0.005f, duration = 430.milliseconds, reach = 0.99f),
                    SweepFeel(bow = 0.004f, duration = 400.milliseconds),
                    SweepFeel(bow = 0.004f, duration = 350.milliseconds),
                ),
        )

    // Clear glass: a breath fogs it from the bottom, the hand scrubs a porthole, a moment to look
    // through it, then the fog evaporates, back to the clear glass the loop starts from.
    private val wipeTour = demoScript {
        at(0.seconds) { select(1) }
        at(0.4.seconds) { breathe(2.4.seconds, strength = 1f) }
        at(3.2.seconds) { wipe(porthole.path(), porthole.duration) }
        at(10.8.seconds) { select(0) }
    }

    // On the phone the glass is the user's to breathe on and wipe; Replay plays the clip.
    val all: List<Demo> =
        listOf(
            Demo("fog.window", Res.string.demo_fog, wipeTour, autoplay = false) {
                FogDemo(it)
            }
        )
}

/** The demo's window: clear glass, waiting for a breath. */
internal fun newFogDemoState() = FogState(startClear = true)
