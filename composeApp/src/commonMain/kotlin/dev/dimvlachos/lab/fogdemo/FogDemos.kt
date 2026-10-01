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
    val PortholeCentre = Offset(0.5f, 0.46f)
    val PortholeRadii = Offset(0.4f, 0.24f)

    // A hand clearing a porthole to look through: sixteen quick sweeps from the top of the oval to
    // the bottom, none quite like the last. Sixteen, and bows this shallow, so the rows overlap
    // everywhere, up into the oval's narrow top and, the last sweep coming in from the right,
    // down into both its lower corners.
    val porthole =
        ovalScrub(
            PortholeCentre,
            PortholeRadii,
            feel =
                listOf(
                    SweepFeel(bow = 0.004f, duration = 280.milliseconds),
                    SweepFeel(bow = 0.005f, duration = 300.milliseconds, reach = 0.97f),
                    SweepFeel(bow = 0.004f, duration = 320.milliseconds, reach = 1.03f),
                    SweepFeel(bow = 0.006f, duration = 340.milliseconds),
                    SweepFeel(bow = 0.005f, duration = 350.milliseconds, reach = 0.98f),
                    SweepFeel(bow = 0.005f, duration = 360.milliseconds, reach = 1.02f),
                    SweepFeel(bow = 0.004f, duration = 370.milliseconds, reach = 0.99f),
                    SweepFeel(bow = 0.004f, duration = 380.milliseconds, reach = 0.98f),
                    SweepFeel(bow = 0.006f, duration = 370.milliseconds, reach = 1.02f),
                    SweepFeel(bow = 0.005f, duration = 370.milliseconds, reach = 0.99f),
                    SweepFeel(bow = 0.004f, duration = 360.milliseconds, reach = 1.01f),
                    SweepFeel(bow = 0.005f, duration = 350.milliseconds, reach = 0.98f),
                    SweepFeel(bow = 0.006f, duration = 340.milliseconds, reach = 1.02f),
                    SweepFeel(bow = 0.004f, duration = 320.milliseconds, reach = 0.99f),
                    SweepFeel(bow = 0.005f, duration = 300.milliseconds),
                    SweepFeel(bow = 0.004f, duration = 280.milliseconds),
                ),
        )

    // Fogged glass: a beat, a hand scrubs a porthole clear with the flat of a finger, two drops of
    // condensation run down in the corners beside it while it is looked through, then a breath
    // fogs it all over, back to the fogged glass the loop starts from.
    private val wipeTour = demoScript {
        val start = 0.3.seconds
        at(start) { wipe(porthole.path(), porthole.duration) }
        val scrubbed = start + porthole.duration
        at(scrubbed + 0.1.seconds) { drip(Offset(0.06f, 0.04f), 0.12f) }
        at(scrubbed + 0.6.seconds) { drip(Offset(0.94f, 0.76f), 0.14f) }
        at(scrubbed + LookThrough, lasts = ClosingBreath) { breathe(ClosingBreath, strength = 1f) }
    }

    // The steamy bathroom, left to itself: drops of condensation gather and run down around the
    // glass, a hand scrubs a porthole clear between them, smearing away the one in its path, one
    // more drop runs into it and spreads away on the wet glass, then the room mists it all back
    // over, to the fogged glass the loop starts from.
    private val bathroomTour = demoScript {
        at(0.3.seconds) { drip(Offset(0.06f, 0.04f), 0.12f) }
        at(0.8.seconds) { drip(Offset(0.94f, 0.76f), 0.12f) }
        at(1.2.seconds) { drip(Offset(0.22f, 0.84f), 0.08f) }
        // One settles where the hand will pass, to be smeared away by it.
        at(1.6.seconds) { drip(Offset(0.62f, 0.3f), 0.1f) }
        at(WipeAfterDrops) { wipe(porthole.path(), porthole.duration) }
        val scrubbed = WipeAfterDrops + porthole.duration
        at(scrubbed + 0.3.seconds) { drip(Offset(0.35f, 0.1f), 0.2f) }
        at(scrubbed + SpreadAway, lasts = MistOver) { mist(MistOver) }
    }

    // On the phone the glass is the user's to wipe; Replay plays the clip.
    val bathroom =
        Demo("fog.mirror.bathroom", Res.string.demo_fog_bathroom, bathroomTour, autoplay = false) {
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

// Long enough for the drop to run into the porthole and spread away, before the mist.
private val SpreadAway = 7.5.seconds

// Long enough to watch the first drops run before the hand comes.
private val WipeAfterDrops = 5.seconds

// The camera clip's closing breath.
private val ClosingBreath = 2.4.seconds

// The room's mist, quickened for the clip: on the phone it takes about 25 s.
private val MistOver = 5.seconds

/** The demo's glass: fogged all over, waiting for a wipe. */
internal fun newFogDemoState() = FogState()
