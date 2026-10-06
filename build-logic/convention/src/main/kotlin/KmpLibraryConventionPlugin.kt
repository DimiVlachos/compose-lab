import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A Compose Multiplatform library for Android and iOS: the Android target at the catalog's SDK
 * levels with Android resources on, and the two iOS targets. Each module still sets its own
 * namespace, and names its framework with [iosFramework].
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<KotlinMultiplatformExtension> {
                targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                    compileSdk = libs.int("android-compileSdk")
                    minSdk = libs.int("android-minSdk")
                    compilerOptions { jvmTarget.set(LabJvmTarget) }
                    experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
                }
                iosArm64()
                iosSimulatorArm64()
            }
        }
}

/** Builds each iOS target as a static framework called [baseName], for Xcode to import. */
fun KotlinMultiplatformExtension.iosFramework(baseName: String) {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            this.baseName = baseName
            isStatic = true
        }
    }
}
