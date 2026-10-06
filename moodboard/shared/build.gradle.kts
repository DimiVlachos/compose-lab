plugins {
    alias(libs.plugins.lab.kmp.library)
    alias(libs.plugins.skie)
}

kotlin {
    android { namespace = "dev.dimvlachos.moodboard.shared" }
    iosFramework(baseName = "MoodboardShared")

    sourceSets {
        commonMain.dependencies {
            implementation(libs.cmp.runtime)
            implementation(libs.cmp.animation)
            implementation(libs.cmp.foundation)
            implementation(libs.cmp.ui)
            implementation(libs.cmp.material3.expressive)
            implementation(libs.cmp.components.resources)
            implementation(libs.cmp.ui.tooling.preview)
            implementation(libs.kermit)
            api(libs.kotlinx.coroutines.core)
            api(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.cmp.ui.test)
        }
    }
}

compose.resources { packageOfResClass = "dev.dimvlachos.moodboard.resources" }

// SKIE collects build analytics and uploads them by default; nothing from this build leaves it.
skie { analytics { enabled.set(false) } }
