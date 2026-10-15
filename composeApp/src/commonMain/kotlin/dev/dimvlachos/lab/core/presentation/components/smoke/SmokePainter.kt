package dev.dimvlachos.lab.core.presentation.components.smoke

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import kotlin.math.pow

/**
 * The smoke's four layers, drawn bottom to top: a wide blurred [Haze] of the oldest smoke, a
 * lightly blurred [Soft] body, the [Crisp] young particles that still carry the words' shapes, and
 * the [Glow] of the embers and sparks, added to what is under it on a dark screen.
 */
internal enum class SmokeLayer {
    Haze,
    Soft,
    Crisp,
    Glow,
}

/** The colours the smoke is drawn in, from the theme. */
internal class SmokeColors(
    val smoke: Color,
    val mist: Color,
    val ember: Color,
    val spark: Color,
    /** Whether the embers glow by adding light, as they do on a dark screen. */
    val glowAdds: Boolean,
)

/**
 * Works out where every particle of a plume is ([fill]) and draws them by layer ([draw]): each
 * batch of one colour at one age in a single call of points, its size, colour and opacity those of
 * a particle in the middle of that age.
 */
internal class SmokePainter {
    private val paint = Paint().apply { strokeCap = StrokeCap.Round }

    // Scratch for [fill], which runs one frame's job at a time.
    private val place = FloatArray(2)
    private val scratch = FloatArray(2)

    /**
     * Sorts [plume]'s particles at [now] into [batches], at [density] px per dp, [reform] of the
     * way back in since [reformAt] (or -1). It reads only the plume's particles and its front's
     * crossing times, so it can run off the main thread while the last frame's batches are drawn,
     * one plume after another: never two at once, as they share their scratch.
     */
    fun fill(
        plume: SmokePlume,
        batches: PlumeBatches,
        now: Float,
        reform: Float,
        reformAt: Float,
        density: Float,
    ) {
        val particles = plume.particles
        if (particles == null) {
            batches.clear(0)
            return
        }
        val colours = particles.palette.size
        batches.clear(colours)
        val front = plume.front
        val material = plume.material
        // Breathed back in, the smoke is eased over to where the row is now, at its size now.
        val pull = if (reform >= 0f) smoothstep(0f, 1f, reform) else 0f
        val sx = if (plume.width > 0f) plume.atWidth / plume.width else 1f
        val sy = if (plume.height > 0f) plume.atHeight / plume.height else 1f
        for (i in 0 until particles.count) {
            val column = particles.column[i].toInt()
            val cross = front.crossing(column)
            if (cross == BurnFront.Uncrossed) continue
            val x = particles.x[i]
            val y = particles.y[i]
            if (!front.showed(column, y)) continue
            val seed = particles.seed[i]
            val ink = particles.isInk(i)
            val life = SmokeMotion.life(seed, ink, material)
            val age =
                SmokeMotion.ageAt(now, cross, SmokeMotion.jitter(seed), life, reform, reformAt)
            if (age < 0f || age >= life) continue
            // It left from where the row was as the front crossed its column.
            val fromX = plume.originX + front.movedXOf(column) + x
            val fromY = plume.originY + front.movedYOf(column) + y
            SmokeMotion.place(
                fromX,
                fromY,
                seed,
                ink,
                age,
                life,
                material,
                plume.travel,
                density,
                place,
                scratch,
            )
            if (pull > 0f) {
                place[0] += (plume.atX + x * sx - fromX) * pull
                place[1] += (plume.atY + y * sy - fromY) * pull
            }
            val band = PlumeBatches.bandOf(age / life)
            val spark = SmokeMotion.isSpark(seed, ink, material)
            val batch =
                if (spark) PlumeBatches.sparks(colours, band)
                else {
                    // Half are drawn smaller and half larger, so the smoke isn't one grain.
                    val size = if (fract(seed * 5.71f) < 0.5f) 0 else 1
                    PlumeBatches.of(particles.colour[i].toInt(), band, size)
                }
            batches.add(batch, place[0], place[1])
            // Once it has spread, a particle is drawn twice, a little apart, so the body of the
            // smoke reads as a soft mass rather than a few thousand dots.
            if (!spark && (!ink || age > SplatFrom)) {
                val reach = SmokeMotion.spreadAt(age, life, ink) * SplatReach * density
                if (reach > 0.5f) {
                    batches.add(
                        batch,
                        place[0] + (fract(seed * 31.7f) - 0.5f) * 2f * reach,
                        place[1] + (fract(seed * 47.3f) - 0.5f) * 2f * reach,
                    )
                }
            }
        }
        batches.seal()
    }

