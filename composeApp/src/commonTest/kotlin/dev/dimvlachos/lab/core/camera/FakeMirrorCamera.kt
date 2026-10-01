package dev.dimvlachos.lab.core.camera

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import kotlinx.coroutines.awaitCancellation

/**
 * A camera that shows one solid [color] frame while it runs (none while it is still [starting]), or
 * throws [failure] instead; [nobodyThere], its frames come with a mask that keeps nothing. Counts
 * how often it was started and how many runs are live.
 */
internal class FakeMirrorCamera(
    private val color: Color = Color.Magenta,
    private val failure: Throwable? = null,
    private val starting: Boolean = false,
    private val nobodyThere: Boolean = false,
) : MirrorCamera {
    private val painter = MirrorFramePainter()
    var starts = 0
        private set

    var running = 0
        private set

    override val mirror = painter
    override val showing: Boolean
        get() = painter.hasFrame

    override suspend fun run() {
        starts++
        failure?.let { throw it }
        running++
        try {
            if (starting) awaitCancellation()
            val image = ImageBitmap(4, 4)
            Canvas(image)
                .drawRect(0f, 0f, 4f, 4f, Paint().apply { color = this@FakeMirrorCamera.color })
            val mask = if (nobodyThere) ImageBitmap(4, 4) else null
            painter.show(
                CameraFrame(image, width = 4, height = 4, rotationDegrees = 0, mask = mask)
            )
            awaitCancellation()
        } finally {
            // Stopped, it keeps its last frame, as the real one does.
            running--
        }
    }
}
