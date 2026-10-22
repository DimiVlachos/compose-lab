package dev.dimvlachos.lab.core.presentation.components.keyhook

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.TAU
import dev.dimvlachos.lab.core.presentation.canvas2d.freeze
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

// The rack, ported call by call from the prototype's draw(). Coordinates are the hero's dp; [px]
// is the screen's pixels per dp, which the prototype's shadows (given in CSS px times its DPR)
// are scaled by.

private const val PI_F = PI.toFloat()

private object Brass {
    const val edge = "#3e2806"
    const val dark = "#6d4a12"
    const val mid = "#bb8c33"
    const val light = "#f3d68d"
    const val spec = "#fff7dc"
}

private val MetalBrass = arrayOf("#6e4a10", "#c5953b", "#f6dd9b")
private val MetalNickel = arrayOf("#545c64", "#a7b0b8", "#f0f3f6")

/**
 * The walnut grain for a board [w] × [h] dp, baked once per size at [px] pixels per dp with the
 * prototype's fixed seed, so it never crawls.
 */
internal fun bakeWood(w: Float, h: Float, px: Float): ImageBitmap {
    val c = Canvas2D.create(max(1, (w * px).roundToInt()), max(1, (h * px).roundToInt()))
    c.scale(px, px)
    var s = 11L
    val r = {
        s = (s * 16807L) % 2147483647L
        s.toFloat() / 2147483647f
    }
    var g = c.createLinearGradient(0f, 0f, 0f, h)
    g.addColorStop(0f, "#9a6236")
    g.addColorStop(.45f, "#7b4724")
    g.addColorStop(1f, "#5a3117")
    c.fillStyle = g
    c.fillRect(0f, 0f, w, h)
    c.lineCap = "butt"
    for (i in 0 until 90) {
        val y0 = r() * h
        val a = .05f + r() * .14f
        val amp = 1f + r() * 2.6f
        val fr = .006f + r() * .012f
        val ph = r() * TAU
        c.strokeStyle =
            if (r() < .62f) Color(45, 20, 6).copy(alpha = a)
            else Color(255, 205, 150).copy(alpha = a * .55f)
        c.lineWidth = .5f + r() * 1.3f
        c.beginPath()
        var x = -4f
        while (x <= w + 4f) {
            val y = y0 + sin(x * fr + ph) * amp + sin(x * fr * 3.1f + ph) * amp * .3f
            if (x < 0f) c.moveTo(x, y) else c.lineTo(x, y)
            x += 6f
        }
        c.stroke()
    }
    // One soft knot, with the grain bending round it.
    val kx = w * .63f
    val ky = h * .7f
    for (k in 0 until 6) {
        c.strokeStyle = Color(50, 22, 6).copy(alpha = .22f - k * .03f)
        c.lineWidth = 1f
        c.beginPath()
        c.ellipse(kx, ky, 4f + k * 3.2f, 1.6f + k * 1.2f, 0f, 0f, TAU)
        c.stroke()
    }
    // The lamp sits up and to the left: the left of the board is warmer.
    g = c.createLinearGradient(0f, 0f, w, 0f)
    g.addColorStop(0f, "rgba(255,200,130,.16)")
    g.addColorStop(.5f, "rgba(255,200,130,0)")
    g.addColorStop(1f, "rgba(20,8,0,.18)")
    c.fillStyle = g
    c.fillRect(0f, 0f, w, h)
    return c.bitmap.also { it.freeze() }
}

/** Fonts the rack names: `archivo` for the tags, `mono` for the name plates. */
internal const val TagFont = "archivo"
internal const val PlateFont = "mono"

/**
 * Bakes what never moves for [g0] — the lamp's glow, the board with its [wood], the name plates,
 * rosettes and hooks, and the D-ring's back — into a bitmap the size of the hero at [px] pixels per
 * dp, drawn as the prototype draws them each frame. [measurer] and [fonts] set the plates.
 */
internal fun bakeRackBackdrop(
    state: KeyRackState,
    g0: RackGeometry,
    dark: Boolean,
    px: Float,
    wood: ImageBitmap,
    measurer: TextMeasurer,
    fonts: Map<String, FontFamily>,
): ImageBitmap {
    val c = Canvas2D.create(backdropSize(g0.width, px), backdropSize(g0.height, px))
    c.textMeasurer = measurer
    c.fonts = fonts
    c.scale(px, px)
    c.drawBackdrop(state, g0, dark, px, wood)
    return c.bitmap.also { it.freeze() }
}

