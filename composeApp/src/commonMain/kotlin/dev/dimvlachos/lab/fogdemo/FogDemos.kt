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

    /** A fingertip: what the script draws with, fine enough for a heart and an arrow. */
    val FingertipBrush = 16.dp

    // Stroke by stroke, with a moment between, as a hand lifts and places the fingertip again.
    private val drawing = heartWithArrow()

    // Fogged glass: a beat, a fingertip draws a heart pierced by an arrow, two drops of
    // condensation run down either side of it while it is looked at, then a breath fogs it all
    // over, back to the fogged glass the loop starts from.
    private val wipeTour = demoScript {
        var start = 0.3.seconds
        for (stroke in drawing) {
            at(start) { wipe(stroke.path, stroke.duration) }
            start += stroke.duration + LiftBetweenStrokes
        }
        at(start + 0.1.seconds) { drip(Offset(0.12f, 0.1f), 0.18f) }
        at(start + 0.6.seconds) { drip(Offset(0.88f, 0.6f), 0.18f) }
        at(start + LookAtTheDrawing) { breathe(2.4.seconds, strength = 1f) }
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

private val LiftBetweenStrokes = 250.milliseconds

// Long enough for both drops to finish running before the breath.
private val LookAtTheDrawing = 5.seconds

/** The demo's glass: fogged all over, waiting for a wipe. */
internal fun newFogDemoState() = FogState()
