package dev.dimvlachos.lab.core.presentation.components.conch

import androidx.compose.ui.graphics.Color
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// The conch, ported line by line from the prototype's drawConch() and draw(): the same calls, in
// the same order, with the same numbers. Constant colours keep their CSS strings; the ones that
// move with the voice or the cancel are made as Colors, so no new string is parsed every frame.

/**
 * The unit shell, about 1.45 tall, seen from the front: one queen-conch outline. A pointed spire
 * up-left with knobs on the shoulder; the shoulder line runs on into the lip, which flares up and
 * out into a broad wing with a gently wavy free edge and sweeps back down into the siphonal canal.
 */
private val Shell =
    floatArrayOf(
        -.22f,
        -.71f,
        -.12f,
        -.57f,
        0f,
        -.49f,
        .12f,
        -.5f,
        .24f,
        -.62f,
        .38f,
        -.7f,
        .53f,
        -.63f,
        .6f,
        -.5f,
        .61f,
        -.35f,
        .69f,
        -.2f,
        .62f,
        -.03f,
        .68f,
        .13f,
        .59f,
        .29f,
        .54f,
        .43f,
        .4f,
        .54f,
        .26f,
        .64f,
        .12f,
        .72f,
        .02f,
        .76f,
        -.08f,
        .6f,
        -.22f,
        .38f,
        -.33f,
        .1f,
        -.37f,
        -.18f,
        -.32f,
        -.44f,
    )

/** The lip's free edge: [Shell] points [Free0] to [Free1], shoulder to canal. */
private const val Free0 = 3
private const val Free1 = 17

/** The body whorl inside the outline, only used to clip its sutures and axial lines. */
private val Body =
    floatArrayOf(
        -.22f,
        -.71f,
        -.12f,
        -.57f,
        0f,
        -.49f,
        .14f,
        -.41f,
        .2f,
        -.2f,
        .16f,
        .2f,
        .08f,
        .6f,
        .02f,
        .74f,
        -.08f,
        .6f,
        -.22f,
        .38f,
        -.33f,
        .1f,
        -.37f,
        -.18f,
        -.32f,
        -.44f,
    )

private const val MouthX = .1f
private const val MouthY = .14f
internal const val Tilt0 = -.42f

private val Sutures =
    arrayOf(
        floatArrayOf(-.34f, -.38f, -.08f, -.46f, .16f, -.37f),
        floatArrayOf(-.3f, -.52f, -.12f, -.58f, .02f, -.5f),
        floatArrayOf(-.26f, -.63f, -.17f, -.66f, -.1f, -.6f),
    )
private val AxialX = floatArrayOf(-.28f, -.19f, -.1f, -.01f)
private val GrowthLines = floatArrayOf(.9f, .2f, .8f, .15f, .69f, .1f)
private val Knobs =
    floatArrayOf(
        -.24f,
        -.4f,
        .045f,
        -.12f,
        -.45f,
        .055f,
        .01f,
        -.45f,
        .06f,
        -.18f,
        -.56f,
        .035f,
        -.07f,
        -.57f,
        .035f,
    )

private fun Canvas2D.smooth(p: FloatArray) {
    beginPath()
    val n = p.size / 2
    fun mx(i: Int) = (p[(i % n) * 2] + p[((i + 1) % n) * 2]) / 2f
    fun my(i: Int) = (p[(i % n) * 2 + 1] + p[((i + 1) % n) * 2 + 1]) / 2f
    moveTo(mx(0), my(0))
    for (i in 1..n) quadraticCurveTo(p[(i % n) * 2], p[(i % n) * 2 + 1], mx(i), my(i))
    closePath()
}

/**
 * The free edge as an open path, drawn exactly as [smooth] draws it, scaled by [f] toward the point
 * (.1, .08): f < 1 gives the growth lines and the inner side of the rolled rim.
 */
private fun Canvas2D.freeEdge(f: Float = 1f) {
    val p = Shell
    val n = p.size / 2
    fun tx(x: Float) = .1f + (x - .1f) * f
    fun ty(y: Float) = .08f + (y - .08f) * f
    fun mx(i: Int) = tx((p[i * 2] + p[((i + 1) % n) * 2]) / 2f)
    fun my(i: Int) = ty((p[i * 2 + 1] + p[((i + 1) % n) * 2 + 1]) / 2f)
    beginPath()
    moveTo(mx(Free0 - 1), my(Free0 - 1))
    for (i in Free0..Free1) quadraticCurveTo(tx(p[i * 2]), ty(p[i * 2 + 1]), mx(i), my(i))
}

