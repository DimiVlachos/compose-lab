package dev.dimvlachos.lab.core.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun ShakeImpulses(enabled: Boolean, onImpulse: (Offset) -> Unit) {
    val context = LocalContext.current
    val latest by rememberUpdatedState(onImpulse)
    DisposableEffect(context, enabled) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        if (!enabled || manager == null || sensor == null) return@DisposableEffect onDispose {}
        val listener =
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    shakeImpulse(event.values[0], event.values[1])?.let { latest(it) }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { manager.unregisterListener(listener) }
    }
}
