import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.skie)
}

kotlin {
    android {
        namespace = "dev.dimvlachos.moodboard.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "MoodboardShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.animation)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(libs.cmp.material3.expressive)
            implementation(compose.components.resources)
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
