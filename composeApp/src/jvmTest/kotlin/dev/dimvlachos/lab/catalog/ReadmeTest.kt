package dev.dimvlachos.lab.catalog

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The README keeps up with the catalog: every demo has its row in the components table and its GIF,
 * shown in the README, so a new component can't land without them. JVM only, as it reads the repo's
 * files.
 */
class ReadmeTest {
    private val root =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .first { File(it, "README.md").isFile && File(it, "docs/media").isDirectory }
    private val readme = File(root, "README.md").readText()

    // Demos shown by another demo's GIF: the camera reflection is the fogged mirror's second
    // version, and its clip needs a face in front of the phone.
    private val sharesAGif = mapOf("fog.mirror.camera" to "fog.mirror.bathroom")

    @Test
    fun everyDemoHasItsRowInTheComponentsTable() {
        val rows = readme.lines().filter { it.startsWith("|") }
        val missing = Catalog.demos.map { it.id }.filter { id -> rows.none { "`$id`" in it } }
        assertTrue(missing.isEmpty(), "no row in the README's components table for $missing")
    }

    @Test
    fun everyDemoHasItsGifInDocsMediaAndTheReadme() {
        val missing =
            Catalog.demos
                .map { sharesAGif[it.id] ?: it.id }
                .distinct()
                .filter { id ->
                    !File(root, "docs/media/$id.gif").isFile || "docs/media/$id.gif)" !in readme
                }
        assertTrue(
            missing.isEmpty(),
            "make the GIF with scripts/gif.sh and show it under the component's heading: $missing",
        )
    }

    @Test
    fun everyGifInDocsMediaIsShown() {
        val unused =
            File(root, "docs/media")
                .listFiles { f -> f.extension == "gif" }!!
                .map { it.name }
                .filter { "docs/media/$it" !in readme }
        assertTrue(unused.isEmpty(), "GIFs in docs/media the README doesn't show: $unused")
    }
}
