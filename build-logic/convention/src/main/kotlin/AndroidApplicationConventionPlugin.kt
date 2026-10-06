import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/**
 * An Android app with Compose: the catalog's SDK levels and app version, Java 17, and lint that
 * fails the build on errors. Each app still sets its own namespace and application id.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<ApplicationExtension> {
                compileSdk = libs.int("android-compileSdk")
                defaultConfig {
                    minSdk = libs.int("android-minSdk")
                    targetSdk = libs.int("android-targetSdk")
                    versionCode = libs.int("app-versionCode")
                    versionName = libs.string("app-versionName")
                }
                packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                buildFeatures { compose = true }
                lint {
                    abortOnError = true
                    warningsAsErrors = false
                }
            }
            tasks.withType<KotlinJvmCompile>().configureEach {
                compilerOptions.jvmTarget.set(LabJvmTarget)
            }
        }
}
