package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

import kotlin.math.pow
import kotlin.math.sqrt

internal data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)

    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)

    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)

    infix fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z

    fun length() = sqrt(this dot this)

    fun normalized(): Vec3 = length().let { if (it == 0f) this else this * (1f / it) }
}

internal data class Point2(val x: Float, val y: Float)

internal fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t

internal fun mix(a: Vec3, b: Vec3, t: Float) =
    Vec3(mix(a.x, b.x, t), mix(a.y, b.y, t), mix(a.z, b.z, t))

internal fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

internal fun easeInOutCubic(p: Float): Float =
    if (p < 0.5f) 4f * p * p * p else 1f - (-2f * p + 2f).pow(3) / 2f

internal fun easeOutCubic(p: Float): Float = 1f - (1f - p).pow(3)