    /** Draws [layer] of every plume in [plumes] in [colors]. */
    fun DrawScope.draw(plumes: List<SmokePlume>, layer: SmokeLayer, colors: SmokeColors) {
        paint.blendMode =
            if (layer == SmokeLayer.Glow && colors.glowAdds) BlendMode.Plus else BlendMode.SrcOver
        drawIntoCanvas { canvas ->
            for (p in plumes.indices) {
                val plume = plumes[p]
                val particles = plume.particles ?: continue
                val colours = plume.batches.colours
                if (colours == 0 || colours != particles.palette.size) continue
                val fade =
                    if (plume.batchesReform >= 0f) {
                        1f - smoothstep(0.72f, 1f, plume.batchesReform)
                    } else 1f
                for (k in 0 until colours) {
                    for (band in 0 until PlumeBatches.Bands) {
                        val small = plume.batches.countOf(PlumeBatches.of(k, band, 0))
                        val large = plume.batches.countOf(PlumeBatches.of(k, band, 1))
                        if (small + large == 0) continue
                        if (
                            !look(
                                k,
                                Color(particles.palette[k]),
                                band,
                                plume.material,
                                colors,
                                layer,
                            )
                        )
                            continue
                        paint.color = lookColour.copy(alpha = (lookAlpha * fade).coerceIn(0f, 1f))
                        // The blurred layers' grains are of two sizes; the crisp ones of one.
                        val blurred = layer == SmokeLayer.Haze || layer == SmokeLayer.Soft
                        for (size in 0 until PlumeBatches.Sizes) {
                            val batch = PlumeBatches.of(k, band, size)
                            val count = plume.batches.countOf(batch)
                            if (count == 0) continue
                            val share = if (blurred) SizeClass[size] else 1f
                            paint.strokeWidth = lookSize * share * density
                            drawPointBatch(canvas, plume.batches.pointsOf(batch), count, paint)
                        }
                    }
                }
                if (layer == SmokeLayer.Glow) drawSparks(canvas, plume, colours, colors, fade)
            }
        }
    }

    private fun DrawScope.drawSparks(
        canvas: Canvas,
        plume: SmokePlume,
        colours: Int,
        colors: SmokeColors,
        fade: Float,
    ) {
        val life = representativeLife(ink = true, plume.material)
        for (band in 0 until PlumeBatches.Bands) {
            val batch = PlumeBatches.sparks(colours, band)
            val count = plume.batches.countOf(batch)
            if (count == 0) continue
            val age = PlumeBatches.middleOf(band) * life
            val glow = 1f - smoothstep(0.35f, 1.25f, age)
            if (glow <= 0f) continue
            paint.color = colors.spark.copy(alpha = (glow * fade).coerceIn(0f, 1f))
            paint.strokeWidth = SmokeDimens.SparkSize.toPx()
            drawPointBatch(canvas, plume.batches.pointsOf(batch), count, paint)
        }
    }

    // The look of a batch, set by [look]: its colour, opacity and size in dp.
    private var lookColour = Color.Unspecified
    private var lookAlpha = 0f
    private var lookSize = 0f

