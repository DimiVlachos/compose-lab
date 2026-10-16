package dev.dimvlachos.lab.core.presentation.components.snowglobe

import kotlin.math.max
import kotlin.math.min

/**
 * Where the globe sits on a screen [width] wide, between the header ending at [headerBottom] and
 * the day cards starting at [daysTop] (all in pixels): the sphere's centre ([cx], [cy]) and
 * [radius], the screen's [scale] against a 360 px design width, and where the hint under the base
 * goes ([hintTop]). The prototype's `layout()`, number for number.
 */
internal class GlobeLayout(
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val scale: Float,
    val hintTop: Float,
) {
    /** The box the globe and its base occupy, which takes its touches: 2.12 r by 2.42 r. */
    val boxLeft: Float
        get() = cx - 1.06f * radius

    val boxTop: Float
        get() = cy - 1.06f * radius

    val boxWidth: Float
        get() = 2.12f * radius

    val boxHeight: Float
        get() = 2.42f * radius

    /** What the globe rocks about: the middle of its base's foot, 1.3 r under the centre. */
    val pivotY: Float
        get() = cy + 1.3f * radius
}

internal fun globeLayout(width: Float, headerBottom: Float, daysTop: Float): GlobeLayout {
    val s = width / 360f
    val top = headerBottom + 4f * s
    val bottom = daysTop - 46f * s
    val avail = bottom - top
    val r = max(40f, min(width * 0.4f, avail / 2.38f))
    val cy = top + max(0f, avail - 2.36f * r) / 2f + 1.04f * r
    return GlobeLayout(width / 2f, cy, r, s, cy + 1.31f * r + 12f * s)
}
