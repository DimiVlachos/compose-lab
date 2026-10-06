package dev.dimvlachos.lab.core.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * One frame from a camera, as its sensor saw it: the top-left [width] by [height] pixels of [image]
 * (rows may be padded wider), to be turned [rotationDegrees] clockwise to stand upright, and
 * brightened [gain] times, for a dim room. With a [mask], only the person shows: the mask stands
 * upright, unmirrored, as large as it likes, opaque where the person is.
 */
internal class CameraFrame(
    val image: ImageBitmap,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val gain: Float = 1f,
    val mask: ImageBitmap? = null,
)

/**
 * Paints the latest [CameraFrame] upright and flipped left to right, as a mirror shows a face. A
 * new frame only redraws: the frame is read while drawing, never while composing, and the size
 * changes only with the resolution or the rotation.
 */
internal class MirrorFramePainter : Painter() {
    private var frame by mutableStateOf<CameraFrame?>(null)
    private var uprightSize by mutableStateOf(Size.Unspecified)
    private var colorFilter: ColorFilter? = null
    // One brightening filter per gain, reused while the gain holds.
    private var brighteningGain = 1f
    private var brightening: ColorFilter? = null

    /** Whether the frame showing has the person cut out of it. Read while drawing. */
    val cutOut: Boolean
        get() = frame?.mask != null

    /** Whether there is a frame to show. Changes only when frames start or stop coming. */
    var hasFrame by mutableStateOf(false)
        private set

    /** Shows [next] from the next draw on; `null` shows nothing. */
    fun show(next: CameraFrame?) {
        frame = next
        if (hasFrame != (next != null)) hasFrame = next != null
        if (next == null) return
        val size =
            if (next.rotationDegrees.isQuarterTurn())
                Size(next.height.toFloat(), next.width.toFloat())
            else Size(next.width.toFloat(), next.height.toFloat())
        if (size != uprightSize) uprightSize = size
    }

    override val intrinsicSize: Size
        get() = uprightSize

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun DrawScope.onDraw() {
        val frame = frame ?: return
        val brighten = brighteningFor(frame.gain)
        // The frame as the sensor has it, sized so that once turned it fills the upright box.
        val sensorSize =
            if (frame.rotationDegrees.isQuarterTurn())
                IntSize(size.height.roundToInt(), size.width.roundToInt())
            else IntSize(size.width.roundToInt(), size.height.roundToInt())
        val filter = colorFilter
        val mask = frame.mask
        if (mask != null) {
            // Only the person: the frame, then the mask keeping what it covers, in a layer of their
            // own, which the caller's filter tints as it lands.
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(Offset.Zero, size), Paint().apply { colorFilter = filter })
                drawTurned(frame, sensorSize, brighten)
                // Upright already: only the mirror's flip.
                withTransform({ scale(-1f, 1f) }) {
                    drawImage(
                        mask,
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                        blendMode = BlendMode.DstIn,
                        filterQuality = FilterQuality.Low,
                    )
                }
                canvas.restore()
            }
            return
        }
        withTransform({
            translate(size.width / 2, size.height / 2)
            // The mirror's flip, left to right as the frame will be seen, after it is turned.
            scale(-1f, 1f, pivot = Offset.Zero)
            rotate(frame.rotationDegrees.toFloat(), pivot = Offset.Zero)
        }) {
            if (brighten != null && filter != null) {
                // Brightened first, then the caller's filter (the fog's milky one) over the result.
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(
                        Rect(
                            -sensorSize.width / 2f,
                            -sensorSize.height / 2f,
                            sensorSize.width / 2f,
                            sensorSize.height / 2f,
                        ),
                        Paint().apply { colorFilter = filter },
                    )
                    drawFrame(frame, sensorSize, brighten)
                    canvas.restore()
                }
            } else {
                drawFrame(frame, sensorSize, brighten ?: filter)
            }
        }
    }

    private fun brighteningFor(gain: Float): ColorFilter? {
        if (gain <= 1.001f) return null
        if (gain != brighteningGain || brightening == null) {
            brighteningGain = gain
            brightening =
                ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(gain, gain, gain, 1f) })
        }
        return brightening
    }
}

// The frame turned upright and mirrored, filling the box.
private fun DrawScope.drawTurned(frame: CameraFrame, sensorSize: IntSize, filter: ColorFilter?) {
    withTransform({
        translate(size.width / 2, size.height / 2)
        scale(-1f, 1f, pivot = Offset.Zero)
        rotate(frame.rotationDegrees.toFloat(), pivot = Offset.Zero)
    }) {
        drawFrame(frame, sensorSize, filter)
    }
}

private fun DrawScope.drawFrame(frame: CameraFrame, sensorSize: IntSize, filter: ColorFilter?) {
    drawImage(
        frame.image,
        srcSize = IntSize(frame.width, frame.height),
        dstOffset = IntOffset(-sensorSize.width / 2, -sensorSize.height / 2),
        dstSize = sensorSize,
        colorFilter = filter,
    )
}

private fun Int.isQuarterTurn() = (this / 90) % 2 != 0
