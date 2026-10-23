package dev.dimvlachos.lab.core.platform

import kotlin.random.Random

/**
 * Plays [sound] like [SoundPlayer.play], but a touch different each time: the rate moves by up to
 * 4% either way and the level drops by up to 10%. A real object never sounds exactly the same
 * twice, and a clip that repeats unchanged is what gives a synthesized sound away. Pass a seeded
 * [random] in tests.
 */
internal fun SoundPlayer.playVaried(
    sound: Sound,
    volume: Float = 1f,
    rate: Float = 1f,
    random: Random = Random.Default,
) {
    val r = rate * (1f + (random.nextFloat() * 2f - 1f) * RateJitter)
    val v = volume * (1f - random.nextFloat() * VolumeJitter)
    play(sound, v.coerceIn(0f, 1f), r.coerceIn(.5f, 2f))
}

private const val RateJitter = .04f
private const val VolumeJitter = .1f
