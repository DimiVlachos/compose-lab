import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins { alias(libs.plugins.lab.kmp.library) }

kotlin {
    explicitApi()
    android { namespace = "dev.dimvlachos.lab.shared" }
    iosFramework(baseName = "ComposeApp")
    // Only for the shared tests: they run on the JVM, drawing with the same Skia as iOS, far
    // faster than as an iOS simulator binary. Nothing ships for it.
    jvm()
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            // iOS and the JVM both draw with Skia, so they share the code that talks to it.
            group("skiko") {
                group("ios")
                withJvm()
            }
        }
    }

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
        // Skia for the machine the tests run on, and a main dispatcher, as a desktop app has.
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

compose.resources { packageOfResClass = "dev.dimvlachos.lab.resources" }

// The shared tests' classes run spread over several JVMs at once, half the machine's cores.
tasks.named<Test>("jvmTest") {
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    // CI splits the classes over several machines: -PtestShards=3 -PtestShard=0 runs a third of
    // them, chosen by a hash of each class's path. The salt was picked so the three weigh alike.
    val shards = providers.gradleProperty("testShards").orNull?.toInt()
    val shard = providers.gradleProperty("testShard").orNull?.toInt()
    if (shards != null && shard != null) {
        val salt = 112
        exclude { element ->
            val path = element.path
            !element.isDirectory &&
                path.endsWith(".class") &&
                !path.contains('$') &&
                Math.floorMod("$salt$path".hashCode(), shards) != shard
        }
    }
    // These pin timing the JVM test host doesn't give: images load on a real thread outside the
    // test clock, a swipe arrives as fewer moves, and a recomposition count shifts under the
    // load of the parallel forks. They run in the iOS simulator suite on main.
    filter {
        excludeTestsMatching("*ProfileGalleryUiTest.aCloseDuringTheOpenWaitsForTheMorphToLand")
        excludeTestsMatching(
            "*MagnetTableUiTest.aJoinedMagnetFlickedHomeGoesAloneAndTheOtherStaysPut"
        )
        excludeTestsMatching("*SnowGlobeUiTest.steadyFramesDoNotRecompose")
    }
}
