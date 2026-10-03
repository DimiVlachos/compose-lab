package dev.dimvlachos.lab.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope

/** The lab's own sources: Moodboard lives in the same repo with its own rules. */
internal fun labScope(): KoScope =
    Konsist.scopeFromProject().slice { !it.path.contains("/moodboard/") }
