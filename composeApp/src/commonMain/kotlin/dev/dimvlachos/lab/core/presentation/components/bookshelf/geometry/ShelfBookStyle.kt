package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

import kotlin.math.pow

private const val FALLBACK_CLOTH = 0x3A4A52
private const val CREAM = 0xF6EEDC
private const val GILT = 0xF1D590
private const val CLOTH_MAX_LUMINANCE = 0.08f
private const val PAPER_MIN_LUMINANCE = 0.65f
private const val MIN_CONTRAST = 4.5f

internal enum class Binding {
    Cloth,
    Paper,
}

internal enum class PaperDesign {
    Block,
    Rule,
    Bands,
}

internal enum class Emblem {
    Sun,
    Arch,
    Leaf,
    Wave,
}

internal data class ShelfBookStyle(
    val binding: Binding,
    val base: Int,
    val ink: Int,
    val accent: Int,
    val paperDesign: PaperDesign,
    val emblem: Emblem,
)

internal fun shelfBookStyle(bookId: Int, colorHex: String?): ShelfBookStyle {
    val hash = scramble(bookId)
    val source = colorHex?.let(::parseRgb) ?: FALLBACK_CLOTH
    val binding = if (hash and 1 == 0) Binding.Cloth else Binding.Paper
    val design = PaperDesign.entries[(hash ushr 1) % PaperDesign.entries.size]
    val emblem = Emblem.entries[(hash ushr 3) % Emblem.entries.size]
    return when (binding) {
        Binding.Cloth -> {
            val base = darkenTo(source, CLOTH_MAX_LUMINANCE)
            ShelfBookStyle(binding, base, GILT, mixRgb(source, CREAM, 0.55f), design, emblem)
        }
        Binding.Paper -> {
            val base = lightenTo(mixRgb(source, CREAM, 0.82f), PAPER_MIN_LUMINANCE)
            val ink = darkenUntilContrast(source, base)
            ShelfBookStyle(binding, base, ink, source, design, emblem)
        }
    }
}

internal fun parseRgb(hex: String): Int? {
    val digits = hex.trim().removePrefix("#")
    val rgb =
        when (digits.length) {
            6 -> digits
            8 -> digits.substring(2)
            else -> return null
        }
    return rgb.toIntOrNull(16)
}

internal fun relativeLuminance(rgb: Int): Float {
    fun channel(shift: Int): Float {
        val c = ((rgb shr shift) and 0xFF) / 255f
        return if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }
    return 0.2126f * channel(16) + 0.7152f * channel(8) + 0.0722f * channel(0)
}

internal fun contrastRatio(a: Int, b: Int): Float {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

internal fun mixRgb(a: Int, b: Int, t: Float): Int {
    fun channel(shift: Int) =
        mix(((a shr shift) and 0xFF).toFloat(), ((b shr shift) and 0xFF).toFloat(), t).toInt()
    return (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}

private fun scale(rgb: Int, factor: Float): Int {
    fun channel(shift: Int) = (((rgb shr shift) and 0xFF) * factor).toInt().coerceIn(0, 255)
    return (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}

private fun darkenTo(rgb: Int, maxLuminance: Float): Int =
    generateSequence(rgb) { scale(it, 0.88f) }.first { relativeLuminance(it) <= maxLuminance }

private fun lightenTo(rgb: Int, minLuminance: Float): Int =
    generateSequence(0f) { it + 0.05f }
        .map { mixRgb(rgb, CREAM, it.coerceAtMost(1f)) }
        .first { relativeLuminance(it) >= minLuminance || it == CREAM }

private fun darkenUntilContrast(ink: Int, stock: Int): Int =
    generateSequence(ink) { scale(it, 0.85f) }.first { contrastRatio(it, stock) >= MIN_CONTRAST }

private fun scramble(id: Int): Int = (((id.toLong() * 2654435761L) ushr 13) and 0x7FFF).toInt()
