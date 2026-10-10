package dev.dimvlachos.lab.core.presentation.components.popup

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import dev.dimvlachos.lab.core.presentation.components.perspective.Homography
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sin

/** Every bitmap one frame draws, painted for one size of the book. */
internal class BookArt(
    /** Bitmap pixels per book unit. */
    val quality: Float,
    val fronts: List<ImageBitmap>,
    val backs: List<ImageBitmap?>,
    val pieces: List<List<ImageBitmap>>,
    /** A piece's plain paper back: its art's shape, filled with this. */
    val paperBack: Color,
    val shadows: List<List<ImageBitmap?>>,
    val shadowPad: Float,
)

/**
 * Draws a pop-up book a frame at a time: leaves, the pieces of the spreads that are open, their
 * shadows and tabs, each flat sheet under its own projection.
 */
internal class PopUpPainter {
    /** Paints the art for a book seen by [camera]. */
    fun prepare(
        cover: Painter,
        spreads: List<PopUpSpread>,
        camera: BookCamera,
        density: Density,
        paperBack: Color,
    ): BookArt {
        val q = (camera.pxPerUnit * ArtSharpness).coerceIn(1f, MaxQuality)
        fun raster(painter: Painter, w: Float, h: Float) =
            PopUpArt.raster(painter, ceil(w * q).toInt(), ceil(h * q).toInt(), density)
        val n = spreads.size
        // Leaf j's front is the near page of spread j - 1 (the cover first); its back the far page
        // of spread j. The board is the last spread's near page and has no back.
        val fronts =
            List(n + 1) { j ->
                val painter = if (j == 0) cover else spreads[j - 1].near
                raster(painter, leafWidth(j, n), leafDepth(j, n))
            }
        val backs =
            List(n + 1) { j ->
                if (j < n) raster(spreads[j].far, leafWidth(j, n), leafDepth(j, n)) else null
            }
        val pieces = spreads.map { s -> s.pieces.map { raster(it.art, it.width, it.height) } }
        val blur = PopUpDimens.ShadowBlur * q
        val shadows = spreads.mapIndexed { s, spread ->
            spread.pieces.mapIndexed { i, piece ->
                if (piece.castsShadow) PopUpArt.softShadow(pieces[s][i], blur) else null
            }
        }
        return BookArt(q, fronts, backs, pieces, paperBack, shadows, (blur.toInt() + 1).toFloat())
    }

    fun draw(
        scope: DrawScope,
        state: PopUpBookState,
        spreads: List<PopUpSpread>,
        camera: BookCamera,
        art: BookArt,
        tab: Color,
        tabLine: Color,
    ) {
        val angles = state.angles
        drawTable(scope, camera, angles)
        // Room for every leaf, the board and every spread.
        if (order.size < 2 * angles.size + 1) order = IntArray(2 * angles.size + 1)
        val count = PopUpMath.drawOrder(angles, order)
        for (i in 0 until count) {
            val code = order[i]
            if (code < PopUpMath.Spread) drawLeaf(scope, code, angles, camera, art)
            else
                drawSpread(
                    scope,
                    code - PopUpMath.Spread,
                    state,
                    spreads,
                    camera,
                    art,
                    tab,
                    tabLine,
                )
        }
    }

    // A soft dark patch on the table under the book.
    private fun drawTable(scope: DrawScope, camera: BookCamera, angles: FloatArray) {
        val w = PopUpDimens.BoardWidth / 2f + TableMargin
        val near = -PopUpDimens.BoardDepth - TableMargin
        val far = if (angles.any { it > 0f }) PopUpDimens.BoardDepth + TableMargin else TableMargin
        path.reset()
        moveTo(camera.project(Vec3(-w, near, 0f)))
        lineTo(camera.project(Vec3(w, near, 0f)))
        lineTo(camera.project(Vec3(w, far, 0f)))
        lineTo(camera.project(Vec3(-w, far, 0f)))
        path.close()
        scope.drawPath(path, Color.Black, alpha = TableAlpha)
    }

