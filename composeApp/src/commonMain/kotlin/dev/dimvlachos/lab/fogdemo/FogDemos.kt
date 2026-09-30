package dev.dimvlachos.lab.fogdemo

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
    /** The flat of a finger: what a user wipes with. */
    val FingerBrush = 36.dp

    /** A fingertip: what the script draws with, fine enough for a heart and an arrow. */
    val FingertipBrush = 16.dp

    // Stroke by stroke, with a moment between, as a hand lifts and places the fingertip again.
    private val drawing = heartWithArrow()

    // Fogged glass: a beat, a fingertip draws a heart pierced by an arrow, a moment to look at it,
    // then a breath fogs it all over, back to the fogged glass the loop starts from.
    private val wipeTour = demoScript {
        var start = 0.3.seconds
        for (stroke in drawing) {
            at(start) { wipe(stroke.path, stroke.duration) }
            start += stroke.duration + LiftBetweenStrokes
        }
        at(start + LookAtTheDrawing) { breathe(2.4.seconds, strength = 1f) }
    }

    // On the phone the glass is the user's to breathe on and wipe; Replay plays the clip.
    val all: List<Demo> =
        listOf(
            Demo("fog.mirror", Res.string.demo_fog, wipeTour, autoplay = false) {
                FogDemo(it)
            }
        )
}

private val LiftBetweenStrokes = 250.milliseconds

private val LookAtTheDrawing = 1.5.seconds

/** The demo's glass: fogged all over, waiting for a wipe. */
internal fun newFogDemoState() = FogState()