private fun Canvas2D.slit() {
    beginPath()
    moveTo(.03f, -.25f)
    quadraticCurveTo(.19f, .12f, .07f, .6f)
    quadraticCurveTo(-.04f, .15f, .03f, -.25f)
    closePath()
}

/** The prototype's `mixv`/`mixc`: [a] toward [b] by [t], each channel rounded, at [alpha]. */
private fun mix(a: Int, b: Int, t: Float, alpha: Float = 1f): Color {
    fun ch(sh: Int) =
        lerp((a shr sh and 255).toFloat(), (b shr sh and 255).toFloat(), t).roundToInt()
    return Color(ch(16), ch(8), ch(0), (alpha.coerceIn(0f, 1f) * 255f).toInt())
}

private fun rgba(r: Int, g: Int, b: Int, a: Float) =
    Color(r, g, b, (a.coerceIn(0f, 1f) * 255f).toInt())

/**
 * Contact shadow below a shell of size [s] centred at [y]: how far below it lies. When lifted it
 * stays above the composer ([floor]), so it lands in the stage and not across the text field.
 */
internal fun contactShadowY(y: Float, s: Float, lift: Float, floor: Float): Float {
    val sr = contactShadowR(s, lift)
    val sy = s * (.52f + lift * .3f)
    if (floor.isNaN()) return sy
    return lerp(sy, min(sy, max(0f, floor - y - sr * .28f - 4f)), lift.coerceIn(0f, 1f))
}

/** The contact shadow's radius, squashed to .28 of it tall. */
internal fun contactShadowR(s: Float, lift: Float) = s * (.44f + lift * .12f)

/**
 * Paints the shell centred at ([x], [y]), [s] tall, in dp under a canvas scaled by [density].
 * [glow] 0..1 lights the inside, [level] 0..1 ripples the wavy lines, [cool] 0..1 greys it toward
 * cancel; [t] is the time in seconds, [lift] how far it is lifted and [floor] the composer's top.
 */
