plugins { alias(libs.plugins.lab.android.application) }

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.konsist)
}

android {
    namespace = "dev.dimvlachos.lab"

    defaultConfig {
        applicationId = "dev.dimvlachos.lab"
        // 64-bit Arm only: every phone the camera demo is for. ML Kit's native library is about
        // 20 MB for each architecture.
        ndk { abiFilters += "arm64-v8a" }
    }
    // Chromebooks are x86_64, left out above on purpose.
    lint { disable += "ChromeOsAbiSupport" }
}
