package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.TAU

/** How much the plain glass magnifies the interior, as the prototype's fallback scales it. */
internal const val FallbackZoom = 1 / .86f

/**
 * The prototype's 2D fallback, for where the shader can't run: over the interior (drawn
 * [FallbackZoom] larger) a darkening rim, the window and its glint, the caustic glow, the rim line
 * and the air [bubble]. [caustics] is the weather's caustic strength.
 */
internal fun DrawScope.drawGlassFallback(
    center: Offset,
    radius: Float,
    caustics: Float,
    bubble: Offset,
) {
    drawIntoCanvas { canvas ->
        val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
        val cxp = center.x
        val cyp = center.y
        val r = radius
        c.save()
        c.beginPath()
        c.arc(cxp, cyp, r - .5f, 0f, TAU)
        c.clip()
        var g = c.createRadialGradient(cxp, cyp, r * .5f, cxp, cyp, r)
        g.addColorStop(0f, "rgba(20,40,70,0)")
        g.addColorStop(.85f, "rgba(20,40,70,.12)")
        g.addColorStop(1f, "rgba(230,240,255,.4)")
        c.fillStyle = g
        c.fillRect(0f, 0f, size.width, size.height)
        c.save()
        c.translate(cxp - r * .42f, cyp - r * .5f)
        c.rotate(-.5f)
        g = c.createLinearGradient(0f, -r * .2f, 0f, r * .2f)
        g.addColorStop(0f, "rgba(255,255,255,.4)")
        g.addColorStop(1f, "rgba(255,255,255,.08)")
        c.fillStyle = g
        c.beginPath()
        c.ellipse(0f, 0f, r * .22f, r * .12f, 0f, 0f, TAU)
        c.fill()
        c.restore()
        c.fillStyle = "rgba(255,255,255,.9)"
        c.beginPath()
        c.arc(cxp - r * .5f, cyp - r * .56f, r * .03f, 0f, TAU)
        c.fill()
        g =
            c.createRadialGradient(
                cxp + r * .55f,
                cyp + r * .6f,
                0f,
                cxp + r * .55f,
                cyp + r * .6f,
                r * .5f,
            )
        g.addColorStop(0f, rgba("255,224,170", .35 * caustics))
        g.addColorStop(1f, "rgba(255,224,170,0)")
        c.fillStyle = g
        c.fillRect(0f, 0f, size.width, size.height)
        c.restore()
        c.strokeStyle = "rgba(255,255,255,.55)"
        c.lineWidth = 1.2f * density
        c.beginPath()
        c.arc(cxp, cyp, r - 1, 0f, TAU)
        c.stroke()
        c.fillStyle = "rgba(255,255,255,.4)"
        c.beginPath()
        c.ellipse(cxp + bubble.x * r, cyp + bubble.y * r, r * .06f, r * .035f, 0f, 0f, TAU)
        c.fill()
    }
}
