package dev.dimvlachos.lab.core.platform

/**
 * The name a copy of [sound]'s [bytes] is kept under on disk. It carries a hash of the bytes, so a
 * clip that changes in an update is copied afresh instead of the old copy being played.
 */
internal fun soundCacheName(sound: Sound, bytes: ByteArray): String {
    var hash = FnvOffset
    for (b in bytes) {
        hash = (hash xor (b.toLong() and 0xff)) * FnvPrime
    }
    return "${sound.file}-${hash.toULong().toString(16).padStart(16, '0')}.wav"
}

/** Whether [name] is an older copy of [sound] that [soundCacheName] has replaced with [current]. */
internal fun isStaleSoundCopy(sound: Sound, name: String, current: String): Boolean =
    name != current &&
        (name == "${sound.file}.wav" || name.startsWith("${sound.file}-") && name.endsWith(".wav"))

// 64-bit FNV-1a.
private const val FnvOffset = -0x340d631b7bdddcdbL
private const val FnvPrime = 0x100000001b3L

/**
 * A clip's samples, decoded from a WAVE file by [decodeWave]: [channels] interleaved, from -1 to 1.
 */
internal class WaveClip(val sampleRate: Int, val channels: Int, val samples: FloatArray) {
    val frames: Int
        get() = samples.size / channels
}

/** Decodes a 16-bit PCM WAVE file, the kind `scripts/synth-sounds.py` writes. */
internal fun decodeWave(bytes: ByteArray): WaveClip {
    require(
        bytes.size >= 12 &&
            bytes.decodeToString(0, 4) == "RIFF" &&
            bytes.decodeToString(8, 12) == "WAVE",
    ) {
        "Not a WAVE file"
    }
    var channels = 0
    var sampleRate = 0
    var at = 12
    while (at + 8 <= bytes.size) {
        val id = bytes.decodeToString(at, at + 4)
        val size = bytes.int32(at + 4)
        val body = at + 8
        require(size >= 0 && body + size <= bytes.size) { "Chunk $id runs past the end" }
        when (id) {
            "fmt " -> {
                require(bytes.int16(body) == PcmFormat) { "Not PCM" }
                require(bytes.int16(body + 14) == 16) { "Not 16-bit" }
                channels = bytes.int16(body + 2)
                sampleRate = bytes.int32(body + 4)
            }
            "data" -> {
                require(channels > 0 && sampleRate > 0) { "Data before format" }
                val samples =
                    FloatArray(size / 2 / channels * channels) { i ->
                        bytes.int16(body + i * 2).toShort() / 32768f
                    }
                return WaveClip(sampleRate, channels, samples)
            }
        }
        at = body + size + (size and 1)
    }
    throw IllegalArgumentException("No data")
}

private fun ByteArray.int16(at: Int): Int =
    (this[at].toInt() and 0xff) or (this[at + 1].toInt() and 0xff shl 8)

private fun ByteArray.int32(at: Int): Int = int16(at) or (int16(at + 2) shl 16)

private const val PcmFormat = 1
