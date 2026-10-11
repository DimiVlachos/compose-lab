package dev.dimvlachos.lab.architecture

import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Assert.assertTrue
import org.junit.Test

// This test class scans the whole project, including itself, so its own source text would
// otherwise match the substrings these rules search for (the string literal for a banned
// substring contains that same substring). Exclude these files from the text-content checks
// below; every other rule here is import- or annotation-based and doesn't self-match.
private val ARCHITECTURE_TEST_PATH = "src/test/kotlin/dev/dimvlachos/lab/architecture"

class CodingConventionsKonsistTest {

    @Test
    fun `the scope holds the lab's sources and not Moodboard's`() {
        val paths = labScope().files.map { it.projectPath }
        assertTrue(paths.any { it.startsWith("/composeApp/") })
        assertTrue(paths.none { it.startsWith("/moodboard/") })
    }

    @Test
    fun `no wildcard imports`() {
        labScope().files.assertFalse { file ->
            file.imports.any { it.isWildcard }
        }
    }

    @Test
    fun `preview functions are private`() {
        labScope()
            .functions()
            .filter { it.hasAnnotationWithName("Preview") }
            .assertTrue { it.hasPrivateModifier }
    }

    @Test
    fun `no println calls`() {
        labScope()
            .files
            .filter { !it.path.contains(ARCHITECTURE_TEST_PATH) }
            .assertFalse { file -> file.text.contains("println(") }
    }

    @Test
    fun `no android util Log - use Kermit`() {
        labScope().files.assertFalse { file ->
            file.imports.any { it.name == "android.util.Log" }
        }
    }

    @Test
    fun `no GlobalScope`() {
        labScope().files.assertFalse { file ->
            file.imports.any { it.name == "kotlinx.coroutines.GlobalScope" }
        }
    }

    @Test
    fun `no hardcoded Color outside the theme package`() {
        labScope()
            .files
            .filter { !it.path.contains(ARCHITECTURE_TEST_PATH) }
            .filter {
                !it.packagee?.name.orEmpty().startsWith("dev.dimvlachos.lab.core.presentation.ui")
            }
            .assertFalse { file -> file.text.contains("Color(0x") }
    }

    // The theme decides light or dark, once, in App.kt; everything else reads LabTheme. A screen
    // that asked the system itself would ignore the theme button and stay light while recording.
    @Test
    fun `isSystemInDarkTheme only in App and the theme package`() {
        labScope()
            .files
            .filter { file ->
                file.imports.any { it.name == "androidx.compose.foundation.isSystemInDarkTheme" }
            }
            .assertTrue { file ->
                file.path.endsWith("/dev/dimvlachos/lab/App.kt") ||
                    file.packagee
                        ?.name
                        .orEmpty()
                        .startsWith("dev.dimvlachos.lab.core.presentation.ui")
            }
    }

    // Icons are vector drawables in composeResources; the icons library is megabytes.
    @Test
    fun `no Material icons library`() {
        labScope().files.assertFalse { file ->
            file.imports.any { it.name.startsWith("androidx.compose.material.icons") }
        }
    }

    // The lab's colours and type come from LabTheme; Material components are given them directly.
    @Test
    fun `no MaterialTheme`() {
        labScope()
            .files
            .filter { !it.path.contains(ARCHITECTURE_TEST_PATH) }
            .assertFalse { file -> file.text.contains("MaterialTheme") }
    }
}
