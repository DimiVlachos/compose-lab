package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import dev.dimvlachos.lab.core.presentation.ui.SnowGlobeArt
import kotlin.math.sin

// Everything inside the glass is drawn in globe units (the sphere's centre at 0,0, its radius 1),
// so its paths and brushes don't depend on the globe's size and are built once, here.

private val Sphere =
    Path().apply { addOval(Rect(center = Offset.Zero, radius = SnowGlobeDimens.InnerRadius)) }

private val SkyTopLeft = Offset(-1f, -1f)
private val SkySize = Size(2f, 2f)

private fun sky(top: Color, low: Color) = Brush.verticalGradient(listOf(top, low), -1f, 0.5f)

private val SunnySky = sky(SnowGlobeArt.sunnySkyTop, SnowGlobeArt.sunnySkyLow)
private val WindySky = sky(SnowGlobeArt.windySkyTop, SnowGlobeArt.windySkyLow)
private val ShowerSky = sky(SnowGlobeArt.showerSkyTop, SnowGlobeArt.showerSkyLow)

private val RainbowBand = Stroke(width = 0.035f)

private val Dune =
    Path().apply {
        moveTo(-1f, Figurine.duneTop(-1f))
        var x = -1f
        while (x <= 1f) {
            lineTo(x, Figurine.duneTop(x))
            x += 0.05f
        }
        lineTo(1f, 1f)
        lineTo(-1f, 1f)
        close()
    }
private val Sand =
    Brush.verticalGradient(listOf(SnowGlobeArt.duneTop, SnowGlobeArt.duneLow), 0.35f, 1f)

private val Tower =
    with(Figurine.Lighthouse) {
        val foot = Figurine.duneTop(-0.3f) + 0.02f
        Path().apply {
            moveTo(TowerBaseLeft, foot)
            lineTo(TowerTopLeft, galleryTop)
            lineTo(TowerTopRight, galleryTop)
            lineTo(TowerBaseRight, foot)
            close()
        }
    }
private val TowerPaint =
    Brush.horizontalGradient(
        listOf(SnowGlobeArt.towerLit, SnowGlobeArt.towerShade),
        Figurine.Lighthouse.TowerBaseLeft,
        Figurine.Lighthouse.TowerBaseRight,
    )
private val BeamCenter = Offset(-0.30f, -0.36f)
private val Beam =
    Brush.radialGradient(listOf(SnowGlobeArt.lanternBeam, Color.Transparent), BeamCenter, 0.25f)

private class CottageArt(val cottage: Figurine.Cottage) {
    val base = Figurine.duneTop(cottage.centre) + 0.03f
    val roof =
        Path().apply {
            moveTo(cottage.left - 0.01f, cottage.eaves + 0.005f)
            lineTo(cottage.centre, cottage.ridge)
            lineTo(cottage.right + 0.01f, cottage.eaves + 0.005f)
            close()
        }
}

private val Cottages = listOf(CottageArt(Figurine.CottageA), CottageArt(Figurine.CottageB))

private val VaneHead =
    Path().apply {
        moveTo(Figurine.VaneX + 0.08f, Figurine.VanePostTop)
        lineTo(Figurine.VaneX + 0.05f, Figurine.VanePostTop - 0.02f)
        lineTo(Figurine.VaneX + 0.05f, Figurine.VanePostTop + 0.02f)
        close()
    }

/** The scene, then the glitter, inside the sphere of [radius] around [center]. */
internal fun DrawScope.drawInterior(state: SnowGlobeState, center: Offset, radius: Float) {
    inGlobeUnits(center, radius) {
        clipPath(Sphere) {
            drawScene(state.sceneFrom, state.time, 1f)
            if (state.fade < 1f) drawScene(state.sceneTo, state.time, state.fade)
            drawGlitter(state)
        }
    }
}

private fun DrawScope.drawScene(scene: GlobeScene, t: Float, alpha: Float) {
    val sky =
        when (scene) {
            GlobeScene.Sunny -> SunnySky
            GlobeScene.Windy -> WindySky
            GlobeScene.Shower -> ShowerSky
        }
    drawRect(sky, topLeft = SkyTopLeft, size = SkySize, alpha = alpha)
    if (scene == GlobeScene.Shower) drawRainbow(alpha)
    drawClouds(scene, t, alpha)
    drawFigurine(scene, t, alpha)
    if (scene == GlobeScene.Shower) drawRain(t, alpha)
}

private fun DrawScope.drawRainbow(alpha: Float) {
    SnowGlobeArt.rainbow.forEachIndexed { i, c ->
        val r = 0.78f - i * 0.035f
        drawArc(
            color = c,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(-0.55f - r, 0.3f - r),
            size = Size(r * 2, r * 2),
            alpha = alpha,
            style = RainbowBand,
        )
    }
}