    private fun drawLeaf(
        scope: DrawScope,
        j: Int,
        angles: FloatArray,
        camera: BookCamera,
        art: BookArt,
    ) {
        val n = angles.size
        val angle = if (j < n) angles[j] else 0f
        val front = j == n || PopUpMath.leafFrontSeen(angle)
        val image = (if (front) art.fronts[j] else art.backs[j]) ?: return
        val w = leafWidth(j, n)
        val d = leafDepth(j, n)
        val a = PopUpMath.across(angle)
        val du = w / image.width
        val dv = d / image.height
        // The front has the gutter along the top of its image, the back along the bottom.
        if (front) {
            camera.plane(-w / 2f, 0f, 0f, du, 0f, 0f, 0f, a.y * dv, a.z * dv, h)
        } else {
            camera.plane(-w / 2f, a.y * d, a.z * d, du, 0f, 0f, 0f, -a.y * dv, -a.z * dv, h)
        }
        val normal = PopUpMath.leafNormal(angle).let { if (front) it else -it }
        drawSheet(scope, image, shade(PopUpMath.brightness(normal)))
    }

    private fun drawSpread(
        scope: DrawScope,
        s: Int,
        state: PopUpBookState,
        spreads: List<PopUpSpread>,
        camera: BookCamera,
        art: BookArt,
        tab: Color,
        tabLine: Color,
    ) {
        val angles = state.angles
        val spread = spreads[s]
        val far = angles[s]
        val near = if (s + 1 < angles.size) angles[s + 1] else 0f
        val pieces = spread.pieces
        val fade = PopUpMath.shadowFade(near, far)
        if (fade > 0f) {
            clipToSpread(camera, near, far, s, angles.size)
            for (i in pieces.indices) {
                val shadow = art.shadows[s][i] ?: continue
                drawShadow(scope, pieces[i], spread, shadow, s, state, near, far, camera, art, fade)
            }
        }
        if (spread.tab != null)
            drawTab(scope, spread.tab, state.tabTravel[s], near, camera, tab, tabLine)
        // Pieces far to near.
        val count = pieces.size
        if (depthOrder.size < count) {
            depthOrder = IntArray(count)
            depths = FloatArray(count)
        }
        for (i in 0 until count) {
            depthOrder[i] = i
            val piece = pieces[i]
            depths[i] =
                camera.nearness(PopUpMath.base(piece, near, far, piece.x + piece.width / 2f))
        }
        for (i in 1 until count) {
            val k = depthOrder[i]
            var m = i - 1
            while (m >= 0 && depths[depthOrder[m]] > depths[k]) {
                depthOrder[m + 1] = depthOrder[m]
                m--
            }
            depthOrder[m + 1] = k
        }
        val frontSeen = PopUpMath.pieceFrontSeen(near, far)
        val normal = PopUpMath.pieceNormal(near, far).let { if (frontSeen) it else -it }
        val brightness = PopUpMath.brightness(normal)
        // Seen from behind, a piece is its own shape in plain paper.
        val filter = if (frontSeen) shade(brightness) else paperShade(art.paperBack, brightness)
        for (o in 0 until count) {
            val i = depthOrder[o]
            val piece = pieces[i]
            val image = art.pieces[s][i]
            piecePlane(piece, spread, s, state, near, far, camera, image, pad = 0f)
            val spins = piece.motion as? PieceMotion.Spins
            if (spins == null) {
                drawSheet(scope, image, filter)
            } else {
                Homography.toMatrix(h, matrix)
                scope.withTransform({ transform(matrix) }) {
                    rotateRad(
                        state.sailAngle[s],
                        Offset(
                            spins.pivotX * image.width / piece.width,
                            spins.pivotY * image.height / piece.height,
                        ),
                    ) {
                        drawImage(image, colorFilter = filter)
                    }
                }
            }
        }
    }

