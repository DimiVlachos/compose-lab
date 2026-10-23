package dev.dimvlachos.lab.zipperdemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_zipper
import dev.dimvlachos.lab.resources.demo_zipper_description
import dev.dimvlachos.lab.resources.ic_demo_zipper
import dev.dimvlachos.lab.zipperdemo.presentation.components.ZipperDemo
import kotlin.time.Duration.Companion.milliseconds

internal object ZipperDemos {
    /** The finger comes in from below left and takes the pull. */
    const val Grab = "grab"

    /** The finger lets go of the pull. */
    const val Release = "release"

    /** An ordinary flick on the list, which the pull only follows. */
    const val Flick = "flick"

    private const val DragPrefix = "drag:"
    private const val TapPrefix = "tap:"

    /** The control that drags the held pull to [to] of the way down over [ms]. */
    fun drag(to: Float, ms: Int) = "$DragPrefix$to:$ms"

    /** Where [drag] drags to and how long it takes, or null for another control. */
    fun dragged(control: String): Pair<Float, Int>? {
        val rest = control.removePrefix(DragPrefix).takeIf { it != control } ?: return null
        val (to, ms) = rest.split(":").takeIf { it.size == 2 } ?: return null
        return (to.toFloatOrNull() ?: return null) to (ms.toIntOrNull() ?: return null)
    }

    /** The control that taps the tape at [to] of the way down, so the pull glides there. */
    fun tap(to: Float) = "$TapPrefix$to"

    /** Where [tap] taps, or null for another control. */
    fun tapped(control: String): Float? =
        control.removePrefix(TapPrefix).takeIf { it != control }?.toFloatOrNull()

    /** How long the finger takes to reach the tape for a tap, and how long the pull glides. */
    const val TapReachMs = 500
    const val TapGlideMs = 900

    /** How long the finger takes to reach the pull, and to reach the list for the flick. */
    const val ReachMs = 600
    const val FlickReachMs = 500
    const val FlickMs = 260
    const val CoastMs = 700

    // The prototype's clip: the finger takes the pull and holds it while the tab stands and the
    // tag swings out, drags it to the middle, near the end and back up a third, and lets go so
    // the tab clacks flat; then an ordinary flick on the list, which the pull just follows. Then a
    // tap low on the tape glides the pull down there, and a tap at its top glides it home.
    // Everything is put back, so the clip loops.
    private val scroll = demoScript {
        var t = 900.milliseconds
        at(t, lasts = ReachMs.milliseconds) { tapControl(Grab) }
        t += (ReachMs + 550).milliseconds
        for ((to, ms, pause) in Drags) {
            at(t, lasts = ms.milliseconds) { tapControl(drag(to, ms)) }
            t += (ms + pause).milliseconds
        }
        at(t) { tapControl(Release) }
        t += 700.milliseconds
        val flick = FlickReachMs + FlickMs + CoastMs + 300
        at(t, lasts = flick.milliseconds) { tapControl(Flick) }
        t += (flick + 500).milliseconds
        val tapTakes = (TapReachMs + ScriptedTouch.TapHoldMs + TapGlideMs).milliseconds
        at(t, lasts = tapTakes) { tapControl(tap(.85f)) }
        t += tapTakes + 500.milliseconds
        at(t, lasts = tapTakes) { tapControl(tap(0f)) }
        t += tapTakes + 500.milliseconds
        at(t) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "zipper.scrollbar",
                Res.string.demo_zipper,
                scroll,
                description = Res.string.demo_zipper_description,
                icon = Res.drawable.ic_demo_zipper,
                tags = setOf(DemoTag.Gesture, DemoTag.Physics, DemoTag.Canvas),
                tall = true,
            ) {
                ZipperDemo(it)
            },
        )
}

/** The script's drags of the held pull: where to, how long, and the pause after. */
private val Drags = listOf(Triple(.46f, 1700, 800), Triple(.86f, 800, 600), Triple(.3f, 900, 800))
