package dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn

import androidx.compose.ui.unit.dp

internal object PageTurnDimens {
    const val Strips = 36
    const val MaxWedges = 52

    const val BendMax = 0.6f

    const val BowRest = 0.8f
    const val HoldBow = 0.7f
    const val FlexGain = 0.3f
    const val FlexMax = 0.6f
    const val BowMax = 1.6f
    const val FlexFrequency = 2.4f
    const val FlexDamping = 0.4f
    const val HeldFlexFrequency = 1.6f

    const val SpineLever = 0.3f
    const val EdgeLever = 1.15f
    const val GripPeak = 2.2f
    const val GripSpread = 0.22f

    const val TiltDeadZone = 0.12f
    const val TiltMax = 0.55f
    const val TiltRamp = 0.12f
    const val TiltFrequency = 3f
    const val PullSettle = 0.06f
    const val TurnBack = 0.02f
    const val TiltLimit = 0.65f
    const val TiltRestFade = 0.35f
    const val PinSteps = 8

    const val LiftOff = 0.12f
    const val LiftSwing = 0.3f
    const val PinPerspective = 0.6f

    const val StraightenFrequency = 1.6f

    const val CommitProgress = 0.42f
    const val CommitVelocity = 0.7f
    const val CommitStiffness = 170f
    const val CancelStiffness = 150f

    val Perspective = 1500.dp
    const val PerspectivePerHeight = 5.15f
    const val OriginYFraction = 0.46f

    const val ShadowMax = 0.62f
    const val GlareMax = 0.2f

    const val SteepestDrag = 2.14f

    const val SeamPx = 1.5f

    const val GutterWidthFraction = 0.46f
    val CastShadowBlur = 26.dp
    val CastShadowDrop = 4.dp
    const val CastShadowAlpha = 0.28f
    const val GutterLeftAlpha = 0.3f
    const val GutterRightAlpha = 0.24f

    const val FollowRiffle = 3

    const val RestLift = 1f
    const val GutterSpan = 0.11f
    const val GutterSink = 0.042f
    const val CurlSpan = 0.11f
    const val CurlDrop = 0.014f
    const val SheetFlatten = 0.09f
    const val FlattestShare = 0.1f
    const val EdgeStrips = 6
    const val MaxSheets = 6
    val SheetEdgeWidth = 1.dp
    val SheetFan = 1.dp
    const val SheetShadePerDepth = 0.025f
    const val SheetShadeMax = 0.22f
    const val SheetEdgeAlpha = 0.55f
    const val SettleSpan = 0.5f
}
