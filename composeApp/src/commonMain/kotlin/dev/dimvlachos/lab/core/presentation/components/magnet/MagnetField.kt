package dev.dimvlachos.lab.core.presentation.components.magnet

import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.physics.Pendulum
import kotlin.math.sqrt

/**
 * A tag's magnet, in dp from the table's top left: hanging from its nail at its [slot] in the
 * strip, [held] by a finger, or put down [onTable]. Held or down, it is [out] and pulls, unless it
 * is [parting]. In the strip it [hang]s from the nail by its arch and swings there.
 */
internal class Magnet(val tag: String) {
    var at = Offset.Zero
    var slot = Offset.Zero
    var velocity = Offset.Zero
    var held = false
    var onTable = false

    /** How it swings on its nail. */
    val hang = Pendulum(MagnetDimens.HangHz, MagnetDimens.HangDamping, MagnetDimens.MostHang)

    /** Whether, on its way home, it has caught on its nail yet. */
    var hooked = true

    /**
     * Whether it has been torn off a pair and is still in the hand: on its way somewhere, not a
     * search yet, so nothing comes to it until it is put down.
     */
    var parting = false

    val out: Boolean
        get() = held || onTable
}

/**
 * The magnets' pull on the photos and the field the filings follow, in dp and seconds. A photo
 * feels each magnet pull it straight towards it, harder the better it matches and the closer it is,
 * out to the magnet's reach; two magnets' pulls add. The field falls off the same way but points
 * away from each magnet, as round a pole facing down.
 */
internal object MagnetField {
    /**
     * The acceleration, in dp/s², a magnet at [magnet] gives a photo at [at] that matches its tag
     * with [strength]: `strength × MagnetPull / (distance² + Softening)` within reach, else none.
     */
    fun pull(at: Offset, magnet: Offset, strength: Float): Offset {
        if (strength <= 0f) return Offset.Zero
        val dx = magnet.x - at.x
        val dy = magnet.y - at.y
        val d2 = dx * dx + dy * dy
        val reach = MagnetDimens.Reach.value
        if (d2 > reach * reach) return Offset.Zero
        val d = sqrt(d2)
        // Right on the magnet there is no direction to be pulled in, and no NaN either.
        if (d < Epsilon) return Offset.Zero
        val a = strength * MagnetDimens.MagnetPull / (d2 + MagnetDimens.Softening)
        return Offset(dx / d * a, dy / d * a)
    }

    /**
     * The field at ([x], [y]) from the first [count] magnets at ([mx], [my]), written into [out] as
     * its x and y. It allocates nothing, so it can run for every filing every frame.
     */
    fun fieldAt(x: Float, y: Float, mx: FloatArray, my: FloatArray, count: Int, out: FloatArray) {
        var fx = 0f
        var fy = 0f
        for (i in 0 until count) {
            val dx = x - mx[i]
            val dy = y - my[i]
            val d2 = dx * dx + dy * dy
            val d = sqrt(d2)
            if (d < Epsilon) continue
            val b = FieldUnit / (d2 + MagnetDimens.Softening)
            fx += dx / d * b
            fy += dy / d * b
        }
        out[0] = fx
        out[1] = fy
    }

    private const val Epsilon = 1e-3f

    // So the field is exactly 1 at FieldUnitDistance from a magnet.
    private val FieldUnit =
        MagnetDimens.FieldUnitDistance.value * MagnetDimens.FieldUnitDistance.value +
            MagnetDimens.Softening
}
