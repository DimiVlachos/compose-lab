package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

internal data class Rgb(val r: Float, val g: Float, val b: Float) {
    operator fun plus(o: Rgb) = Rgb(r + o.r, g + o.g, b + o.b)

    operator fun times(s: Float) = Rgb(r * s, g * s, b * s)

    fun clamped() = Rgb(r.coerceIn(0f, 255f), g.coerceIn(0f, 255f), b.coerceIn(0f, 255f))

    fun toArgb(alpha: Float = 1f): Int {
        val c = clamped()
        return ((alpha.coerceIn(0f, 1f) * 255f).toInt() shl 24) or
            (c.r.toInt() shl 16) or
            (c.g.toInt() shl 8) or
            c.b.toInt()
    }

    companion object {
        fun of(rgb: Int) =
            Rgb(
                ((rgb shr 16) and 0xFF).toFloat(),
                ((rgb shr 8) and 0xFF).toFloat(),
                (rgb and 0xFF).toFloat(),
            )
    }
}

internal data class ShelfLighting(
    val read: Rgb,
    val ambient: Rgb,
    val sun: Rgb,
    val glow: Rgb,
    val sunPatch: List<Point2>,
    val wall: Rgb,
    val oak: Rgb,
    val dim: Rgb,
    val dimAlpha: Float,
    val pebble: Rgb,
) {
    fun lambert(normal: Vec3): Rgb {
        val d = (normal dot SUN_DIRECTION).coerceAtLeast(0f)
        return (ambient + sun * d).clamped()
    }

    fun faceLight(normal: Vec3, held: Float): Rgb {
        val readDot = (normal.x * -0.3f + normal.y * -0.4f + normal.z * 0.87f).coerceAtLeast(0f)
        val reading = read * (0.7f + 0.3f * readDot)
        val lit = lambert(normal)
        return Rgb(
            mix(lit.r, reading.r, held),
            mix(lit.g, reading.g, held),
            mix(lit.b, reading.b, held),
        )
    }

    companion object {
        val SUN_DIRECTION = Vec3(-0.5f, -0.62f, 0.6f).normalized()

        val Day =
            ShelfLighting(
                read = Rgb(255f, 250f, 240f),
                ambient = Rgb(150f, 138f, 138f),
                sun = Rgb(178f, 168f, 148f),
                glow = Rgb(26f, 12f, 0f),
                sunPatch =
                    listOf(
                        Point2(0.04f, 0.13f),
                        Point2(1.3f, 0.08f),
                        Point2(1.3f, 0.8f),
                        Point2(-0.16f, 0.8f),
                    ),
                wall = Rgb(240f, 230f, 214f),
                oak = Rgb(198f, 150f, 102f),
                dim = Rgb(26f, 18f, 12f),
                dimAlpha = 0.5f,
                pebble = Rgb(186f, 180f, 172f),
            )

        val Dusk =
            ShelfLighting(
                read = Rgb(232f, 222f, 206f),
                ambient = Rgb(50f, 44f, 70f),
                sun = Rgb(306f, 208f, 104f),
                glow = Rgb(44f, 22f, 0f),
                sunPatch =
                    listOf(
                        Point2(0.12f, 0.19f),
                        Point2(1.3f, 0.13f),
                        Point2(1.3f, 0.72f),
                        Point2(-0.06f, 0.72f),
                    ),
                wall = Rgb(220f, 200f, 180f),
                oak = Rgb(182f, 130f, 84f),
                dim = Rgb(8f, 6f, 12f),
                dimAlpha = 0.62f,
                pebble = Rgb(176f, 168f, 160f),
            )
    }
}
