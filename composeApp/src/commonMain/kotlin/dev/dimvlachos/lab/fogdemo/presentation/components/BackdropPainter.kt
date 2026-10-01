package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntSize
import dev.dimvlachos.lab.core.camera.MirrorFramePainter
import dev.dimvlachos.lab.core.presentation.components.fog.coverCrop
import kotlin.math.roundToInt

/**
 * [front], the person the camera cut out, standing in front of [back], the bathroom, cropped to
 * cover the same box. As large as [front], and the caller's filter tints both. A frame with no one
 * cut out of it shows whole, with nothing drawn behind it.
 */
internal class BackdropPainter(
    private val front: MirrorFramePainter,
    private val back: ImageBitmap,
) : Painter() {
    private var colorFilter: ColorFilter? = null

    override val intrinsicSize: Size
        get() = front.intrinsicSize

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun DrawScope.onDraw() {
        if (!front.cutOut) {
            with(front) { draw(size, colorFilter = this@BackdropPainter.colorFilter) }
            return
        }
        val (offset, cropped) = coverCrop(IntSize(back.width, back.height), size)
        drawImage(
            back,
            srcOffset = offset,
            srcSize = cropped,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            colorFilter = colorFilter,
            filterQuality = FilterQuality.High,
        )
        with(front) { draw(size, colorFilter = this@BackdropPainter.colorFilter) }
    }
}
