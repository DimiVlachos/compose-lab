package dev.dimvlachos.lab.fogdemo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.fogdemo.bathroom.BathroomMirror
import dev.dimvlachos.lab.fogdemo.reflection.ReflectionMirror
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_fog_bathroom
import dev.dimvlachos.lab.resources.demo_fog_reflection
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object FogDemos {
    /** The flat of a finger: what a user wipes with. */
    val FingerBrush = 36.dp

    // The oval the finger scrubs, in fractions of the clip's frame; the brush's width around it
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

    // Fogged glass: a beat, a hand scrubs a porthole clear with the flat of a finger, two drops of
    // condensation run down either side of it while it is looked through, then a breath fogs it
    // all over, back to the fogged glass the loop starts from.
    private val wipeTour = demoScript {
        val start = 0.3.seconds
        at(start) { wipe(porthole.path(), porthole.duration) }
        val scrubbed = start + porthole.duration
        at(scrubbed + 0.1.seconds) { drip(Offset(0.12f, 0.1f), 0.18f) }
        at(scrubbed + 0.6.seconds) { drip(Offset(0.88f, 0.6f), 0.18f) }
        at(scrubbed + LookThrough) { breathe(2.4.seconds, strength = 1f) }
    }

    // On the phone the glass is the user's to breathe on and wipe; Replay plays the clip.
    val bathroom =
        Demo("fog.mirror.bathroom", Res.string.demo_fog_bathroom, wipeTour, autoplay = false) {
            BathroomMirror(it)
        }

    val reflection =
        Demo("fog.mirror.camera", Res.string.demo_fog_reflection, wipeTour, autoplay = false) {
            ReflectionMirror(it)
        }

    val all: List<Demo> = listOf(bathroom, reflection)
}

// Long enough for both drops to finish running before the breath.
private val LookThrough = 5.seconds

/** The demo's glass: fogged all over, waiting for a wipe. */
internal fun newFogDemoState() = FogState()