    // Writes into [h] the plane of [piece]'s art (padded by [pad] bitmap pixels on every side).
    private fun piecePlane(
        piece: PopUpPiece,
        spread: PopUpSpread,
        s: Int,
        state: PopUpBookState,
        near: Float,
        far: Float,
        camera: BookCamera,
        image: ImageBitmap,
        pad: Float,
    ) {
        place(piece, spread, s, state)
        val lift = placedLift
        val up = PopUpMath.up(near, far)
        val base = PopUpMath.base(piece, near, far, placedX)
        val du = piece.width / (image.width - 2f * pad)
        val dv = piece.height / (image.height - 2f * pad)
        val top = lift + piece.height
        camera.plane(
            base.x - pad * du,
            base.y + up.y * (top + pad * dv),
            base.z + up.z * (top + pad * dv),
            du,
            0f,
            0f,
            0f,
            -up.y * dv,
            -up.z * dv,
            h,
        )
    }

    // Where along the gutter [piece] is, and how far it is lifted, as its tab and the time say:
    // into [placedX] and [placedLift].
    private fun place(piece: PopUpPiece, spread: PopUpSpread, s: Int, state: PopUpBookState) {
        val rides = piece.motion as? PieceMotion.RidesTab
        if (rides == null) {
            placedX = piece.x
            placedLift = piece.lift
            return
        }
        val travel = spread.tab?.travel?.takeIf { it > 0f } ?: 1f
        val t = (state.tabTravel[s] / travel).coerceIn(0f, 1f)
        placedX = rides.from + (rides.to - rides.from) * t
        placedLift = piece.lift + rides.bob * sin(PopUpDimens.BobRate * state.time)
    }

    private var placedX = 0f
    private var placedLift = 0f

    private fun drawShadow(
        scope: DrawScope,
        piece: PopUpPiece,
        spread: PopUpSpread,
        shadow: ImageBitmap,
        s: Int,
        state: PopUpBookState,
        near: Float,
        far: Float,
        camera: BookCamera,
        art: BookArt,
        fade: Float,
    ) {
        // The plane's origin and axes in book space, cast along the light onto the page.
        val pageAngle = if (piece.side == PopUpSide.Near) near else far
        place(piece, spread, s, state)
        val lift = placedLift
        val up = PopUpMath.up(near, far)
        val base = PopUpMath.base(piece, near, far, placedX)
        val du = piece.width / (shadow.width - 2f * art.shadowPad)
        val dv = piece.height / (shadow.height - 2f * art.shadowPad)
        val top = lift + piece.height + art.shadowPad * dv
        val origin = Vec3(base.x - art.shadowPad * du, base.y + up.y * top, base.z + up.z * top)
        val o = PopUpMath.ontoPage(origin, pageAngle)
        val u = PopUpMath.ontoPage(origin + Vec3(du, 0f, 0f), pageAngle) - o
        val v = PopUpMath.ontoPage(origin - up * dv, pageAngle) - o
        camera.plane(o, u, v, h)
        Homography.toMatrix(h, matrix)
        scope.withTransform({
            clipPath(clip)
            transform(matrix)
        }) {
            drawImage(shadow, alpha = PopUpDimens.ShadowAlpha * fade)
        }
    }

    // The two pages of spread [s], as the eye sees them: shadows fall only on paper.
    private fun clipToSpread(camera: BookCamera, near: Float, far: Float, s: Int, leaves: Int) {
        clip.reset()
        page(camera, near, leafWidth(s + 1, leaves), leafDepth(s + 1, leaves))
        page(camera, far, leafWidth(s, leaves), leafDepth(s, leaves))
    }

    private fun page(camera: BookCamera, angle: Float, w: Float, d: Float) {
        val a = PopUpMath.across(angle)
        val edge = Vec3(0f, a.y * d, a.z * d)
        clip.moveTo(camera.project(Vec3(-w / 2f, 0f, 0f)))
        clip.lineTo(camera.project(Vec3(w / 2f, 0f, 0f)))
        clip.lineTo(camera.project(Vec3(w / 2f, edge.y, edge.z)))
        clip.lineTo(camera.project(Vec3(-w / 2f, edge.y, edge.z)))
        clip.close()
    }

