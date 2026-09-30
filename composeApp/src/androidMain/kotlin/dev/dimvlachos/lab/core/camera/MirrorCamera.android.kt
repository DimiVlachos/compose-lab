package dev.dimvlachos.lab.core.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.dimvlachos.lab.core.permission.PermissionStatus
import dev.dimvlachos.lab.core.permission.rememberPermissionStatus
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred

@Composable
actual fun rememberCameraAccess(enabled: Boolean): CameraAccess {
    if (!enabled) return CameraAccess.Unavailable
    val context = LocalContext.current
    val hasFrontCamera = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
    }
    if (!hasFrontCamera) return CameraAccess.Unavailable
    val status = rememberPermissionStatus(Manifest.permission.CAMERA)
    val lifecycleOwner = LocalLifecycleOwner.current
    val grantedAccess =
        remember(context, lifecycleOwner) {
            CameraAccess.Granted(AndroidMirrorCamera(context, lifecycleOwner))
        }
    // Remembered per status, so each refusal still hands the demo a new Askable.
    return remember(status, grantedAccess) {
        when (status) {
            PermissionStatus.Granted -> grantedAccess
            is PermissionStatus.Askable -> CameraAccess.Askable(status.ask)
            is PermissionStatus.Blocked -> CameraAccess.Blocked(status.openSettings)
        }
    }
}

// About 720p: sharp enough behind the fog, light enough to blur every frame.
private val AnalysisSize = Size(1280, 720)

/**
 * The front camera through CameraX's image analysis rather than a preview surface: the fog draws
 * the mirror twice, sharp and blurred, so each frame has to be an image it can paint.
 */
private class AndroidMirrorCamera(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) : MirrorCamera {
    private val painter = MirrorFramePainter()

    override val mirror: Painter = painter

    override val showing: Boolean
        get() = painter.hasFrame

    // Only called once CAMERA is granted, on the main thread.
    override suspend fun run() {
        val provider = ProcessCameraProvider.awaitInstance(context)
        check(provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) { "no front camera" }
        val display = checkNotNull(context.display) { "no display to face" }
        val analysis =
            ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                AnalysisSize,
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                            )
                        )
                        .build()
                )
                .setTargetRotation(display.rotation)
                .build()
        val executor = Executors.newSingleThreadExecutor()
        val ring = FrameRing()
        // Off once the mirror stops, so a frame still on its way cannot show after it.
        val live = AtomicBoolean(true)
        analysis.setAnalyzer(executor) { image ->
            image.use { if (live.get()) painter.show(ring.next(it)) }
        }

        // The activity turns itself, so the frames' rotation follows the display by hand.
        val displays = context.getSystemService(DisplayManager::class.java)
        val rotation =
            object : DisplayManager.DisplayListener {
                override fun onDisplayChanged(displayId: Int) {
                    if (displayId == display.displayId) analysis.targetRotation = display.rotation
                }

                override fun onDisplayAdded(displayId: Int) {}

                override fun onDisplayRemoved(displayId: Int) {}
            }
        displays.registerDisplayListener(rotation, null)

        // A camera taken by another app for a moment only hides the mirror until it is back; one
        // the system has disabled, or broken, ends it for this visit.
        val failure = CompletableDeferred<Unit>()
        val cameraState =
            Observer<CameraState> { state ->
                val error = state.error ?: return@Observer
                if (error.type == CameraState.ErrorType.CRITICAL) {
                    failure.completeExceptionally(
                        IllegalStateException("camera error ${error.code}")
                    )
                } else {
                    painter.show(null)
                }
            }
        val camera =
            try {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    analysis,
                )
            } catch (e: Exception) {
                displays.unregisterDisplayListener(rotation)
                executor.shutdown()
                throw e
            }
        camera.cameraInfo.cameraState.observe(lifecycleOwner, cameraState)
        try {
            failure.await()
        } finally {
            live.set(false)
            camera.cameraInfo.cameraState.removeObserver(cameraState)
            provider.unbind(analysis)
            analysis.clearAnalyzer()
            displays.unregisterDisplayListener(rotation)
            // The last frame stays: back from a pause, the face waits for the camera, not a still.
            executor.shutdown()
        }
    }
}

/**
 * Three bitmaps the camera's frames are copied into in turn, so no frame allocates one: one on
 * screen, one being drawn, one being filled. Never recycled, as the last may still be on screen.
 */
private class FrameRing {
    private val bitmaps = arrayOfNulls<Bitmap>(3)
    private var index = 0
    private var scratch: ByteBuffer? = null

    fun next(image: ImageProxy): CameraFrame {
        val plane = image.planes[0]
        // Rows may be padded: the bitmap is as wide as a padded row, the frame says what is used.
        val rowPixels = plane.rowStride / plane.pixelStride
        index = (index + 1) % bitmaps.size
        val bitmap =
            bitmaps[index]?.takeIf { it.width == rowPixels && it.height == image.height }
                ?: Bitmap.createBitmap(rowPixels, image.height, Bitmap.Config.ARGB_8888).also {
                    bitmaps[index] = it
                }
        val buffer = plane.buffer.apply { rewind() }
        if (buffer.remaining() >= bitmap.byteCount) {
            bitmap.copyPixelsFromBuffer(buffer)
        } else {
            // The last row can stop short of its padding: copy into a buffer a full bitmap long.
            val full =
                scratch?.takeIf { it.capacity() == bitmap.byteCount }
                    ?: ByteBuffer.allocateDirect(bitmap.byteCount).also { scratch = it }
            full.clear()
            full.put(buffer)
            full.rewind()
            bitmap.copyPixelsFromBuffer(full)
        }
        return CameraFrame(
            bitmap.asImageBitmap(),
            width = image.width,
            height = image.height,
            rotationDegrees = image.imageInfo.rotationDegrees,
        )
    }
}