internal fun Canvas2D.drawConch(
    x: Float,
    y: Float,
    s: Float,
    dark: Boolean,
    density: Float,
    glow: Float = 0f,
    level: Float = 0f,
    cool: Float = 0f,
    t: Float = 0f,
    alpha: Float = 1f,
    shadow: Boolean = true,
    lift: Float = 0f,
    tilt: Float = 0f,
    floor: Float = Float.NaN,
) {
    val k = s / 1.45f
    save()
    globalAlpha = alpha
    translate(x, y)
    // Soft contact shadow.
    if (shadow) {
        val sy = contactShadowY(y, s, lift, floor)
        val sr = contactShadowR(s, lift)
        val g = createRadialGradient(0f, sy, 0f, 0f, sy, sr)
        g.addColorStop(0f, rgba(40, 20, 10, (if (dark) .55f else .3f) * (1f - lift * .55f)))
        g.addColorStop(1f, "rgba(40,20,10,0)")
        save()
        translate(0f, sy)
        scale(1f, .28f)
        translate(0f, -sy)
        fillStyle = g
        beginPath()
        arc(0f, sy, sr, 0f, (PI * 2).toFloat())
        fill()
        restore()
    }
    rotate(tilt + Tilt0)
    scale(k, k)
    translate(-.12f, 0f)
    // One silhouette: body and lip share the outline, the fill and the drop shadow.
    var g = createLinearGradient(-.4f, -.6f, .3f, .7f)
    g.addColorStop(0f, mix(0xfbf2e1, 0xc4cfdb, cool))
    g.addColorStop(.5f, mix(0xe9cba3, 0x97a5b5, cool))
    g.addColorStop(1f, mix(0xb07a48, 0x5d6a79, cool))
    // The prototype's unitShadow(c, 'rgba(60,30,15,.28)', .035, -.012, .018): a shadow given in
    // shell units, so it scales and turns with the shell. A canvas shadow of blur B is a Gaussian
    // of B / 2; drawn as a blurred copy of the outline, offset in shell units.
    save()
    translate(-.012f, .018f)
    filter = "blur(${.035f * k * density / 2f}px)"
    fillStyle = "rgba(60,30,15,.28)"
    smooth(Shell)
    fill()
    filter = "none"
    restore()
    smooth(Shell)
    fillStyle = g
    fill()
    save()
    smooth(Shell)
    clip()
    // Spiral cords across the body whorl, fading out before the lip.
    g = createLinearGradient(-.42f, 0f, .14f, 0f)
    val cord = rgba(176, 104, 56, .2f * (1f - cool * .5f))
    g.addColorStop(0f, cord)
    g.addColorStop(.75f, cord)
    g.addColorStop(1f, "rgba(176,104,56,0)")
    strokeStyle = g
    lineWidth = .045f
    for (i in 0 until 5) {
        beginPath()
        moveTo(-.42f, -.28f + i * .2f)
        quadraticCurveTo(-.1f, -.16f + i * .2f, .2f, -.04f + i * .2f)
        stroke()
    }
    save()
    smooth(Body)
    clip()
    strokeStyle = "rgba(120,74,36,.22)"
    lineWidth = .012f
    for (gx in AxialX) {
        beginPath()
        moveTo(gx, -.36f)
        quadraticCurveTo(gx - .08f, .12f, gx + .06f, .62f)
        stroke()
    }
    // Spire sutures, clipped to the body so they never run past its outline.
    lineCap = "round"
    for (q in Sutures) {
        strokeStyle = "rgba(110,66,30,.5)"
        lineWidth = .02f
        beginPath()
        moveTo(q[0], q[1])
        quadraticCurveTo(q[2], q[3], q[4], q[5])
        stroke()
        strokeStyle = "rgba(255,248,232,.65)"
        lineWidth = .012f
        beginPath()
        moveTo(q[0], q[1] + .025f)
        quadraticCurveTo(q[2], q[3] + .025f, q[4], q[5] + .025f)
        stroke()
    }
    restore()
    // Lamp highlight on the upper left, shade toward the lower left.
    g = createRadialGradient(-.2f, -.32f, 0f, -.2f, -.32f, .5f)
    g.addColorStop(0f, "rgba(255,252,240,.8)")
    g.addColorStop(1f, "rgba(255,252,240,0)")
    fillStyle = g
    fillRect(-.6f, -.8f, 1.4f, 1.6f)
    g = createLinearGradient(-.4f, .1f, -.05f, .75f)
    g.addColorStop(0f, "rgba(70,40,15,0)")
    g.addColorStop(1f, "rgba(70,40,15,.32)")
    fillStyle = g
    fillRect(-.6f, -.8f, 1.4f, 1.6f)
    // The lip: the body's sand turns to a pale cream as the shell flares out to the right...
    val lipA = 0xf8e6d2
    val lipB = 0xbcc6d3
    val rimA = 0xe9c7a6
    val rimB = 0x9eabbb
    g = createLinearGradient(.06f, 0f, .34f, 0f)
    g.addColorStop(0f, mix(lipA, lipB, cool, 0f))
    g.addColorStop(1f, mix(lipA, lipB, cool, .92f))
    fillStyle = g
    fillRect(-.6f, -.8f, 1.4f, 1.6f)
    // ...and the pink lives in the aperture and the inner lip, fading out toward the rim and the
    // back.
    save()
    translate(.2f, .06f)
    scale(1f, 1.5f)
    g = createRadialGradient(0f, 0f, 0f, 0f, 0f, .44f)
    g.addColorStop(0f, mix(0xec8f84, 0xc39ba4, cool, 1f))
    g.addColorStop(.5f, mix(0xf5bba7, 0xbba9b6, cool, .9f))
    g.addColorStop(1f, mix(0xf5bba7, 0xbba9b6, cool, 0f))
    fillStyle = g
    fillRect(-.6f, -.6f, 1.2f, 1.2f)
    restore()
    // Growth lines running parallel to the lip's edge.
    lineWidth = .01f
    for (i in 0 until 3) {
        strokeStyle = mix(rimA, rimB, cool, GrowthLines[i * 2 + 1] * 2.2f)
        freeEdge(GrowthLines[i * 2])
        stroke()
    }
    // Gloss across the upper wing.
    g = createRadialGradient(.4f, -.44f, 0f, .4f, -.44f, .36f)
    g.addColorStop(0f, "rgba(255,250,244,.7)")
    g.addColorStop(1f, "rgba(255,250,244,0)")
    fillStyle = g
    fillRect(-.1f, -.8f, .9f, 1.6f)
    if (glow > .01f) {
        globalCompositeOperation = "lighter"
        g = createRadialGradient(MouthX, MouthY, 0f, MouthX, MouthY, .5f)
        g.addColorStop(0f, rgba(255, 190, 170, .45f * glow))
        g.addColorStop(1f, "rgba(255,170,150,0)")
        fillStyle = g
        fillRect(-.2f, -.8f, .9f, 1.6f)
        globalCompositeOperation = "source-over"
    }
    // A thick rolled rim along the free edge: a sand band in shade with a glossy crest inside it.
    lineCap = "round"
    lineJoin = "round"
    strokeStyle = mix(rimA, rimB, cool, .85f)
    lineWidth = .075f
    freeEdge()
    stroke()
    strokeStyle = mix(lipA, lipB, cool, 1f)
    lineWidth = .03f
    freeEdge(.965f)
    stroke()
    strokeStyle = "rgba(255,252,246,.85)"
    lineWidth = .012f
    freeEdge(.955f)
    stroke()
    restore()
    // Knobs on the shoulder: little blunt spikes, lit on their upper left.
    for (i in 0 until 5) {
        val kx = Knobs[i * 3]
        val ky = Knobs[i * 3 + 1]
        val kr = Knobs[i * 3 + 2]
        beginPath()
        moveTo(kx - kr, ky + kr * .6f)
        quadraticCurveTo(kx - kr * .5f, ky - kr * 1.6f, kx, ky - kr * 1.7f)
        quadraticCurveTo(kx + kr * .5f, ky - kr * 1.6f, kx + kr, ky + kr * .6f)
        closePath()
        g = createLinearGradient(kx - kr, ky - kr * 1.7f, kx + kr, ky + kr)
        g.addColorStop(0f, "#fffaf0")
        g.addColorStop(.55f, mix(0xe4c296, 0x97a5b5, cool))
        g.addColorStop(1f, mix(0x9c6a3c, 0x5d6a79, cool))
        fillStyle = g
        fill()
    }
    // The mouth: a deep pink slit along the inner side of the lip, lit from inside by the voice.
    slit()
    g = createLinearGradient(.02f, -.2f, .12f, .55f)
    g.addColorStop(0f, mix(0xd8636a, 0xa87c88, cool))
    g.addColorStop(.5f, mix(0xa83748, 0x6f5563, cool))
    g.addColorStop(1f, mix(0xc4525c, 0x8f6c7a, cool))
    fillStyle = g
    fill()
    if (glow > .01f) {
        save()
        slit()
        clip()
        globalCompositeOperation = "lighter"
        g = createRadialGradient(MouthX, MouthY, 0f, MouthX, MouthY, .42f)
        g.addColorStop(0f, rgba(255, 236, 220, .9f * glow))
        g.addColorStop(.5f, rgba(255, 150, 140, .5f * glow))
        g.addColorStop(1f, "rgba(255,120,120,0)")
        fillStyle = g
        fillRect(-.1f, -.3f, .4f, 1f)
        lineWidth = .012f
        for (i in 0 until 2) {
            strokeStyle = rgba(255, 244, 236, (.3f + .6f * level) * glow)
            beginPath()
            for (j in 0..22) {
                val v = j / 22f
                val yy = -.2f + v * .76f
                val sv = sin(v * PI.toFloat())
                val base = .03f + sv * (.065f + i * .025f)
                val xx = base + sin(v * 10f + t * 7f + i * 2f) * (.006f + .024f * level) * sv
                if (j == 0) moveTo(xx, yy) else lineTo(xx, yy)
            }
            stroke()
        }
        restore()
    }
    save()
    slit()
    strokeStyle = "rgba(255,236,226,.55)"
    lineWidth = .012f
    stroke()
    restore()
    // One outline around the whole shell.
    save()
    smooth(Shell)
    strokeStyle = "rgba(90,52,22,.3)"
    lineWidth = .014f
    stroke()
    restore()
    restore()
}