    // A paper strip out of the slit at the near page's outer edge, [travel] units out.
    private fun drawTab(
        scope: DrawScope,
        tab: PullTab,
        travel: Float,
        near: Float,
        camera: BookCamera,
        color: Color,
        line: Color,
    ) {
        val a = PopUpMath.across(near)
        val d0 = PopUpDimens.TabFromGutter
        val d1 = d0 + PopUpDimens.TabWidth
        val x0 = PopUpDimens.TabX - PopUpDimens.TabStub
        val x1 = PopUpDimens.TabX + PopUpDimens.TabStub + travel.coerceAtMost(tab.travel)
        path.reset()
        moveTo(camera.project(Vec3(x0, a.y * d0, a.z * d0)))
        lineTo(camera.project(Vec3(x1, a.y * d0, a.z * d0)))
        lineTo(camera.project(Vec3(x1, a.y * d1, a.z * d1)))
        lineTo(camera.project(Vec3(x0, a.y * d1, a.z * d1)))
        path.close()
        scope.drawPath(path, color)
        scope.drawPath(path, line, style = tabEdge)
    }

    private fun drawSheet(scope: DrawScope, image: ImageBitmap, filter: ColorFilter?) {
        Homography.toMatrix(h, matrix)
        scope.withTransform({ transform(matrix) }) { drawImage(image, colorFilter = filter) }
    }

    // Paper turned from the light, darkened; one filter per step of brightness, made once.
    private fun shade(brightness: Float): ColorFilter? {
        val dark = (1f - brightness) * PopUpDimens.ShadeAlpha
        val level = (dark * ShadeLevels).roundToInt().coerceIn(0, ShadeLevels)
        if (level == 0) return null
        return shades[level]
            ?: run {
                val k = 1f - level.toFloat() / ShadeLevels
                ColorFilter.lighting(Color(k, k, k), Color.Black).also { shades[level] = it }
            }
    }

    // Paper seen from behind, tinted plain and darkened as it turns from the light.
    private fun paperShade(paper: Color, brightness: Float): ColorFilter {
        val dark = (1f - brightness) * PopUpDimens.ShadeAlpha
        val level = (dark * ShadeLevels).roundToInt().coerceIn(0, ShadeLevels)
        val cached = backShades[level]
        if (cached != null && backPaper == paper) return cached
        if (backPaper != paper) {
            backShades.fill(null)
            backPaper = paper
        }
        val k = 1f - level.toFloat() / ShadeLevels
        val tint = Color(paper.red * k, paper.green * k, paper.blue * k, paper.alpha)
        return ColorFilter.tint(tint, BlendMode.SrcIn).also { backShades[level] = it }
    }

    private fun moveTo(p: Offset) = path.moveTo(p)

    private fun lineTo(p: Offset) = path.lineTo(p)

    private fun Path.moveTo(p: Offset) = moveTo(p.x, p.y)

    private fun Path.lineTo(p: Offset) = lineTo(p.x, p.y)

    private val h = FloatArray(9)
    private val matrix = Matrix()
    private val path = Path()
    private val clip = Path()
    private var order = IntArray(16)
    private var depthOrder = IntArray(16)
    private var depths = FloatArray(16)
    private val shades = arrayOfNulls<ColorFilter>(ShadeLevels + 1)
    private val backShades = arrayOfNulls<ColorFilter>(ShadeLevels + 1)
    private var backPaper = Color.Unspecified
    private val tabEdge = Stroke(width = 1f)

    private companion object {
        // Art is painted at about the size it is seen, and no sharper than this per book unit.
        const val ArtSharpness = 1.15f
        const val MaxQuality = 3f
        const val TableMargin = 8f
        const val TableAlpha = 0.18f
        const val ShadeLevels = 48
    }
}

// The cover and the back board are a little larger than the pages between them.
internal fun leafWidth(j: Int, leaves: Int): Float =
    if (j == 0 || j == leaves) PopUpDimens.BoardWidth else PopUpDimens.PageWidth

internal fun leafDepth(j: Int, leaves: Int): Float =
    if (j == 0 || j == leaves) PopUpDimens.BoardDepth else PopUpDimens.PageDepth
