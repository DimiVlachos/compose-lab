package dev.dimvlachos.lab.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

private const val StandardGravity = 9.80665f

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun ShakeImpulses(enabled: Boolean, onImpulse: (Offset) -> Unit) {
    val latest by rememberUpdatedState(onImpulse)
    DisposableEffect(enabled) {
        val manager = CMMotionManager()
        if (!enabled || !manager.deviceMotionAvailable) return@DisposableEffect onDispose {}
        manager.deviceMotionUpdateInterval = 1.0 / 60.0
        manager.startDeviceMotionUpdatesToQueue(NSOperationQueue.mainQueue) { motion, _ ->
            motion?.userAcceleration?.useContents {
                // userAcceleration is in g.
                shakeImpulse(x.toFloat() * StandardGravity, y.toFloat() * StandardGravity)?.let {
                    latest(it)
                }
            }
        }
        onDispose { manager.stopDeviceMotionUpdates() }
    }
}
