package dev.dimvlachos.lab.fogdemo

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * How long a test that plays out the fog misting back over may take, in real time. They draw half a
 * minute of the mirror's blurred layers frame by frame: about 30 s on a laptop, and twice that on a
 * CI runner, past the test runner's own 60 s.
 */
internal val SlowFogTest: Duration = 3.minutes
