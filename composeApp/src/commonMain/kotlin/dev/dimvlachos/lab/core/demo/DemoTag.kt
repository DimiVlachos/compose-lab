package dev.dimvlachos.lab.core.demo

import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.tag_camera
import dev.dimvlachos.lab.resources.tag_canvas
import dev.dimvlachos.lab.resources.tag_gesture
import dev.dimvlachos.lab.resources.tag_motion
import dev.dimvlachos.lab.resources.tag_navigation
import dev.dimvlachos.lab.resources.tag_physics
import dev.dimvlachos.lab.resources.tag_text
import dev.dimvlachos.lab.resources.tag_three_d
import org.jetbrains.compose.resources.StringResource

/** What a demo is about, for filtering the catalog: each demo has one or more. */
internal enum class DemoTag(val label: StringResource) {
    /** Driven by a drag, a pull or a wipe of the finger. */
    Gesture(Res.string.tag_gesture),
    /** Springs, fields and forces: things that move as if they had weight. */
    Physics(Res.string.tag_physics),
    /** Animated transitions between states and screens. */
    Motion(Res.string.tag_motion),
    /** Drawn by hand in a DrawScope. */
    Canvas(Res.string.tag_canvas),
    /** Paper and pages turned and folded in perspective. */
    ThreeD(Res.string.tag_three_d),
    /** Uses the phone's camera. */
    Camera(Res.string.tag_camera),
    /** Plays with the letters themselves. */
    Text(Res.string.tag_text),
    /** Bars, tabs and screens to move between. */
    Navigation(Res.string.tag_navigation),
}
