package dev.dimvlachos.lab.frostdemo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.frostdemo.presentation.components.FrostDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_frost
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object FrostDemos {
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

    private val wipeTour = demoScript {
        at(0.seconds) { select(1) }
        at(0.4.seconds) { wipe(porthole.path(), porthole.duration) }
        at(8.6.seconds) { select(0) }
    }

    val all: List<Demo> =
        listOf(Demo("frost.window", Res.string.demo_frost, wipeTour) { FrostDemo(it) })
}
