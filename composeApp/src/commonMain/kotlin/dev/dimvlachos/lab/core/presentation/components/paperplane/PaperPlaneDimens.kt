package dev.dimvlachos.lab.core.presentation.components.paperplane

internal object PaperPlaneDimens {
    // How deep the keel hangs under the wings at the tail, as a share of the folded half's
    // height, and how far each wing opens out from it: a little short of square, so they rise in
    // a shallow V.
    const val KeelShare = 0.42f
    const val WingOpen = 78f

    // Paper has a thickness: the layers of a stack lie this far apart, so the one on top is seen.
    // Every facet counts as a layer of its own, so it is thin: the order is what matters.
    const val Thickness = 0.1f

    // In the air the dart shows its back to the eye, wings spread, a little short of straight
    // overhead so the keel shows under it: near enough overhead that leaning into a turn, which
    // turns it towards side on, still shows its wings. On the button it lies flatter, seen from
    // right above,
    // the outline of the send glyph it lifts off as.
    const val RestRoll = -72f
    const val TakeoffRoll = -84f

    // The eye's distance in front of the plane, in multiples of the density.
    const val Eye = 640f

    // Light from above and to the left, as from a window: a crease turned to it is lit, one
    // turned from it falls into shade.
    const val LightX = -0.55f
    const val LightY = -0.35f
    const val LightZ = 0.76f
    const val MaxShade = 0.55f
    const val MaxLight = 0.3f

    // The inside of the paper, where the folds show it: the colour a little deeper, as the
    // inside of a folded sheet is in its own shade.
    const val InsideShade = 0.12f

    // Paper is not a flat colour: its grain mottles it by up to this share, and its fibres darken
    // it by up to this; a crease or an edge is pressed in this many sheet units wide, this much
    // darker than the paper either side, and this strong.
    const val Grain = 0.07f
    const val Fibre = 0.12f
    const val CreaseWidth = 0.7f
    const val CreaseShade = 0.35f
    const val CreaseAlpha = 0.55f

    // In the air it is never quite steady: it rocks on its length up to this many degrees,
    // yaws up to this many, and lifts and sinks across its path up to this many density units,
    // each at its own pace, so the flutter never quite repeats.
    const val WobbleRoll = 5f
    const val WobbleYaw = 2.5f
    const val WobbleDrift = 3f

    // The space for a message opens over this long, finishing as the plane comes in over it.
    const val OpenMs = 450f

    // How long the dart is in the air, tail to nose, in multiples of the density; it grows to it
    // from the glyph it left as over this long.
    const val PlaneLength = 64f
    const val GrowMs = 420f

    // Thrown, the plane rises off the screen towards the eye, in multiples of the density, and
    // looks the bigger for it; it comes down to skim the conversation as it drops its letters,
    // and climbs a little as it leaves. Its shadow falls on the conversation below, the further
    // off the higher it flies.
    const val FlightLift = 95f
    const val SkimLift = 24f
    const val ExitLift = 40f
    const val ShadowAlpha = 0.26f

    // A sent message's letters go into the send button one after another, the nearest first:
    // each takes this long, lifting this far as it arcs in and tumbling up to a quarter turn,
    // gone to this share of its size; they leave this far apart, closer for a long message, so
    // the whole of it is in within the stream's time.
    const val LetterMs = 200f
    const val LetterGapMs = 25f
    const val StreamMs = 650f
    const val LetterLift = 7f
    const val LetterEndScale = 0.2f
    const val MaxSpin = 90f

    // The send glyph: a dart seen from above, nose to the right, from 2 to 23 of its 24 units
    // across, its middle at (12.5, 12).
    const val IconHeading = 0f
    const val IconLength = 21f / 24f
    const val IconMiddleX = 12.5f / 24f
    const val IconMiddleY = 12f / 24f

    // The throw: thrown hard off the button, at this many density units a millisecond, out along
    // the nose for a short way (a share of the stage's width: the button is by its edge), up the
    // conversation by this share of the room above the button to the top of a loop this far
    // across the stage, level over it for this far, and back in level from the left, this long
    // from the button to where the first letter drops.
    const val ThrowSpeed = 2.4f
    const val LaunchReach = 0.06f
    const val Climb = 0.62f
    const val LoopAcross = 0.4f
    const val LoopSweep = 0.35f
    const val ApproachReach = 0.3f
    const val ShortestApproach = 0.04f
    const val ApproachMs = 1_150f

    // Over the message it flies level, this high over the first line of the text, at this many
    // density units a millisecond, never over less than this far, and fast enough to let the
    // last letter go in time for it to land within the drop's time; then it speeds up out of the
    // right of the stage, this much faster, climbing this share of the stage's width, and gone
    // this many of its own lengths past the edge.
    const val SkimAbove = 30f
    const val Cruise = 0.55f
    const val MinSweep = 24f
    const val DropMs = 620f
    const val ExitBoost = 1.8f
    const val ExitClimb = 0.12f
    const val ExitBeyond = 1.2f

    // Each letter falls from the plane's belly this long, carried on by the plane's speed for
    // this share of the fall's time, so it is let go a little before its place, slowed by the
    // air; it lands at this share of the fall and dips this far, in multiples of the density,
    // before it settles. It starts small as it went into the button, grows to its size, and
    // tumbles out of the turn it had.
    const val FallMs = 220f
    const val CarryShare = 0.3f
    const val LandAt = 0.72f
    const val Settle = 2.5f

    // Leaning into its turns: degrees of bank for each degree of heading the flight swings
    // through over its length, up to a glider's steepest.
    const val BankPerTurn = 0.16f
    const val MaxBank = 28f

    // Cut a pixel past each crease, the facets either side overlap: cut exactly, their smoothed
    // edges would let the background show through in a hairline along every crease.
    const val Seam = 0.8f
}