private fun backdropSize(dp: Float, px: Float) = max(1, (dp * px).roundToInt())

private fun Canvas2D.drawBackdrop(
    state: KeyRackState,
    g0: RackGeometry,
    dark: Boolean,
    px: Float,
    wood: ImageBitmap,
) {
    val u = g0.u
    val w = g0.width
    val h = g0.height
    val boardTop = g0.boardTop
    val boardL = g0.boardL
    val boardR = g0.boardR
    // The browser's context keeps these from the first frame on: every stroke after it is round.
    lineCap = "round"
    lineJoin = "round"

    // A warm pool of lamp light on the wall round the rack, faded out over the hero's last stretch
    // so it never ends in a line above the feed. Its own layer, so the fade takes only the glow.
    beginLayer()
    save()
    translate(w * .42f, boardTop + 20f * u)
    scale(1.6f, 1f)
    var g = createRadialGradient(-10f, -16f, 8f, 0f, 0f, w * .5f)
    g.addColorStop(0f, if (dark) "rgba(255,180,90,.16)" else "rgba(255,212,150,.5)")
    g.addColorStop(.6f, if (dark) "rgba(255,180,90,.05)" else "rgba(255,212,150,.16)")
    g.addColorStop(1f, "rgba(255,212,150,0)")
    fillStyle = g
    fillRect(-w * .5f, -w * .5f, w, w)
    restore()
    save()
    globalCompositeOperation = "destination-out"
    g = createLinearGradient(0f, h - 40f * u, 0f, h)
    g.addColorStop(0f, "rgba(0,0,0,0)")
    g.addColorStop(1f, "rgba(0,0,0,1)")
    fillStyle = g
    fillRect(0f, h - 40f * u, w, 40f * u)
    restore()
    endLayer()

    // ---- The board ----
    val bw = boardR - boardL
    val bh = 66f * u
    save()
    shadowColor = if (dark) "rgba(0,0,0,.65)" else "rgba(60,32,8,.32)"
    shadowBlur = 16f * u * px
    shadowOffsetX = 6f * u * px
    shadowOffsetY = 9f * u * px
    rr(boardL, boardTop, bw, bh, 7f * u)
    fillStyle = "#6b3d1f"
    fill()
    restore()
    save()
    rr(boardL, boardTop, bw, bh, 7f * u)
    clip()
    drawImage(wood, boardL, boardTop, bw, bh)
    if (dark) {
        fillStyle = "rgba(10,6,3,.22)"
        fillRect(boardL, boardTop, bw, bh)
    }
    g = createLinearGradient(0f, boardTop, 0f, boardTop + bh)
    g.addColorStop(0f, "rgba(255,225,180,.38)")
    g.addColorStop(.07f, "rgba(255,225,180,0)")
    g.addColorStop(.85f, "rgba(30,12,0,0)")
    g.addColorStop(1f, "rgba(30,12,0,.35)")
    fillStyle = g
    fillRect(boardL, boardTop, bw, bh)
    restore()
    // A routed bevel round the edge.
    save()
    rr(boardL + 1.5f * u, boardTop + 1.5f * u, bw - 3f * u, bh - 3f * u, 6f * u)
    g = createLinearGradient(boardL, boardTop, boardL + bw * .4f, boardTop + bh * 2.5f)
    g.addColorStop(0f, "rgba(255,220,170,.5)")
    g.addColorStop(.5f, "rgba(255,220,170,.06)")
    g.addColorStop(1f, "rgba(25,10,0,.45)")
    strokeStyle = g
    lineWidth = 1.6f * u
    stroke()
    restore()
    // Two mounting screws.
    screw(boardL + 9f * u, boardTop + 33f * u, 2.6f * u, -.5f, u)
    screw(boardR - 9f * u, boardTop + 33f * u, 2.6f * u, -.5f, u)

    // ---- Name plates and hooks ----
    for (r in state.rings) {
        val hx = g0.hookX(r.index)
        val hy = g0.hookY
        val plateX = hx - 5f * u
        val plateY = boardTop + 15f * u
        val pw = 44f * u
        val ph = 13f * u
        save()
        shadowColor = "rgba(30,12,0,.45)"
        shadowBlur = 2f * u * px
        shadowOffsetX = .8f * u * px
        shadowOffsetY = 1.4f * u * px
        rr(plateX - pw / 2f, plateY - ph / 2f, pw, ph, 2.2f * u)
        g = createLinearGradient(plateX - pw / 2f, plateY - ph, plateX + pw / 2f, plateY + ph)
        g.addColorStop(0f, "#f7df9f")
        g.addColorStop(.45f, "#c99a3f")
        g.addColorStop(1f, "#8a6119")
        fillStyle = g
        fill()
        restore()
        // The prototype asks for 700 but loads only 400 and 600, so the browser draws 600.
        font = "600 ${7f * u}px $PlateFont"
        textAlign = "center"
        textBaseline = "middle"
        fillStyle = "rgba(255,245,215,.55)"
        fillText(r.holder.plate, plateX, plateY + .7f * u)
        fillStyle = "rgba(55,32,4,.82)"
        fillText(r.holder.plate, plateX, plateY)
        // Rosette, then the hook.
        val ry = hy - 24f * u
        save()
        shadowColor = "rgba(30,12,0,.5)"
        shadowBlur = 3f * u * px
        shadowOffsetX = 1.5f * u * px
        shadowOffsetY = 2.5f * u * px
        fillStyle = "#9b7128"
        beginPath()
        arc(plateX, ry, 6.5f * u, 0f, TAU)
        fill()
        restore()
        g = createRadialGradient(plateX - 2.4f * u, ry - 2.6f * u, 0f, plateX, ry, 7f * u)
        g.addColorStop(0f, "#fff2c8")
        g.addColorStop(.45f, "#d2a34a")
        g.addColorStop(1f, "#6e4c10")
        fillStyle = g
        beginPath()
        arc(plateX, ry, 6.5f * u, 0f, TAU)
        fill()
        save()
        shadowColor = if (dark) "rgba(0,0,0,.6)" else "rgba(40,18,0,.45)"
        shadowBlur = 3f * u * px
        shadowOffsetX = 3f * u * px
        shadowOffsetY = 5f * u * px
        hookPath(hx, hy, false, u)
        lineWidth = 2.8f * u
        strokeStyle = Brass.dark
        lineCap = "round"
        stroke()
        restore()
        tube(2.8f * u, u) { hookPath(hx, hy, false, u) }
        drawHookFront(hx, hy, u)
    }

    // ---- The D-ring on the account card ----
    val tx = g0.carryX
    val ty = g0.cardBottom
    save()
    shadowColor = "rgba(30,15,0,.35)"
    shadowBlur = 2f * u * px
    shadowOffsetY = 1.5f * u * px
    rr(tx - 6f * u, ty - 5f * u, 12f * u, 13f * u, 2f * u)
    fillStyle = "#6a4326"
    fill()
    restore()
    setLineDash(floatArrayOf(1.4f * u, 1.2f * u))
    strokeStyle = "rgba(255,225,190,.5)"
    lineWidth = .6f * u
    beginPath()
    moveTo(tx - 4f * u, ty + 4.5f * u)
    lineTo(tx + 4f * u, ty + 4.5f * u)
    stroke()
    setLineDash(NoDash)
    tube(2.4f * u, u) { dPath(g0, false) }
}

