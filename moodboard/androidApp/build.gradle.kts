plugins {
    alias(libs.plugins.lab.android.application)
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(project(":moodboard:shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.cmp.material3.expressive)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kermit)
    implementation(libs.lifecycle.runtime.compose)
    testImplementation(libs.junit)
    testImplementation(libs.konsist)
}

android {
    namespace = "dev.dimvlachos.moodboard"
    defaultConfig { applicationId = "dev.dimvlachos.moodboard" }
}
