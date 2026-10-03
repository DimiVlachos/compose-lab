package dev.dimvlachos.lab.core.presentation.components.paperplane

internal object PaperPlaneDimens {
    // How deep the keel hangs under the wings at the tail, as a share of the folded half's
    // height, and how far each wing opens out from it: well short of square, so they rise in a
    // clear V.
    const val KeelShare = 0.46f
    const val WingOpen = 65f

    // Creased down the middle, paper springs back: the two halves of the dart stand this many
    // degrees apart about the spine, so the eye sees into the V between them.
    const val KeelSpread = 50f

    // Paper has a thickness: the layers of a stack lie this far apart, so the one on top is seen.
    // Every facet counts as a layer of its own, so it is thin: the order is what matters.
    const val Thickness = 0.1f

    // In the air the dart shows its back to the eye, wings spread, turned well past straight
    // overhead so the V of its wings and the keel under them show. Past overhead, not short of
    // it: the throw loops round anticlockwise, and leaning into that turn rolls it back through
    // overhead towards side on, so from here it leans to as far short of overhead as it started
    // past it, its wings still in view, not edge on. On the button it is seen as below, the send
    // button's dart it lifts off as.
    const val RestRoll = -110f
    const val TakeoffRoll = -36f

    // On the button it is seen close up, three-quarters from behind, nose up and away to the
    // right: pitched this far from the eye, and seen from this many of its own lengths away.
    // Thrown, its nose swings round to its flight over this long, and the eye eases back to its
    // own distance as it grows.
    const val TakeoffPitch = -48f
    const val IconEye = 1f
    const val SwingMs = 180f

    // The eye's distance in front of the plane, in multiples of the density.
    const val Eye = 640f

    // Paper is lit mostly by the room, from every side at once, so it stays white whichever way
    // it turns and falls into shade only where its own folds stand over it; and a little by the
    // window, above and to the left, so a face turned to it is a touch brighter. Turned to the
    // window, its sheen goes this share of the way to white for each share brighter it is lit. The
    // window is this wide seen from the paper (a slope either side of its middle), so the shadow's
    // edge is soft.
    const val LightX = -0.3f
    const val LightY = -0.55f
    const val LightZ = 0.78f
    const val RoomLight = 0.8f
    const val WindowLight = 0.2f
    const val Sheen = 0.6f
    const val WindowSpread = 0.12f

    // The inside of the paper, where the folds show it: the same sheet, so the same colour; its
    // shade is all the light's doing.
    const val InsideShade = 0f

    // Paper is not a flat colour: its grain mottles it by up to this share, and its fibres darken
    // it by up to this; a crease or an edge is pressed in this many sheet units wide, this much
    // darker than the paper either side, and this strong.
    const val Grain = 0.07f
    const val Fibre = 0.12f
    const val CreaseWidth = 0.7f
    const val CreaseShade = 0.35f
    const val CreaseAlpha = 0.06f

    // In the air it is never quite steady: it rocks on its length up to this many degrees,
    // yaws up to this many, and lifts and sinks across its path up to this many density units,
    // each at its own pace, so the flutter never quite repeats.
    const val WobbleRoll = 5f
    const val WobbleYaw = 2.5f
    const val WobbleDrift = 3f

    // The space for a message opens over this long, finishing as the plane comes in over it.
    const val OpenMs = 450f

    // How long the dart is in the air, tail to nose, in multiples of the density; it grows to it
    // from the icon it left as over this long.
    const val PlaneLength = 64f
    const val GrowMs = 420f

    // Thrown, the plane rises off the screen towards the eye, to this many multiples of the
    // density, over this share of the way round to the message, and looks the bigger for it; it
    // glides down the rest of the way to skim the conversation as it drops its letters, and
    // climbs a little as it leaves. Its shadow falls on the conversation below, the further off
    // the higher it flies.
    const val FlightLift = 120f
    const val ClimbShare = 0.35f
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

    // The send button's dart: nose to the right, as long as this share of the icon's width.
    const val IconHeading = -32f
    const val IconLength = 21f / 24f

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

    // Leaning into its turns as a glider does, against this pull of gravity across the stage, in
    // density units a millisecond squared, up to its steepest; it takes this long to roll into a
    // turn and out of it.
    const val Gravity = 0.01f
    const val MaxBank = 55f
    const val RollLagMs = 140f

    // Cut a pixel past each crease, the facets either side overlap: cut exactly, their smoothed
    // edges would let the background show through in a hairline along every crease.
    const val Seam = 0.8f
}
