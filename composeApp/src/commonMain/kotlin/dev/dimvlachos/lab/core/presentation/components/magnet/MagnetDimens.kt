package dev.dimvlachos.lab.core.presentation.components.magnet

import androidx.compose.ui.unit.dp

/** The magnet table's sizes, timings and physics, in dp, seconds and milliseconds. */
internal object MagnetDimens {
    // A photo print on the table: the card, its rounding and the white border round the picture,
    // with a metal clip across its top edge. Cards may lie at most this many radians askew.
    val CardWidth = 76.dp
    val CardHeight = 56.dp
    val CardCorner = 4.dp
    val CardBorder = 3.dp
    val ClipWidth = 16.dp
    val ClipHeight = 7.dp
    const val MostTilt = 0.14f

    // Cards push each other apart as discs this wide across: a little under a card's width, so
    // they overlap at their corners as real prints do.
    val CardSpan = 60.dp

    // A magnet is a puck this wide; a finger within GrabRadius of its middle takes it. At most two
    // are out of the strip at once, and two are kept at least MagnetGap apart.
    val MagnetRadius = 22.dp

    // Drawn as a horseshoe standing over its point: this wide, its bar this thick, the top of its
    // arch this far above the point, its legs reaching down to the point's level and on for their
    // steel tips. The field comes from the point, between its legs.
    val HorseshoeWidth = 44.dp
    val HorseshoeThickness = 12.dp
    val HorseshoeTop = 26.dp
    val HorseshoeLegEnd = 12.dp
    val HorseshoeTip = 9.dp

    // In the strip a magnet hangs by the inside of its arch from a nail this far above its point,
    // and swings there as the lamp's shade swings from the ceiling: this many times a second,
    // losing this share of its swing, never past MostHang. Coming home, it catches on the nail
    // within HookReach of its slot and swings from the way it came: its sideways speed over the
    // nail's drop, times HookShare, and never less than LeastHook either way. A breath of air as
    // the screen opens sets each swinging by about StirSwing.
    val NailDrop = 14.dp
    val NailRadius = 3.5.dp
    const val HangHz = 1.6f
    const val HangDamping = 0.14f
    const val MostHang = 0.6f
    val HookReach = 8.dp
    const val HookShare = 0.35f
    const val LeastHook = 1.2f
    const val StirSwing = 1.4f

    // A third magnet tugged at while two are out jiggles on its nail this hard, and stays.
    const val RefuseSwing = 2.4f
    const val LevelHang = 0.002f
    const val StillHang = 0.01f
    val GrabRadius = 32.dp
    const val MostMagnets = 2
    val MagnetGap = 72.dp

    // The strip the magnets wait in, along the bottom; the table is everything above it.
    val StripHeight = 92.dp

    // Room below a magnet's label in the strip, when large text makes the strip taller.
    val StripPad = 2.dp

    // The pull on a photo: strength × MagnetPull / (distance² + Softening), in dp/s², within Reach.
    // A strong match a card's width from touching reaches the magnet in about a quarter second.
    const val MagnetPull = 6.0e7f
    const val Softening = 1_600f
    val Reach = 260.dp

    // A magnet is a search, so it finds what matches wherever it lies: a strong match out of every
    // magnet's reach is drawn to the nearest one that would hold it at least this hard, in dp/s²,
    // and slides across the table in a second or two.
    const val GatherPull = 2_400f

    // A card whose middle comes within ContactRing of a magnet touches it, and sticks if it
    // matches at least StickThreshold. Stuck, it is held to the magnet but kept ClusterInner from
    // its middle, so the cluster rings the puck.
    val ContactRing = 58.dp
    const val StickThreshold = 0.5f
    val ClusterInner = 30.dp

    // A card a magnet doesn't draw in is nudged aside by its body where it touches, no closer
    // than this to the magnet's point: about the horseshoe's own half-width.
    val MagnetBody = 30.dp

    // Nudged further than this from where it lay, such a card slips under the magnet and its
    // cluster instead, and slides home beneath them: touched, it is bumped aside; swept through,
    // it isn't carried off. Back within HomeAgain of its spot, it can be nudged again.
    val MostNudge = 36.dp
    val HomeAgain = 4.dp