/**
 * The prototype's fx canvas over the whole screen, [width] by [height] dp: the calm wash and the
 * lamp pool while the shell is up, the rings from its mouth (kept above the composer and gone once
 * it flies), the shell itself (in its slot, lifted, or flying), and the sand of a cancel. [scrim]
 * is the screen's background.
 */
internal fun Canvas2D.drawConchStage(
    st: ConchState,
    width: Float,
    dark: Boolean,
    scrim: Color,
    density: Float,
) {
    val t = st.time / 1000f
    val lifted = st.shellP.coerceIn(0f, 1f)
    val fly = st.fly
    val top = st.composerTop
    // A calm wash over the chat while the shell is up, so it has room to glow.
    if (lifted > .01f || fly != null) {
        val a = if (fly != null) max(0f, 1f - (st.time - fly.t0) / fly.ms) else lifted
        val g = createLinearGradient(0f, top, 0f, top - 380f)
        g.addColorStop(0f, scrim.copy(alpha = .96f * a))
        g.addColorStop(.75f, scrim.copy(alpha = .9f * a))
        g.addColorStop(1f, scrim.copy(alpha = 0f))
        fillStyle = g
        fillRect(0f, top - 380f, width, 380f)
        // Warm lamp pool behind the shell.
        val bx = st.stageX
        val by = st.stageY
        val bs = st.stageSize
        val lg = createRadialGradient(bx, by - bs * .1f, 0f, bx, by, bs * 1.3f)
        lg.addColorStop(0f, rgba(255, 214, 170, (if (dark) .22f else .5f) * a))
        lg.addColorStop(1f, "rgba(255,214,170,0)")
        fillStyle = lg
        fillRect(bx - bs * 1.4f, by - bs * 1.4f, bs * 2.8f, bs * 2.8f)
    }
    val cool = st.cool
    val tilt = st.off * -.0025f
    // Rings spreading from the mouth, kept in the stage above the composer, and gone once the
    // shell flies.
    if (st.rings.isNotEmpty() && fly == null) {
        val s = st.shellS
        val k = s / 1.45f
        val ang = tilt + Tilt0
        val ax = (MouthX - .12f) * k
        val ay = MouthY * k
        val mx = st.shellX + ax * cos(ang) - ay * sin(ang)
        val my = st.shellY + ax * sin(ang) + ay * cos(ang)
        save()
        beginPath()
        rect(0f, 0f, width, top)
        clip()
        globalCompositeOperation = if (dark) "lighter" else "source-over"
        for (i in st.rings.indices) {
            val r = st.rings[i]
            val p = (st.time - r.t0) / 1300f
            val rad = s * (.18f + p * .9f)
            val a = (1f - p) * .85f * max(.5f, r.a) * lifted
            strokeStyle = if (dark) rgba(255, 196, 186, a) else rgba(214, 104, 108, a)
            lineWidth = 3f * (1f - p) + .8f
            beginPath()
            arc(mx, my, rad, -1.25f, 1.25f)
            stroke()
        }
        restore()
    }
    if (fly != null) {
        val p = st.flyP
        drawConch(
            st.flyX,
            st.flyY,
            st.flyS,
            dark,
            density,
            glow = .3f * (1f - p),
            lift = 1f - p,
            t = t,
            tilt = p * .4f,
            floor = top,
        )
    } else if (st.mode == ConchMode.Idle && lifted < .01f) {
        val g = st.regrow.coerceIn(0f, 1.2f)
        drawConch(
            st.slotCenterX,
            st.slotCenterY,
            st.slotSize * g,
            dark,
            density,
            glow = .05f,
            t = t,
            alpha = (st.regrow * 1.5f).coerceIn(0f, 1f),
        )
    } else {
        val rec = st.mode == ConchMode.Rec
        drawConch(
            st.shellX,
            st.shellY,
            st.shellS,
            dark,
            density,
            glow = if (rec) (.3f + .7f * st.levelS) * (1f - cool * .8f) else .1f * lifted,
            level = st.levelS,
            cool = cool,
            t = t,
            lift = lifted,
            tilt = tilt,
            floor = top,
        )
    }
    // Sand puffs on cancel.
    for (i in st.puffs.indices) {
        val p = st.puffs[i]
        val a = .7f * (1f - (st.time - p.t0) / p.life)
        fillStyle = if (dark) rgba(220, 196, 160, a) else rgba(196, 160, 112, a)
        beginPath()
        arc(p.x, p.y, p.r, 0f, (PI * 2).toFloat())
        fill()
    }
}

/** The little shell in a voice-note bubble's 40 × 44 canvas, glowing with [note] as it plays. */
internal fun Canvas2D.drawBubbleShell(note: VoiceNote, time: Float, dark: Boolean, density: Float) {
    val t = time / 1000f
    val lv = if (note.playing) note.levelAt(note.pos) else 0f
    drawConch(
        20f,
        23f,
        ConchState.FlySize,
        dark,
        density,
        glow = if (note.playing) .3f + .7f * lv else .08f,
        level = lv,
        t = t,
        shadow = false,
        tilt = note.bounce * .08f + if (note.playing) sin(t * 5f) * .03f else 0f,
    )
}
