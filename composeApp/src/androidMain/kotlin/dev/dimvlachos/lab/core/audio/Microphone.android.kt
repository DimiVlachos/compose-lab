package dev.dimvlachos.lab.core.audio

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

@Composable
actual fun rememberMicAccess(enabled: Boolean): MicAccess {
    if (!enabled) return MicAccess.Unavailable
    val context = LocalContext.current
    fun isGranted() =
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(isGranted()) }
    var blocked by rememberSaveable { mutableStateOf(false) }
    var refusals by rememberSaveable { mutableIntStateOf(0) }
    var rationaleBefore by rememberSaveable { mutableStateOf(false) }
    fun rationale() =
        context
            .findActivity()
            ?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == true
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            granted = ok
            if (!ok) {
                refusals++
                // Only settings can help once the system will not ask again.
                blocked = refusedForGood(rationaleBefore, rationale(), refusals)
            }
        }
    // Back from settings, or from anywhere, the permission may have changed.
    LifecycleResumeEffect(Unit) {
        granted = isGranted()
        onPauseOrDispose {}
    }
    val unprocessed = remember {
        context
            .getSystemService(AudioManager::class.java)
            .getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
    }
    val grantedAccess = remember(unprocessed) { MicAccess.Granted(AndroidMicrophone(unprocessed)) }
    val blockedAccess = remember {
        MicAccess.Blocked {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
            )
        }
    }
    // A new Askable after each refusal, so the demo can tell the answer came.
    val askableAccess =
        remember(refusals) {
            MicAccess.Askable {
                rationaleBefore = rationale()
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    return when {
        granted -> grantedAccess
        blocked -> blockedAccess
        else -> askableAccess
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

/**
 * The phone's microphone through [AudioRecord]. Raw audio where the device offers it: the voice
 * sources' noise suppression and gain control are made to remove exactly the hiss a blow makes.
 */
private class AndroidMicrophone(private val unprocessed: Boolean) : Microphone {
    // Only created once RECORD_AUDIO is granted.
    @SuppressLint("MissingPermission")
    override val frames: Flow<FloatArray> =
        flow {
                val source =
                    if (unprocessed) MediaRecorder.AudioSource.UNPROCESSED
                    else MediaRecorder.AudioSource.MIC
                val minBuffer =
                    AudioRecord.getMinBufferSize(
                        MicSampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                    )
                val record =
                    AudioRecord(
                        source,
                        MicSampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        maxOf(minBuffer, MicFrameSize * 2 * 4),
                    )
                try {
                    check(record.state == AudioRecord.STATE_INITIALIZED) {
                        "the microphone could not start"
                    }
                    record.startRecording()
                    val samples = ShortArray(MicFrameSize)
                    while (currentCoroutineContext().isActive) {
                        var read = 0
                        while (read < MicFrameSize) {
                            val count = record.read(samples, read, MicFrameSize - read)
                            check(count >= 0) { "the microphone stopped: $count" }
                            read += count
                        }
                        emit(FloatArray(MicFrameSize) { samples[it] / 32_768f })
                    }
                } finally {
                    if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop()
                    record.release()
                }
            }
            .flowOn(Dispatchers.IO)
}
