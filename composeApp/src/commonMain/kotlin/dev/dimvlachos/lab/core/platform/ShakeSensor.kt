package dev.dimvlachos.lab.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt

/** Below this linear acceleration, in m/s², the phone is just being carried. */
private const val ShakeNoiseFloor = 2.5f

/**
 * The impulse a shake of the phone gives, from its linear acceleration along the device's [x] and
 * [y] axes in m/s², in screen space (y down); null while it is only being carried about.
 */
internal fun shakeImpulse(x: Float, y: Float): Offset? =
    if (sqrt(x * x + y * y) < ShakeNoiseFloor) null else Offset(x, -y)

/**
 * Calls [onImpulse] with each [shakeImpulse] while [enabled] and on screen; nothing on a device
 * without the sensor.
 */
@Composable internal expect fun ShakeImpulses(enabled: Boolean, onImpulse: (Offset) -> Unit)
