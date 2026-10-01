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

    // A drag across this fraction of the open book is a full turn; a finger rarely travels the
    // whole spread, and 1:1 feels heavy.
    const val DragSpan = 0.62f

    // Let go past this much of a turn and it finishes, or flick faster than this many turns per
    // second; anything else falls back.
    const val CommitProgress = 0.42f
    const val CommitVelocity = 1.1f

    // Critically damped, so a released page lands without bouncing off the spine; a cancel is a
    // touch softer, the page sinking back rather than being thrown.
    const val CommitStiffness = 170f
    const val CancelStiffness = 150f

    // A drag that changes direction flips the bow over this much progress, not at once: the paper
    // looks pulled, not snapped.
    const val BendFlipSpan = 0.15f
    const val BendReleaseMs = 300

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
    val ThreadWidth = 1.1.dp
    val ThreadBedWidth = 2.4.dp
    const val ThreadBedAlpha = 0.28f
    val ThreadTwist = 2.6.dp
    const val ThreadTwistAlpha = 0.22f
    val StitchHoleWidth = 1.7.dp
    const val StitchHoleAlpha = 0.55f
}