    /**
     * Works out how colour [k] of a row, [source], looks at age band [band] in [material], into
     * [lookColour], [lookAlpha] and [lookSize]. Returns whether it is drawn in [layer] at all.
     */
    private fun look(
        k: Int,
        source: Color,
        band: Int,
        material: SmokeMaterial,
        colors: SmokeColors,
        layer: SmokeLayer,
    ): Boolean {
        val ink = k > 0
        val life = representativeLife(ink, material)
        val age = PlumeBatches.middleOf(band) * life
        // A band that straddles the age where its particles move to the next layer is drawn in
        // both, each in the share of the band that falls there, so they cross over softly rather
        // than all at once.
        val from = PlumeBatches.startOf(band) * life
        val to = PlumeBatches.endOf(band) * life
        var share = 0f
        for (q in 0 until LayerSamples) {
            val at = from + (to - from) * (q + 0.5f) / LayerSamples
            if (layerOf(ink, at, material) == layer) share += 1f / LayerSamples
        }
        if (share == 0f) return false
        val spread = SmokeMotion.spreadAt(age, life, ink)
        val mist = material == SmokeMaterial.Mist
        val target = if (mist) colors.mist else colors.smoke
        val toSmoke =
            if (mist) smoothstep(0f, 0.7f, age) * if (ink) 0.9f else 0.7f
            else smoothstep(SmokeDimens.SmokeFrom, SmokeDimens.SmokeTo, age) * if (ink) 1f else 0.5f
        val ember =
            if (mist) 0f
            else (1f - smoothstep(0f, SmokeDimens.EmberSeconds, age)) * if (ink) 1f else 0.25f
        lookColour = lerp(lerp(source, target, toSmoke), colors.ember, ember * 0.9f)
        val fade = 1f - smoothstep(0.35f, 1f, age / life)
        val opacity =
            if (ink) {
                val thin = smoothstep(0f, 0.3f, spread)
                0.95f * (1f + (0.062f - 1f) * thin) * (1f + SmokeDimens.InkBoost * thin)
            } else {
                val thin = smoothstep(0f, 0.12f, spread)
                0.85f * (1f + (0.024f - 1f) * thin) * (1f + SmokeDimens.CardBoost * thin)
            }
        lookAlpha = opacity * fade * share * if (mist) SmokeDimens.MistOpacity else 1f
        val size =
            if (ink) 1.3f + (17f - 1.3f) * spread.pow(0.6f) else 2f + (30f - 2f) * spread.pow(0.45f)
        lookSize = size * SizeShare[layer.ordinal]
        return lookAlpha > 0.004f
    }

    private companion object {
        // A typical particle's life, for a batch's look.
        fun representativeLife(ink: Boolean, material: SmokeMaterial): Float =
            (SmokeDimens.Life + SmokeDimens.LifeSpread / 2f) *
                (if (ink) 1f else SmokeDimens.BackgroundLife) *
                if (material == SmokeMaterial.Mist) SmokeDimens.MistLife else 1f

        // Which layer a particle of this kind and age is drawn in: the glow while it is an ember,
        // crisp while it still carries a shape, then softer and softer as it spreads.
        fun layerOf(ink: Boolean, age: Float, material: SmokeMaterial): SmokeLayer =
            when {
                material == SmokeMaterial.Smoke && ink && age < 0.25f -> SmokeLayer.Glow
                ink && age < 0.55f -> SmokeLayer.Crisp
                ink && age < 1.5f -> SmokeLayer.Soft
                ink -> SmokeLayer.Haze
                age < 0.08f -> SmokeLayer.Crisp
                age < 0.5f -> SmokeLayer.Soft
                else -> SmokeLayer.Haze
            }

        // Ink is drawn a second time once it is this old, this many dp off at its widest.
        const val SplatFrom = 0.55f
        const val SplatReach = 6f

        // How finely a band is sampled for the layers it falls in.
        const val LayerSamples = 4

        // The smaller and the larger of a blurred layer's grains, against a particle's own size.
        val SizeClass = floatArrayOf(0.75f, 1.3f)

        // A blurred layer's particles are drawn smaller, the blur spreading them out again.
        val SizeShare = floatArrayOf(0.5f, 0.6f, 0.75f, 1f)
    }
}

/**
 * Draws the first [count] points of [points] (x, y pairs, in px) in one call, with [paint]. Every
 * point past them lies off the screen, so a platform that can't take a count draws them all.
 */
internal expect fun drawPointBatch(canvas: Canvas, points: FloatArray, count: Int, paint: Paint)

/** Gives [bitmap]'s memory back now, rather than when it is collected. */
internal expect fun releaseBitmap(bitmap: ImageBitmap)
