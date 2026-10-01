package dev.dimvlachos.lab.core.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.os.SystemClock
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import co.touchlab.kermit.Logger
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.roundToInt

private val log = Logger.withTag("PersonCutter")

/**
 * Finds the person in each camera frame, on the phone itself, with ML Kit's selfie segmentation: a
 * mask, upright, opaque where they are, so the room behind them can be swapped for another. Runs on
 * the camera's own thread; a frame waits for its mask. Should it keep failing, it stops trying, and
 * frames show whole.
 */
internal class PersonCutter {
    // Made on the camera's thread with the first frame, so a failure only means no cut-out.
    private var segmenter: Segmenter? = null
    private val health = SegmentationHealth()

    // The frame, made small and as bright as it shows, for the model.
    private var small: Bitmap? = null
    private val shrink = Paint(Paint.FILTER_BITMAP_FLAG)
    private var brightenedBy = 1f

    // Masks are drawn while the next is made: three, as the frames have.
    private val masks = arrayOfNulls<Bitmap>(3)
    private var index = 0
    private var alpha: ByteBuffer? = null

    // How it is doing, logged now and then: time per frame and how much of the picture is a person.
    private var frames = 0
    private var millis = 0L
    private var person = 0f

    /**
     * The person's mask for the top-left [width] by [height] pixels of [frame], which turns
     * [rotationDegrees] clockwise to stand upright and shows brightened [gain] times; `null` if it
     * cannot tell.
     */
    fun cut(
        frame: Bitmap,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        gain: Float,
    ): ImageBitmap? {
        if (health.givenUp) return null
        val started = SystemClock.elapsedRealtime()
        return try {
            val client = segmenter ?: newSegmenter().also { segmenter = it }
            val input = shrunk(frame, width, height, gain)
            val mask =
                Tasks.await(
                    client.process(InputImage.fromBitmap(input, rotationDegrees)),
                    health.waitMillis,
                    TimeUnit.MILLISECONDS,
                )
            health.succeeded()
            toAlpha(mask.buffer, mask.width, mask.height).also {
                note(SystemClock.elapsedRealtime() - started)
            }
        } catch (e: Exception) {
            if (e is InterruptedException) Thread.currentThread().interrupt()
            // Still at work on the old picture: the next frame gets a new one.
            if (e is TimeoutException) small = null
            health.failed()
            if (health.givenUp)
                log.w(e) { "the person cannot be found, so the whole picture shows" }
            null
        }
    }

    fun close() {
        segmenter?.close()
    }

    private fun newSegmenter() =
        Segmentation.getClient(
            SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
                .build()
        )

    private fun shrunk(frame: Bitmap, width: Int, height: Int, gain: Float): Bitmap {
        val size = segmentSize(width, height)
        val bitmap =
            small?.takeIf { it.width == size.width && it.height == size.height }
                ?: Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888).also {
                    small = it
                }
        // A dim room's frame, as bright as the user sees it: the model finds them better.
        if (gain != brightenedBy) {
            brightenedBy = gain
            shrink.colorFilter =
                if (gain <= 1.001f) null
                else ColorMatrixColorFilter(ColorMatrix().apply { setScale(gain, gain, gain, 1f) })
        }
        Canvas(bitmap)
            .drawBitmap(
                frame,
                Rect(0, 0, width, height),
                Rect(0, 0, size.width, size.height),
                shrink,
            )
        return bitmap
    }

    // The model's confidence, 0 to 1 per pixel, as an alpha mask.
    private fun toAlpha(confidence: ByteBuffer, width: Int, height: Int): ImageBitmap {
        index = (index + 1) % masks.size
        val bitmap =
            masks[index]?.takeIf { it.width == width && it.height == height }
                ?: Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8).also {
                    masks[index] = it
                }
        val rowBytes = bitmap.rowBytes
        val bytes =
            alpha?.takeIf { it.capacity() == rowBytes * height }
                ?: ByteBuffer.allocateDirect(rowBytes * height).also { alpha = it }
        confidence.rewind()
        var sum = 0f
        for (y in 0 until height) {
            bytes.position(y * rowBytes)
            for (x in 0 until width) {
                val a = personAlpha(confidence.float)
                sum += a
                bytes.put((a * 255).roundToInt().toByte())
            }
        }
        bytes.rewind()
        bitmap.copyPixelsFromBuffer(bytes)
        person += sum / (width * height)
        return bitmap.asImageBitmap()
    }

    private fun note(took: Long) {
        frames++
        millis += took
        if (frames == LogEvery) {
            log.i {
                "segmentation ${millis / frames} ms a frame, " +
                    "${(person / frames * 100).roundToInt()}% person"
            }
            frames = 0
            millis = 0
            person = 0f
        }
    }
}

private const val LogEvery = 60
