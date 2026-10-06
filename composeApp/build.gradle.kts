plugins { alias(libs.plugins.lab.kmp.library) }

kotlin {
    explicitApi()
    android { namespace = "dev.dimvlachos.lab.shared" }
    iosFramework(baseName = "ComposeApp")

    sourceSets {
        commonMain.dependencies {
            implementation(libs.cmp.runtime)
            implementation(libs.cmp.animation)
            implementation(libs.cmp.foundation)
            implementation(libs.cmp.ui)
            implementation(libs.cmp.material3)
            implementation(libs.cmp.components.resources)
            implementation(libs.cmp.ui.tooling.preview)
            implementation(libs.kermit)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.navigationevent.compose)
            implementation(libs.lifecycle.runtime.compose)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.mlkit.segmentation.selfie)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.cmp.ui.test)
        }
    }
}

compose.resources { packageOfResClass = "dev.dimvlachos.lab.resources" }
