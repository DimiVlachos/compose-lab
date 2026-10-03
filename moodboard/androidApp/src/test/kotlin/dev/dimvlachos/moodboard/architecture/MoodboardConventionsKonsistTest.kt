package dev.dimvlachos.moodboard.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

// These rules search source text, and their own literals contain what they ban, so the rule
// files themselves are left out of the text checks.
private const val ARCHITECTURE_TEST_PATH = "src/test/kotlin/dev/dimvlachos/moodboard/architecture"

private fun moodboardScope(): KoScope =
    Konsist.scopeFromProject().slice { it.path.contains("/moodboard/") }

class MoodboardConventionsKonsistTest {

    @Test
    fun `no wildcard imports`() {
        moodboardScope().files.assertFalse { file -> file.imports.any { it.isWildcard } }
    }

    @Test
    fun `preview functions are private`() {
        moodboardScope()
            .functions()
            .filter { it.hasAnnotationWithName("Preview") }
            .assertTrue { it.hasPrivateModifier }
    }

    @Test
    fun `no println calls`() {
        moodboardScope()
            .files
            .filter { !it.path.contains(ARCHITECTURE_TEST_PATH) }
            .assertFalse { file -> file.text.contains("println(") }
    }

    @Test
    fun `no android util Log - use Kermit`() {
        moodboardScope().files.assertFalse { file ->
            file.imports.any { it.name == "android.util.Log" }
        }
    }

    @Test
    fun `no GlobalScope`() {
        moodboardScope().files.assertFalse { file ->
            file.imports.any { it.name == "kotlinx.coroutines.GlobalScope" }
        }
    }

    @Test
    fun `no hardcoded Color outside the theme package`() {
        moodboardScope()
            .files
            .filter { !it.path.contains(ARCHITECTURE_TEST_PATH) }
            .filter { !it.packagee?.name.orEmpty().startsWith("dev.dimvlachos.moodboard.ui.theme") }
            .assertFalse { file -> file.text.contains("Color(0x") }
    }

    @Test
    fun `moodboard never imports the lab`() {
        moodboardScope().files.assertFalse { file ->
            file.imports.any { it.name.startsWith("dev.dimvlachos.lab.") }
        }
    }

    @Test
    fun `screens reach the repository only through ViewModels`() {
        moodboardScope()
            .files
            .filter { it.packagee?.name.orEmpty().startsWith("dev.dimvlachos.moodboard.android") }
            .assertFalse { file -> file.text.contains("MoodboardGraph.repository") }
    }
}