    // The most speed, in dp a second, a nudge leaves such a card with: it is moved, not thrown.
    const val NudgeSpeed = 60f

    // Every loose card is tied to where it lay by a spring this stiff (1/s²) and loses this share
    // of its speed a second: a partial match leans towards a magnet, and everything slides home
    // when the magnets go. A stuck card clings to its place with a stiffer, critically damped
    // spring, which also brings a magnet back into its slot.
    const val Leash = 28f
    const val Friction = 7f
    const val Cling = 260f

    // Let go within SnapRange of the other magnet out, a magnet snaps to its side, Dock apart,
    // touching: the two are joined, one search that holds only what matches both. Apart, they are
    // two searches, each holding its own.
    val SnapRange = 110.dp
    val Dock = 52.dp

    // Moved faster than this a second, a joined magnet tears off its pair, as a yank or a flick
    // home does; slower, it carries the pair with it.
    val TearSpeed = 900.dp

    // Let go, a magnet lands where its speed would carry it in this many seconds: in the strip, it
    // goes back to its slot and drops what is on it.
    const val FlickLead = 0.12f

    // The physics steps at a fixed rate whatever the frame rate; a frame longer than
    // MostFrameSeconds (a stall, a test clock's jump) is stepped as that long. Cards slower than
    // StillSpeed (dp/s) for QuietSteps steps in a row are at rest, and the frame loop sleeps; a
    // magnet within StillGap of its slot is in it.
    const val StepSeconds = 1f / 120f
    const val MostFrameSeconds = 0.05f
    const val StillSpeed = 2f
    const val StillGap = 0.5f
    const val QuietSteps = 30
    const val SeparationPasses = 3

    // At most one haptic tick this many steps apart (about 70 ms), however many cards snap.
    const val TickSteps = 8

    // A match at least this strong snaps with a crisp click; a weaker one with a light tick.
    const val StrongMatch = 0.8f

    // The iron filings: this many, lying this long, rising to RisenLength where the field is
    // full; where it is weaker than LieFlat they keep the angle they fell at. The field is 1 at
    // FieldUnitDistance from a magnet.
    const val FilingCount = 1_600
    val FilingLength = 3.dp
    val RisenLength = 9.dp
    val FilingWidth = 1.dp
    const val LieFlat = 0.02f
    val FieldUnitDistance = 90.dp

    // With every magnet put away the filings settle back as they fell over this long, eased.
    const val CalmSeconds = 1f

    // Tapped, a magnet's photos fan out into a grid of up to FanColumns columns, the cards up to
    // FanScale times their size, in FanMs; FanGap between them.
    const val FanMs = 320
    const val FanColumns = 3
    const val FanScale = 1.5f
    val FanGap = 12.dp

    // Room over the grid for its header, and under each card for its title, just below it.
    val FanHeader = 48.dp
    val FanCaption = 22.dp
    val FanCaptionGap = 4.dp

    // A photo opened from the grid grows out of its cell in OpenMs into a photo OpenMargin in
    // from the table's sides, its corners rounding to OpenCorner, its title and caption under it.
    const val OpenMs = 360
    val OpenMargin = 20.dp
    val OpenCorner = 16.dp
    val OpenTextGap = 12.dp

    // While a magnet is out, the photos no magnet pulls are shaded this much, fading in and out
    // over DimMs, so what the filter finds stands out.
    const val DimShade = 0.45f

    // A photo a magnet out pulls without holding, leaning in within its reach, is shaded only
    // this share as much: partly what was asked for. One out of every magnet's reach is shaded
    // fully, as it isn't being pulled at all.
    const val LeanShade = 0.4f
    const val DimMs = 250

    // A held magnet lifts a little; its label sits under it, and its count badge above.
    const val HeldLift = 1.08f
    val LabelGap = 4.dp
    val BadgeGap = 6.dp
    val BadgePadX = 8.dp
    val BadgePadY = 3.dp
    val BadgeRing = 2.dp
}
