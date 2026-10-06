plugins { `kotlin-dsl` }

group = "dev.dimvlachos.lab.buildlogic"

// The plugins are compileOnly: the root build puts them on the classpath, at the catalog's
// versions, and these conventions only configure them.
dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "lab.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "lab.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
    }
}
