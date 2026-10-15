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
