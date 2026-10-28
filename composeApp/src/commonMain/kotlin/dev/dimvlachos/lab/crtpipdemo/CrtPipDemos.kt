package dev.dimvlachos.lab.crtpipdemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.crtpipdemo.presentation.components.CrtPipDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_crtpip
import dev.dimvlachos.lab.resources.demo_crtpip_description
import dev.dimvlachos.lab.resources.ic_demo_crtpip
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object CrtPipDemos {
    // The script's moves, by name.
    const val SwipeDown = "swipe-down"
    const val BrowseDown = "browse-down"
    const val BrowseUp = "browse-up"
    const val SlideSet = "slide-set"
    const val TapSet = "tap-set"

    // How long each move takes, as the prototype's demo moves its finger.
    val SwipeTakes = 1580.milliseconds
    val BrowseDownTakes = 2400.milliseconds
    val BrowseUpTakes = 1150.milliseconds
    val SlideTakes = 1620.milliseconds
    val TapTakes = 760.milliseconds

    // The prototype's tour: swipe the video down so it switches off into the set; browse the page
    // while the show plays in the corner, then come back to the top; slide the set over to the
    // other corner, letting go a little short so the spring shows; tap it and it flies back into
    // the full player. The next minimize goes to the usual corner again, so the clip loops.
    private val minimize = demoScript {
        var t = 1.4.seconds
        at(t, lasts = SwipeTakes) { tapControl(SwipeDown) }
        t += SwipeTakes
        at(t, lasts = BrowseDownTakes) { tapControl(BrowseDown) }
        t += BrowseDownTakes + 900.milliseconds
        at(t, lasts = BrowseUpTakes) { tapControl(BrowseUp) }
        t += BrowseUpTakes + 700.milliseconds
        at(t, lasts = SlideTakes) { tapControl(SlideSet) }
        t += SlideTakes + 700.milliseconds
        at(t, lasts = TapTakes) { tapControl(TapSet) }
        t += TapTakes + 1470.milliseconds
        at(t) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "crtpip.minimize",
                Res.string.demo_crtpip,
                minimize,
                description = Res.string.demo_crtpip_description,
                icon = Res.drawable.ic_demo_crtpip,
                tags = setOf(DemoTag.Canvas, DemoTag.Motion, DemoTag.Gesture, DemoTag.Physics),
                tall = true,
            ) {
                CrtPipDemo(it)
            },
        )
}
