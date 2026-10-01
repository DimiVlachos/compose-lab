package dev.dimvlachos.lab.core.presentation.components.pageturn

import androidx.compose.ui.unit.dp

internal object PageTurnDimens {
    // The leaf is cut into this many strips, each hinged on the one before it; with a bend spread
    // over the chain the leaf curls instead of turning as a flat card. Fewer shows facets on the
    // bow, more buys nothing a phone can see.
    const val Strips = 18

    // The bow at mid-turn, in radians shared across the whole chain: sin(pi t) puts it at its
    // deepest half way and lets the leaf land flat at both ends.
    const val BendMax = 0.6f

    // The paper's bow is a spring. Flying free, a leaf keeps BowRest of BendMax with its edge
    // trailing; in a hand, HoldBow with the hand leading. Moving, the air pushes it back FlexGain
    // more for every turn a second, up to FlexMax more, never past BowMax. It rings at
    // FlexFrequency, damped by FlexDamping: let go, it whips over from one to the other, and a
    // stop lets it settle with a sway.
    const val BowRest = 0.8f
    const val HoldBow = 0.7f
    const val FlexGain = 0.3f
    const val FlexMax = 0.6f
    const val BowMax = 1.6f
    const val FlexFrequency = 2.4f
    const val FlexDamping = 0.4f
    // In hand, the bow follows the finger calmly, without overshoot, at this rate.
    const val HeldFlexFrequency = 1.6f

    // Where the hand holds a leaf bends it: SpineLever of the bow held next to the spine, where
    // there is little lever, EdgeLever at the outer edge. Held, the bow gathers GripPeak times
    // thicker around the fingers, over GripSpread of the leaf either side.
    const val SpineLever = 0.3f
    const val EdgeLever = 1.15f
    const val GripPeak = 2.2f
    const val GripSpread = 0.22f

    // A leaf held nearer a corner than TiltDeadZone (of its height from the middle) leans its
    // rulings, up to TiltMax at the corner, so that corner peels first; the lean grows in over
    // TiltRamp of the turn, and the leaf leans after it as a spring at TiltFrequency. Never past
    // TiltLimit, where the rulings would meet on the page.
    const val TiltDeadZone = 0.12f
    const val TiltMax = 0.55f
    const val TiltRamp = 0.12f
    const val TiltFrequency = 3f
    // A pull at a slant leans the rulings square to it once it has gone this share of a page.
    const val PullSettle = 0.06f
    // The finger's recent way, which a pull's slant is read from, is its last PullWindow of a page.
    const val PullWindow = 0.1f
    // A held leaf's bow swings round only once the finger has come back this share of a page.
    const val TurnBack = 0.02f
    const val TiltLimit = 0.65f
    // Leaning this far, a peeled leaf has lost the rest curve the binding gave it.
    const val TiltRestFade = 0.35f
    const val PinSteps = 8

    // A held point is placed this share of the way from where it lies, seen from straight above,
    // to where the eye sees it, so it stays under the fingertip without snapping up (see pinLeaf).
    const val PinPerspective = 0.6f

    // Let go, the lean and the hand's shape ease out as the leaf flies, so it lands square.
    const val StraightenFrequency = 1.6f

    // Let go past this much of a turn and it finishes, or flick faster than this many turns per
    // second; anything else falls back.
    const val CommitProgress = 0.42f
    const val CommitVelocity = 0.7f

    // Critically damped, so a released page lands without bouncing off the spine; a cancel is a
    // touch softer, the page sinking back rather than being thrown.
    const val CommitStiffness = 170f
    const val CancelStiffness = 150f

    // The camera distance of the 3D turn, and the height it looks from: a little above centre, so
    // the lifted leaf grows more at the bottom, the way a book on a table looks.
    val Perspective = 1500.dp
    const val OriginYFraction = 0.46f

    // Light: a strip facing away from the reader darkens up to ShadowMax; the glare is the sheen
    // on a strip facing it square on, only while the page is up.
    const val ShadowMax = 0.62f
    const val GlareMax = 0.2f

    // The open book at rest: each page stands this high at its crest, as a fraction of its width,
    // and ends this much lower at its outer edge (as a fraction of RestLift). A gentle bow.
    const val RestLift = 0.2f
    const val RestFall = 0.6f

    // The paper falling into the spine darkens by up to this much, over this share of the page.
    const val GutterRestAlpha = 0.16f
    const val GutterRestSpan = 0.42f

    // And past the crest, where the page curls down onto its stack, a lighter shade over this much.
    const val EdgeRestAlpha = 0.12f
    const val EdgeRestSpan = 0.16f

    // The leaves under each page: every sheet down a stack arches SheetFlatten of RestLift less
    // than
    // the one above it, down to FlattestShare of it, so their edges fan out past each other; the
    // fan needs StackFraction of a page's width beside the pages. A sheet's edge is drawn this
    // wide,
    // and only its outer EdgeStrips strips are drawn, all that shows past the sheet above it.
    const val SheetFlatten = 0.09f
    const val FlattestShare = 0.1f
    const val StackFraction = 0.035f
    const val EdgeStrips = 3
    val SheetEdgeWidth = 1.dp

    // A sheet in the stack darkens this much more for each sheet above it, up to SheetShadeMax.
    const val SheetShadePerDepth = 0.025f
    const val SheetShadeMax = 0.22f
    const val SheetEdgeAlpha = 0.55f

    // While a leaf turns, the stack it leaves rises over the first SettleSpan of the turn, and the
    // one it lands on settles under it over the last.
    const val SettleSpan = 0.5f

    // Room left round the pages, as a share of a page's width: the arch lifts their edges a little
    // past the flat page.
    const val EdgeRoomFraction = 0.015f

    val TapSlop = 6.dp

    // A drag takes the page if it sets off no steeper than this, rise over run: tan 65 degrees, so
    // a corner pulled away on a slant turns it, and only a nearly upright drag is left alone.
    const val SteepestDrag = 2.14f

    // Strips overlap by this much so no hairline of the page under them shows between two of
    // them; the last strip has nothing to overlap.
    const val SeamPx = 1.5f

    // The outer corners of the pages, as a fraction of each side.
    const val CornerFraction = 0.045f

    // The shade in the gutter while a leaf is up, on each page, as a fraction of a page's width.
    const val GutterWidthFraction = 0.46f
    const val GutterLeftAlpha = 0.3f
    const val GutterRightAlpha = 0.24f

    // The crease down the spine, as a fraction of the book's width either side of it.
    const val CreaseWidthFraction = 0.05f
    const val CreaseAlpha = 0.07f

    // The book is sewn through its centre fold: the thread goes in and out of four holes, at these
    // heights, so inside the fold it shows between the first two and between the last two. The
    // thread is this thick, lying in a soft shadow of the fold this wide, its twist marked this far
    // apart, and it enters the paper through holes this wide.
    val StitchHoles = floatArrayOf(0.12f, 0.38f, 0.62f, 0.88f)

    // A leaf of the centre fold closes or opens it as its spine strip passes upright: the thread
    // fades while that strip is within this much (as the cosine of its angle) of upright.
    const val FoldFadeCos = 0.2f

    val ThreadWidth = 1.1.dp
    val ThreadBedWidth = 2.4.dp
    const val ThreadBedAlpha = 0.28f
    val ThreadTwist = 2.6.dp
    const val ThreadTwistAlpha = 0.22f
    val StitchHoleWidth = 1.7.dp
    const val StitchHoleAlpha = 0.55f
}
