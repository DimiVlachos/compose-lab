package dev.dimvlachos.lab.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

// This test class scans the whole project, including itself, so its own source text would
// otherwise match the substrings these rules search for (the string literal for a banned
// substring contains that same substring). Exclude these files from the two text-content
// checks below; every other rule here is import- or annotation-based and doesn't self-match.
private val ARCHITECTURE_TEST_PATH = "src/test/kotlin/dev/dimvlachos/lab/architecture"

class CodingConventionsKonsistTest {

    @Test
    fun `no wildcard imports`() {
        Konsist.scopeFromProject().files.assertFalse { file ->
            file.imports.any { it.isWildcard }
        }
    }

    @Test
    fun `preview functions are private`() {
        Konsist.scopeFromProject()
            .functions()
            .filter { it.hasAnnotationWithName("Preview") }
            .assertTrue { it.hasPrivateModifier }
    }

    @Test
    fun `no println calls`() {
        Konsist.scopeFromProject()
            .files
            .filter { !it.path.contains(ARCHITECTURE_TEST_PATH) }
            .assertFalse { file -> file.text.contains("println(") }
    }

    @Test
    fun `no android util Log - use Kermit`() {
        Konsist.scopeFromProject().files.assertFalse { file ->
            file.imports.any { it.name == "android.util.Log" }
        }
    }

    @Test
    fun `no GlobalScope`() {
        Konsist.scopeFromProject().files.assertFalse { file ->
            file.imports.any { it.name == "kotlinx.coroutines.GlobalScope" }
        }
    }

    @Test
    fun `no hardcoded Color outside the theme package`() {
        Konsist.scopeFromProject()
            .files
            .filter { !it.path.contains(ARCHITECTURE_TEST_PATH) }
            .filter {
                !it.packagee?.name.orEmpty().startsWith("dev.dimvlachos.lab.core.presentation.ui")
            }
            .assertFalse { file -> file.text.contains("Color(0x") }
    }
}
