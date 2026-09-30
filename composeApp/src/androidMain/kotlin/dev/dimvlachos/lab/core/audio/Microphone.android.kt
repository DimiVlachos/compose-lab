package dev.dimvlachos.lab.core.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
    var granted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var answered by rememberSaveable { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            granted = it
            answered = true
        }
    LaunchedEffect(Unit) {
        if (!granted && !answered) launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
    val unprocessed = remember {
        context
            .getSystemService(AudioManager::class.java)
            .getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
    }
    val access = remember(unprocessed) { MicAccess.Granted(AndroidMicrophone(unprocessed)) }
    return when {
        granted -> access
        answered -> MicAccess.Unavailable
        else -> MicAccess.Pending
    }
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
