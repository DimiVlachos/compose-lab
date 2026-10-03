package dev.dimvlachos.lab.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope

/**
 * The lab's own sources: Moodboard lives in the same repo with its own rules. Matched on the
 * project-relative path, so a checkout inside a folder named moodboard still scopes correctly.
 */
internal fun labScope(): KoScope =
    Konsist.scopeFromProject().slice { !it.projectPath.startsWith("/moodboard/") }
