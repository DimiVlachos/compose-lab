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
    val Perspective = 1750.dp
    const val OriginYFraction = 0.46f

    // Light: a strip facing away from the reader darkens up to ShadowMax; the glare is the sheen
    // on a strip facing it square on, only while the page is up.
    const val ShadowMax = 0.62f
    const val GlareMax = 0.2f

    // The open book at rest: each page stands this high at its crest, as a fraction of its width,
    // and ends this much lower at its outer edge (as a fraction of RestLift). A gentle bow.
    const val RestLift = 0.06f
    const val RestFall = 0.6f

    // The paper falling into the spine darkens by up to this much, over this share of the page.
    const val GutterRestAlpha = 0.3f
    const val GutterRestSpan = 0.3f

    // And past the crest, where the page curls down onto its stack, a lighter shade over this much.
    const val EdgeRestAlpha = 0.12f
    const val EdgeRestSpan = 0.16f

    // The page stacks at the outer edges: the thickest is this share of a page's width, the
    // thinnest this share of the thickest; the paper edges show as lines this far apart.
    const val StackFraction = 0.045f
    const val StackMinShare = 0.35f
    val StackLineSpacing = 1.4.dp

    // The hardcover boards: how far they reach past the pages and their stacks, as fractions of a
    // page's width, and their rounding; the gold line inset on them.
    const val CoverMarginXFraction = 0.024f
    const val CoverMarginYFraction = 0.034f
    const val CoverCornerFraction = 0.014f
    const val CoverRuleInsetFraction = 0.4f
    val CoverRuleWidth = 1.dp

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
    const val CreaseWidthFraction = 0.04f
    const val CreaseAlpha = 0.14f

    // The binding stitches, at these heights down the spine.
    val StitchFractions = floatArrayOf(0.12f, 0.5f, 0.88f)
    const val StitchLengthFraction = 0.055f
    val StitchWidth = 1.8.dp
    val StitchUnderWidth = 3.2.dp
    val StitchUnderExtra = 3.dp
    const val StitchShadowAlpha = 0.25f

    // The book resting on a table: a wide soft shadow falling towards the reader, a closer one and
    // a thin dark line where the pages meet the table. Each shrinks under the book (negative
    // spread) so it shows below it, not around it.
    val SoftShadowRadius = 28.dp
    val SoftShadowSpread = (-10).dp
    val SoftShadowOffset = 16.dp
    const val SoftShadowAlpha = 0.3f
    val MidShadowRadius = 10.dp
    val MidShadowSpread = (-4).dp
    val MidShadowOffset = 6.dp
    const val MidShadowAlpha = 0.32f
    val ContactShadowRadius = 3.dp
    val ContactShadowSpread = (-1).dp
    val ContactShadowOffset = 2.dp
    const val ContactShadowAlpha = 0.35f

    // The stitches sit on the pages, under a turning leaf; they fade from under it to over it over
    // the first and last stretch of the turn, so the leaf never pops in front of them.
    const val StitchFadeSpan = 0.15f
}
