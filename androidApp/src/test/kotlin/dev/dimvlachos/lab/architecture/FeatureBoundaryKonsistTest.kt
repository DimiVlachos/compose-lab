package dev.dimvlachos.lab.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

class FeatureBoundaryKonsistTest {

    @Test
    fun `core never imports a feature package or App`() {
        Konsist.scopeFromProject()
            .files
            .filter {
                it.packagee?.name.orEmpty().let { p ->
                    p == "dev.dimvlachos.lab.core" || p.startsWith("dev.dimvlachos.lab.core.")
                }
            }
            .assertFalse { file ->
                file.imports.any { import ->
                    val name = import.name
                    name.startsWith("dev.dimvlachos.lab.catalog.") ||
                        name == "dev.dimvlachos.lab.catalog" ||
                        name.startsWith("dev.dimvlachos.lab.demo.") ||
                        name == "dev.dimvlachos.lab.demo" ||
                        name.startsWith("dev.dimvlachos.lab.navbardemo.") ||
                        name == "dev.dimvlachos.lab.navbardemo" ||
                        name.startsWith("dev.dimvlachos.lab.gallerydemo.") ||
                        name == "dev.dimvlachos.lab.gallerydemo" ||
                        name.startsWith("dev.dimvlachos.lab.agenticdemo.") ||
                        name == "dev.dimvlachos.lab.agenticdemo" ||
                        name == "dev.dimvlachos.lab.App"
                }
            }
    }

    @Test
    fun `core presentation components never imports core demo`() {
        Konsist.scopeFromProject()
            .files
            .filter {
                val p = it.packagee?.name.orEmpty()
                p == "dev.dimvlachos.lab.core.presentation.components" ||
                    p.startsWith("dev.dimvlachos.lab.core.presentation.components.")
            }
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name == "dev.dimvlachos.lab.core.demo" ||
                        import.name.startsWith("dev.dimvlachos.lab.core.demo.")
                }
            }
    }
}