/**
 * Draws the rack for [state] in its hero: the [backdrop] baked for its size, then the keys over it.
 */
internal fun Canvas2D.drawKeyRack(
    state: KeyRackState,
    dark: Boolean,
    px: Float,
    backdrop: ImageBitmap,
) {
    val g0 = state.geometry ?: return
    // Laid out for another size, as while the screen turns: the next frame has the right one.
    if (backdrop.width != backdropSize(g0.width, px)) return
    if (backdrop.height != backdropSize(g0.height, px)) return
    val u = g0.u
    drawImage(backdrop, 0f, 0f, g0.width, g0.height)
    // The browser's context keeps these from the first frame on: every stroke after it is round.
    lineCap = "round"
    lineJoin = "round"

    // ---- Keys: resting sets first, then anything in flight or in hand on top ----
    for (pass in 0..1) {
        for (r in state.rings) {
            val resting = r.mode == KeyRing.Mode.Rest
            if ((pass == 0) != resting) continue
            drawRing(state, r, dark, px)
            // The front of whatever it hangs on goes over the ring, so it looks threaded on.
            if (resting) {
                if (state.carriedIndex == r.index) tube(2.4f * u, u) { dPath(g0, true) }
                else drawHookFront(g0.hookX(r.index), g0.hookY, u)
            }
        }
    }
}

private val NoDash = FloatArray(0)

