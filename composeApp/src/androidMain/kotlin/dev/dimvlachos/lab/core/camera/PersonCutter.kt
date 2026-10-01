package dev.dimvlachos.lab.core.camera

import android.graphics.Bitmap
import android.graphics.Canvas
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
import kotlin.math.max
import kotlin.math.roundToInt

private val log = Logger.withTag("PersonCutter")

/**
 * Finds the person in each camera frame, on the phone itself, with ML Kit's selfie segmentation: a
 * mask, upright, opaque where they are, so the room behind them can be swapped for another. Runs on
 * the camera's own thread; a frame waits for its mask. Once it fails, it stops trying, and frames
 * show whole.
 */
internal class PersonCutter {
    private val segmenter: Segmenter =
        Segmentation.getClient(
            SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
                .build()
        )

    // The frame, made small: the model works at about 256 pixels, and a smaller picture is quicker.
    private var small: Bitmap? = null
    private val shrink = Paint(Paint.FILTER_BITMAP_FLAG)

    // Masks are drawn while the next is made: three, as the frames have.
    private val masks = arrayOfNulls<Bitmap>(3)
    private var index = 0
    private var alpha: ByteBuffer? = null

    private var failed = false

    // How it is doing, logged now and then: time per frame and how much of the picture is a person.
    private var frames = 0
    private var millis = 0L
    private var person = 0f

    /**
     * The person's mask for the top-left [width] by [height] pixels of [frame], which turns
     * [rotationDegrees] clockwise to stand upright; `null` if it cannot tell.
     */
    fun cut(frame: Bitmap, width: Int, height: Int, rotationDegrees: Int): ImageBitmap? {
        if (failed) return null
        val started = SystemClock.elapsedRealtime()
        return try {
            val input = shrunk(frame, width, height)
            val mask =
                Tasks.await(
                    segmenter.process(InputImage.fromBitmap(input, rotationDegrees)),
                    MaxWaitMillis,
                    TimeUnit.MILLISECONDS,
                )
            toAlpha(mask.buffer, mask.width, mask.height).also {
                note(SystemClock.elapsedRealtime() - started)
            }
        } catch (e: Exception) {
            if (e is InterruptedException) Thread.currentThread().interrupt()
            log.w(e) { "no person found, so the whole picture shows" }
            failed = true
            null
        }
    }

    fun close() {
        segmenter.close()
    }

    private fun shrunk(frame: Bitmap, width: Int, height: Int): Bitmap {
        val scale = SegmentLongSide.toFloat() / max(width, height)
        val w = (width * scale).roundToInt()
        val h = (height * scale).roundToInt()
        val bitmap =
            small?.takeIf { it.width == w && it.height == h }
                ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { small = it }
        Canvas(bitmap).drawBitmap(frame, Rect(0, 0, width, height), Rect(0, 0, w, h), shrink)
        return bitmap
    }

    // The model's confidence, 0 to 1 per pixel, as an alpha mask with a soft edge: sure of the
    // person, opaque; sure of the room, clear; between, a short ramp, so hair blends.
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
                val t = ((confidence.float - EdgeFrom) / (EdgeTo - EdgeFrom)).coerceIn(0f, 1f)
                val a = t * t * (3 - 2 * t)
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

private const val SegmentLongSide = 512
private const val MaxWaitMillis = 500L
private const val EdgeFrom = 0.35f
private const val EdgeTo = 0.75f
private const val LogEvery = 60
