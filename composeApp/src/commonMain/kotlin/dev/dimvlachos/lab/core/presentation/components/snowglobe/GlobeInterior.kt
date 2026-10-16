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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.sin

private val Gold = Color(0xFFE9C46A)
private val GoldBright = Color(0xFFFFF4CF)

private val RainbowBands =
    listOf(
        Color(0x55E05A5A),
        Color(0x55E8A94F),
        Color(0x55E8DA5B),
        Color(0x5563B86A),
        Color(0x55588BD6),
        Color(0x557A5BC2),
    )

/** The scene, then the glitter, inside the sphere of [radius] around [center]. */
internal fun DrawScope.drawInterior(state: SnowGlobeState, center: Offset, radius: Float) {
    val sphere =
        Path().apply {
            addOval(Rect(center = center, radius = radius * SnowGlobeDimens.InnerRadius))
        }
    clipPath(sphere) {
        translate(center.x, center.y) {
            scale(radius, radius, pivot = Offset.Zero) {
                drawScene(state.sceneFrom, state.time, 1f)
                if (state.fade < 1f) drawScene(state.sceneTo, state.time, state.fade)
                drawGlitter(state)
            }
        }
    }
}

private fun DrawScope.drawScene(scene: GlobeScene, t: Float, alpha: Float) {
    val sky =
        when (scene) {
            GlobeScene.Sunny -> listOf(Color(0xFF5FA8E8), Color(0xFFBFE0F7))
            GlobeScene.Windy -> listOf(Color(0xFF7D9DB8), Color(0xFFC9D8E4))
            GlobeScene.Shower -> listOf(Color(0xFF5E7186), Color(0xFFA9B7C4))
        }
    drawRect(
        Brush.verticalGradient(sky, startY = -1f, endY = 0.5f),
        topLeft = Offset(-1f, -1f),
        size = Size(2f, 2f),
        alpha = alpha,
    )
    if (scene == GlobeScene.Shower) drawRainbow(alpha)
    drawClouds(scene, t, alpha)
    drawFigurine(scene, t, alpha)
    if (scene == GlobeScene.Shower) drawRain(t, alpha)
}

private fun DrawScope.drawRainbow(alpha: Float) {
    RainbowBands.forEachIndexed { i, c ->
        val r = 0.78f - i * 0.035f
        drawArc(
            color = c,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(-0.55f - r, 0.3f - r),
            size = Size(r * 2, r * 2),
            alpha = alpha,
            style = Stroke(width = 0.035f),
        )
    }
}

private fun DrawScope.drawClouds(scene: GlobeScene, t: Float, alpha: Float) {
    val speed = if (scene == GlobeScene.Windy) 0.22f else 0.03f
    val tint = if (scene == GlobeScene.Shower) Color(0xFFD7DEE6) else Color.White
    val count = if (scene == GlobeScene.Sunny) 1 else 3
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
    // Dune.
    val dune =
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
    drawPath(
        dune,
        Brush.verticalGradient(
            listOf(Color(0xFFE9D7AE), Color(0xFFC9AE7C)),
            startY = 0.35f,
            endY = 1f,
        ),
        alpha = alpha,
    )

    // Lighthouse.
    with(Figurine.Lighthouse) {
        val foot = Figurine.duneTop(-0.3f) + 0.02f
        val tower =
            Path().apply {
                moveTo(TowerBaseLeft, foot)
                lineTo(TowerTopLeft, galleryTop)
                lineTo(TowerTopRight, galleryTop)
                lineTo(TowerBaseRight, foot)
                close()
            }
        drawPath(
            tower,
            Brush.horizontalGradient(
                listOf(Color(0xFFF7F4EE), Color(0xFFCFC8BC)),
                startX = TowerBaseLeft,
                endX = TowerBaseRight,
            ),
            alpha = alpha,
        )
        drawRect(
            Color(0xFF2B2F36),
            topLeft = Offset(galleryLeft, galleryTop),
            size = Size(galleryRight - galleryLeft, 0.025f),
            alpha = alpha,
        )
        val glow = if (scene == GlobeScene.Sunny) Color(0xFFFFE7A3) else Color(0xFFFFD36B)
        drawRect(
            glow,
            topLeft = Offset(LanternLeft, LanternTop),
            size = Size(LanternRight - LanternLeft, galleryTop - LanternTop),
            alpha = alpha,
        )
        drawArc(
            color = Color(0xFF2B2F36),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(CapLeft, CapTop),
            size = Size(CapRight - CapLeft, (LanternTop - CapTop) * 2f),
            alpha = alpha,
        )
        if (scene != GlobeScene.Sunny) {
            val beam = Offset(-0.30f, -0.36f)
            drawCircle(
                Brush.radialGradient(listOf(Color(0x66FFE08A), Color.Transparent), beam, 0.25f),
                radius = 0.25f,
                center = beam,
                alpha = alpha,
            )
        }
    }

    // Cottages.
    for (c in Cottages) {
        val base = Figurine.duneTop(c.centre) + 0.03f
        drawRect(
            Color(0xFF9AA0A6),
            topLeft = Offset(c.left + 0.01f, c.eaves),
            size = Size(c.right - c.left - 0.02f, base - c.eaves),
            alpha = alpha,
        )
        val roof =
            Path().apply {
                moveTo(c.left - 0.01f, c.eaves + 0.005f)
                lineTo(c.centre, c.ridge)
                lineTo(c.right + 0.01f, c.eaves + 0.005f)
                close()
            }
        drawPath(roof, Color(0xFF5B5550), alpha = alpha)
        drawRect(
            Color(0xFFFFF3C8),
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
        Color(0xFF8C5A2B),
        start = Offset(Figurine.VaneX, Figurine.CottageA.ridge),
        end = Offset(Figurine.VaneX, Figurine.VanePostTop),
        strokeWidth = 0.008f,
        alpha = alpha,
    )
    rotate(swing, pivot = Offset(Figurine.VaneX, Figurine.VanePostTop)) {
        drawLine(
            Color(0xFFB87333),
            start = Offset(Figurine.VaneX - 0.06f, Figurine.VanePostTop),
            end = Offset(Figurine.VaneX + 0.06f, Figurine.VanePostTop),
            strokeWidth = 0.012f,
            cap = StrokeCap.Round,
            alpha = alpha,
        )
        val head =
            Path().apply {
                moveTo(Figurine.VaneX + 0.08f, Figurine.VanePostTop)
                lineTo(Figurine.VaneX + 0.05f, Figurine.VanePostTop - 0.02f)
                lineTo(Figurine.VaneX + 0.05f, Figurine.VanePostTop + 0.02f)
                close()
            }
        drawPath(head, Color(0xFFB87333), alpha = alpha)
    }
}

private val Cottages = listOf(Figurine.CottageA, Figurine.CottageB)

private fun DrawScope.drawRain(t: Float, alpha: Float) {
    for (i in 0 until 40) {
        val x = ((i * 0.137f) % 1.8f) - 0.9f
        val y = (((i * 0.311f) + t * 1.6f) % 1.6f) - 0.9f
        drawLine(
            Color(0x88D6E4F0),
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
        val c = if (shine > 0.6f) GoldBright else Gold
        drawCircle(
            c,
            radius = g.size[i] * (0.7f + 0.6f * shine),
            center = Offset(g.x[i], g.y[i]),
            alpha = 0.75f + 0.25f * shine,
        )
    }
}