private fun Canvas2D.rr(x: Float, y: Float, w: Float, h: Float, r: Float) {
    beginPath()
    moveTo(x + r, y)
    arcTo(x + w, y, x + w, y + h, r)
    arcTo(x + w, y + h, x, y + h, r)
    arcTo(x, y + h, x, y, r)
    arcTo(x, y, x + w, y, r)
    closePath()
}

/** A brass rod: [path] stroked in five passes, each narrower and nudged toward the light. */
private inline fun Canvas2D.tube(t: Float, u: Float, path: () -> Unit) {
    lineJoin = "round"
    lineCap = "round"
    tubePass(t + 1.4f, Brass.edge, .6f * u, 1.2f * u, .8f, path)
    tubePass(t, Brass.dark, 0f, 0f, 1f, path)
    tubePass(t * .72f, Brass.mid, -t * .08f, -t * .05f, 1f, path)
    tubePass(t * .38f, Brass.light, -t * .16f, -t * .1f, .95f, path)
    tubePass(t * .12f, Brass.spec, -t * .2f, -t * .14f, .85f, path)
}

private inline fun Canvas2D.tubePass(
    w: Float,
    color: String,
    dx: Float,
    dy: Float,
    a: Float,
    path: () -> Unit,
) {
    save()
    globalAlpha = a
    translate(dx, dy)
    lineWidth = w
    strokeStyle = color
    path()
    stroke()
    restore()
}

private fun Canvas2D.shadowOn(lift: Float, dark: Boolean, u: Float, px: Float) {
    setShadowColor(
        if (dark) Color(0f, 0f, 0f, .6f - .15f * lift)
        else Color(55, 30, 8).copy(alpha = .34f - .1f * lift),
    )
    shadowBlur = (3f + 10f * lift) * u * px
    shadowOffsetX = (2.5f + 7f * lift) * u * px
    shadowOffsetY = (4f + 10f * lift) * u * px
}

// Each item is drawn in its own units: the pivot (where the ring passes through) at 0,0, hanging
// down +y.
private fun Canvas2D.drawKey(metal: KeyMetal, lift: Float, dark: Boolean, u: Float, px: Float) {
    val m3 = if (metal == KeyMetal.Brass) MetalBrass else MetalNickel
    val d = m3[0]
    val m = m3[1]
    val l = m3[2]
    beginPath()
    arc(0f, 6f, 8f, 0f, TAU)
    moveTo(2.4f, 0f)
    arc(0f, 0f, 2.4f, 0f, TAU, true)
    moveTo(-3.4f, 13.6f)
    lineTo(3.4f, 13.6f)
    lineTo(3.4f, 24f)
    var i = 0
    while (i < Teeth.size) {
        lineTo(Teeth[i], Teeth[i + 1])
        i += 2
    }
    lineTo(1.4f, 48.4f)
    lineTo(-3.4f, 46.4f)
    closePath()
    val g = createLinearGradient(-8f, -4f, 8f, 4f)
    g.addColorStop(0f, d)
    g.addColorStop(.33f, l)
    g.addColorStop(.55f, m)
    g.addColorStop(1f, d)
    save()
    shadowOn(lift, dark, u, px)
    fillStyle = g
    fill()
    restore()
    strokeStyle = "rgba(30,20,5,.35)"
    lineWidth = .5f
    stroke()
    // The milled groove down the blade.
    lineCap = "round"
    strokeStyle = "rgba(30,18,4,.32)"
    lineWidth = 1.1f
    beginPath()
    moveTo(-.9f, 16f)
    lineTo(-.9f, 44f)
    stroke()
    strokeStyle = "rgba(255,255,255,.4)"
    lineWidth = .6f
    beginPath()
    moveTo(.4f, 16f)
    lineTo(.4f, 44f)
    stroke()
    // A rim of light on the bow's upper-left edge.
    strokeStyle = "rgba(255,252,235,.6)"
    lineWidth = .9f
    beginPath()
    arc(0f, 6f, 7.3f, PI_F * .95f, PI_F * 1.55f)
    stroke()
}

private val Teeth =
    floatArrayOf(
        5.3f,
        26f,
        3.4f,
        28.6f,
        5.7f,
        31.2f,
        3.4f,
        33.8f,
        5.1f,
        36.3f,
        3.4f,
        38.8f,
        4.6f,
        41.2f,
        3.4f,
        43.4f,
    )