private fun DrawScope.drawClouds(scene: GlobeScene, t: Float, alpha: Float) {
    val speed = if (scene == GlobeScene.Windy) 0.22f else 0.03f
    val tint = if (scene == GlobeScene.Shower) SnowGlobeArt.showerCloud else SnowGlobeArt.cloud
    // A sunny day is a clear sky: a cloud drifting past the rim would be magnified to a blob.
    if (scene == GlobeScene.Sunny) return
    val count = 3
    for (i in 0 until count) {
        val x = ((i * 0.83f + t * speed) % 2.4f) - 1.2f
        val y = -0.62f + i * 0.12f
        for (puff in 0..2) {
            drawCircle(
                tint,
                radius = 0.10f + puff * 0.02f,
                center = Offset(x + puff * 0.11f, y - puff % 2 * 0.04f),
                alpha = alpha * 0.92f,
            )
        }
    }
}

private fun DrawScope.drawFigurine(scene: GlobeScene, t: Float, alpha: Float) {
    drawPath(Dune, Sand, alpha = alpha)

    // Lighthouse.
    with(Figurine.Lighthouse) {
        drawPath(Tower, TowerPaint, alpha = alpha)
        drawRect(
            SnowGlobeArt.ironwork,
            topLeft = Offset(galleryLeft, galleryTop),
            size = Size(galleryRight - galleryLeft, 0.025f),
            alpha = alpha,
        )
        val glow =
            if (scene == GlobeScene.Sunny) SnowGlobeArt.lanternDay else SnowGlobeArt.lanternLit
        drawRect(
            glow,
            topLeft = Offset(LanternLeft, LanternTop),
            size = Size(LanternRight - LanternLeft, galleryTop - LanternTop),
            alpha = alpha,
        )
        drawArc(
            color = SnowGlobeArt.ironwork,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(CapLeft, CapTop),
            size = Size(CapRight - CapLeft, (LanternTop - CapTop) * 2f),
            alpha = alpha,
        )
        if (scene != GlobeScene.Sunny) {
            drawCircle(Beam, radius = 0.25f, center = BeamCenter, alpha = alpha)
        }
    }

    // Cottages.
    for (art in Cottages) {
        val c = art.cottage
        drawRect(
            SnowGlobeArt.shingle,
            topLeft = Offset(c.left + 0.01f, c.eaves),
            size = Size(c.right - c.left - 0.02f, art.base - c.eaves),
            alpha = alpha,
        )
        drawPath(art.roof, SnowGlobeArt.roof, alpha = alpha)
        drawRect(
            SnowGlobeArt.window,
            topLeft = Offset(c.centre - 0.03f, c.eaves + 0.05f),
            size = Size(0.06f, 0.05f),
            alpha = alpha * 0.9f,
        )
    }

    // Weathervane: hunts gently on a calm day, swings round in the wind.
    val swing =
        if (scene == GlobeScene.Windy) 35f * sin(t * 2.3f) + 20f * sin(t * 5.1f)
        else 6f * sin(t * 0.8f)
    drawLine(
        SnowGlobeArt.vanePost,
        start = Offset(Figurine.VaneX, Figurine.CottageA.ridge),
        end = Offset(Figurine.VaneX, Figurine.VanePostTop),
        strokeWidth = 0.008f,
        alpha = alpha,
    )
    rotate(swing, pivot = Offset(Figurine.VaneX, Figurine.VanePostTop)) {
        drawLine(
            SnowGlobeArt.vane,
            start = Offset(Figurine.VaneX - 0.06f, Figurine.VanePostTop),
            end = Offset(Figurine.VaneX + 0.06f, Figurine.VanePostTop),
            strokeWidth = 0.012f,
            cap = StrokeCap.Round,
            alpha = alpha,
        )
        drawPath(VaneHead, SnowGlobeArt.vane, alpha = alpha)
    }
}

private fun DrawScope.drawRain(t: Float, alpha: Float) {
    for (i in 0 until 40) {
        val x = ((i * 0.137f) % 1.8f) - 0.9f
        val y = (((i * 0.311f) + t * 1.6f) % 1.6f) - 0.9f
        drawLine(
            SnowGlobeArt.rain,
            start = Offset(x, y),
            end = Offset(x - 0.02f, y + 0.07f),
            strokeWidth = 0.006f,
            alpha = alpha,
        )
    }
}

private fun DrawScope.drawGlitter(state: SnowGlobeState) {
    val g = state.glitter
    for (i in 0 until g.count) {
        val shine = g.glint(i)
        val c = if (shine > 0.6f) SnowGlobeArt.goldBright else SnowGlobeArt.gold
        drawCircle(
            c,
            radius = g.size[i] * (0.7f + 0.6f * shine),
            center = Offset(g.x[i], g.y[i]),
            alpha = 0.75f + 0.25f * shine,
        )
    }
}
