package dev.dimvlachos.lab.core.audio

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.dimvlachos.lab.core.permission.PermissionStatus
import dev.dimvlachos.lab.core.permission.rememberPermissionStatus
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
    val status = rememberPermissionStatus(Manifest.permission.RECORD_AUDIO)
    val unprocessed = remember {
        context
            .getSystemService(AudioManager::class.java)
            .getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
    }
    val grantedAccess = remember(unprocessed) { MicAccess.Granted(AndroidMicrophone(unprocessed)) }
    // Remembered per status, so each refusal still hands the demo a new Askable.
    return remember(status) {
        when (status) {
            PermissionStatus.Granted -> grantedAccess
            is PermissionStatus.Askable -> MicAccess.Askable(status.ask)
            is PermissionStatus.Blocked -> MicAccess.Blocked(status.openSettings)
        }
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