private fun Canvas2D.drawFob(lift: Float, dark: Boolean, u: Float, px: Float) {
    strokeStyle = "#8f979e"
    lineWidth = 1.5f
    beginPath()
    arc(0f, 3.3f, 3.1f, 0f, TAU)
    stroke()
    rr(-10f, 6.5f, 20f, 32f, 7f)
    var g = createLinearGradient(-10f, 0f, 10f, 0f)
    g.addColorStop(0f, "#3a3e44")
    g.addColorStop(.3f, "#25282c")
    g.addColorStop(1f, "#0d0e10")
    save()
    shadowOn(lift, dark, u, px)
    fillStyle = g
    fill()
    restore()
    // Glossy plastic: one soft highlight down the lit side.
    g = createLinearGradient(-8f, 0f, -2f, 0f)
    g.addColorStop(0f, "rgba(255,255,255,.28)")
    g.addColorStop(1f, "rgba(255,255,255,0)")
    rr(-9f, 8f, 6f, 28f, 3f)
    fillStyle = g
    fill()
    for (lock in booleanArrayOf(true, false)) {
        val by = if (lock) 17f else 28.5f
        fillStyle = "#17191c"
        beginPath()
        arc(0f, by, 4.4f, 0f, TAU)
        fill()
        strokeStyle = "rgba(255,255,255,.18)"
        lineWidth = .6f
        stroke()
        strokeStyle = "rgba(220,226,232,.75)"
        lineWidth = .7f
        // strokeRect, which leaves the path alone; the next shape begins a new one anyway.
        beginPath()
        rect(-1.6f, by - .4f, 3.2f, 2.4f)
        stroke()
        beginPath()
        arc(0f, by - .4f, 1.2f, PI_F, if (lock) 0f else -.5f)
        stroke()
    }
}

private fun Canvas2D.drawTag(tag: KeyItem.Tag, lift: Float, dark: Boolean, u: Float, px: Float) {
    rr(-2.8f, -1.5f, 5.6f, 11f, 2f)
    fillStyle = tag.strap
    fill()
    rr(-10f, 7f, 20f, 34f, 5f)
    val g = createLinearGradient(-10f, 7f, 10f, 41f)
    g.addColorStop(0f, tag.lit)
    g.addColorStop(.5f, tag.color)
    g.addColorStop(1f, tag.deep)
    save()
    shadowOn(lift, dark, u, px)
    fillStyle = g
    fill()
    restore()
    setLineDash(TagDash)
    strokeStyle = "rgba(255,238,215,.55)"
    lineWidth = .6f
    rr(-7.6f, 9.4f, 15.2f, 29.2f, 3.4f)
    stroke()
    setLineDash(NoDash)
    font = "800 8.6px $TagFont"
    textAlign = "center"
    textBaseline = "middle"
    fillStyle = "rgba(255,240,220,.28)"
    fillText(tag.text, 0f, 25.6f)
    fillStyle = "rgba(0,0,0,.32)"
    fillText(tag.text, 0f, 25f)
}

private val TagDash = floatArrayOf(1.6f, 1.4f)

private fun Canvas2D.drawBobber(lift: Float, dark: Boolean, u: Float, px: Float) {
    strokeStyle = "#8f979e"
    lineWidth = 1.1f
    beginPath()
    arc(0f, 2.6f, 2.4f, 0f, TAU)
    moveTo(0f, 5f)
    lineTo(0f, 7.5f)
    stroke()
    save()
    shadowOn(lift, dark, u, px)
    fillStyle = "#d23a2f"
    beginPath()
    arc(0f, 17.5f, 10.5f, 0f, TAU)
    fill()
    restore()
    save()
    beginPath()
    arc(0f, 17.5f, 10.5f, 0f, TAU)
    clip()
    fillStyle = "#f4f0e6"
    fillRect(-11f, 17.5f, 22f, 12f)
    val g = createRadialGradient(-4f, 13f, .5f, 0f, 17.5f, 11.5f)
    g.addColorStop(0f, "rgba(255,255,255,.65)")
    g.addColorStop(.35f, "rgba(255,255,255,0)")
    g.addColorStop(.8f, "rgba(0,0,0,.12)")
    g.addColorStop(1f, "rgba(0,0,0,.35)")
    fillStyle = g
    fillRect(-11f, 6f, 22f, 23f)
    restore()
    strokeStyle = "rgba(40,20,10,.35)"
    lineWidth = .7f
    beginPath()
    moveTo(-10.5f, 17.5f)
    lineTo(10.5f, 17.5f)
    stroke()
    fillStyle = "#e9e3d6"
    fillRect(-.8f, 27.8f, 1.6f, 3.4f)
}

