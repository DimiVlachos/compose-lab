package dev.dimvlachos.lab.core.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * One frame from a camera, as its sensor saw it: the top-left [width] by [height] pixels of [image]
 * (rows may be padded wider), to be turned [rotationDegrees] clockwise to stand upright.
 */
class CameraFrame(
    val image: ImageBitmap,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
)

/**
 * Paints the latest [CameraFrame] upright and flipped left to right, as a mirror shows a face. A
 * new frame only redraws: the frame is read while drawing, never while composing, and the size
 * changes only with the resolution or the rotation.
 */
class MirrorFramePainter : Painter() {
    private var frame by mutableStateOf<CameraFrame?>(null)
    private var uprightSize by mutableStateOf(Size.Unspecified)
    private var colorFilter: ColorFilter? = null

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
        // The frame as the sensor has it, sized so that once turned it fills the upright box.
        val sensorSize =
            if (frame.rotationDegrees.isQuarterTurn())
                IntSize(size.height.roundToInt(), size.width.roundToInt())
            else IntSize(size.width.roundToInt(), size.height.roundToInt())
        withTransform({
            translate(size.width / 2, size.height / 2)
            // The mirror's flip, left to right as the frame will be seen, after it is turned.
            scale(-1f, 1f, pivot = Offset.Zero)
            rotate(frame.rotationDegrees.toFloat(), pivot = Offset.Zero)
        }) {
            drawImage(
                frame.image,
                srcSize = IntSize(frame.width, frame.height),
                dstOffset = IntOffset(-sensorSize.width / 2, -sensorSize.height / 2),
                dstSize = sensorSize,
                colorFilter = colorFilter,
            )
        }
    }
}

private fun Int.isQuarterTurn() = (this / 90) % 2 != 0