private fun Canvas2D.drawRing(state: KeyRackState, r: KeyRing, dark: Boolean, px: Float) {
    val g0 = checkNotNull(state.geometry)
    val u = g0.u
    val uk = u * g0.ks
    val rad = state.ringRadius
    val cx = state.ringCenterX(r)
    val cy = state.ringCenterY(r)
    val slots = r.slots
    val scale = 1f + .05f * r.lift
    save()
    translate(r.x, r.y)
    scale(scale, scale)
    translate(-r.x, -r.y)
    val items = r.holder.items
    for (j in items.indices) {
        val a = r.th + slots[j]
        save()
        translate(cx + sin(a) * rad * .92f, cy + cos(a) * rad * .92f)
        rotate(-r.ph[j])
        scale(uk, uk)
        when (val item = items[j]) {
            is KeyItem.Key -> {
                scale(item.size, item.size)
                drawKey(item.metal, r.lift, dark, u, px)
            }
            KeyItem.Fob -> drawFob(r.lift, dark, u, px)
            is KeyItem.Tag -> drawTag(item, r.lift, dark, u, px)
            KeyItem.Bobber -> drawBobber(r.lift, dark, u, px)
        }
        restore()
    }
    // The split ring: a double coil of spring steel.
    lineWidth = 2.3f * uk
    strokeStyle = "#7f878f"
    beginPath()
    arc(cx, cy, rad, 0f, TAU)
    stroke()
    lineWidth = .55f * uk
    strokeStyle = "rgba(20,24,28,.5)"
    beginPath()
    arc(cx, cy, rad - .2f * uk, 0f, TAU)
    stroke()
    lineWidth = .9f * uk
    strokeStyle = "#f2f5f7"
    beginPath()
    arc(cx, cy, rad + .5f * uk, PI_F * .95f, PI_F * 1.6f)
    stroke()
    restore()
}

// The hook's curl from its left side, down round the bottom, and up to the ball tip.
private fun Canvas2D.hookPath(x: Float, y: Float, front: Boolean, u: Float) {
    val r = 5f * u
    beginPath()
    if (front) {
        arc(x, y - r, r, PI_F * .5f, 0f, true)
        lineTo(x + r, y - 12f * u)
        return
    }
    moveTo(x - r, y - 24f * u)
    lineTo(x - r, y - r)
    arc(x, y - r, r, PI_F, 0f, true)
    lineTo(x + r, y - 12f * u)
}

private fun Canvas2D.drawHookFront(x: Float, y: Float, u: Float) {
    tube(2.8f * u, u) { hookPath(x, y, true, u) }
    val bx = x + 5f * u
    val by = y - 12.5f * u
    val g = createRadialGradient(bx - 1f * u, by - 1f * u, 0f, bx, by, 2.8f * u)
    g.addColorStop(0f, "#fff6d6")
    g.addColorStop(.5f, "#c99c45")
    g.addColorStop(1f, "#5e400c")
    fillStyle = g
    beginPath()
    arc(bx, by, 2.5f * u, 0f, TAU)
    fill()
}

private fun Canvas2D.dPath(g0: RackGeometry, front: Boolean) {
    val u = g0.u
    val x = g0.carryX
    val y = g0.cardBottom + 6f * u
    val rx = 8f * u
    val ry = 13f * u
    beginPath()
    if (front) {
        ellipse(x, y, rx, ry, 0f, PI_F * .66f, PI_F * .34f, true)
        return
    }
    moveTo(x - rx, y)
    lineTo(x + rx, y)
    ellipse(x, y, rx, ry, 0f, 0f, PI_F)
}

private fun Canvas2D.screw(x: Float, y: Float, r: Float, a: Float, u: Float) {
    val g = createRadialGradient(x - r * .4f, y - r * .5f, 0f, x, y, r * 1.2f)
    g.addColorStop(0f, "#f4f6f8")
    g.addColorStop(.55f, "#9aa3ab")
    g.addColorStop(1f, "#4b5258")
    fillStyle = g
    beginPath()
    arc(x, y, r, 0f, TAU)
    fill()
    strokeStyle = "rgba(25,28,32,.7)"
    lineWidth = .7f * u
    beginPath()
    moveTo(x - cos(a) * r * .8f, y - sin(a) * r * .8f)
    lineTo(x + cos(a) * r * .8f, y + sin(a) * r * .8f)
    stroke()
}
