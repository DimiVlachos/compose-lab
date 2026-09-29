import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.cmp.foundation)
    implementation(libs.cmp.material3)
    implementation(libs.cmp.resources)
    implementation(libs.cmp.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.a2ui.compose.runtime)
    implementation(libs.a2ui.compose.ui)
    implementation(libs.a2ui.engine)
    implementation(libs.a2ui.material3)
    implementation(libs.a2ui.model)
    implementation(libs.anthropic.java)
    implementation(libs.openai.java)
    implementation(libs.kermit)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test.junit)
    // org.json is stubbed in the Android unit-test jar; the JVM tests use the real one.
    testImplementation(libs.org.json)
    // So is android.util.JsonReader, which the A2UI parser reads with; Gson's is the same API.
    testImplementation(libs.gson)
    testImplementation(libs.junit)
    testImplementation(libs.konsist)
}

android {
    namespace = "dev.dimvlachos.lab"
    // The A2UI renderer needs the 37.1 platform.
    compileSdk {
        version = release(libs.versions.android.compileSdk.get().toInt()) { minorApiLevel = 1 }
    }

    defaultConfig {
        applicationId = "dev.dimvlachos.lab"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        val localProperties = Properties()
        rootProject
            .file("local.properties")
            .takeIf { it.exists() }
            ?.inputStream()
            ?.use {
                localProperties.load(it)
            }
        // The concierge demo's agent: OpenAI when openai.apiKey is set, otherwise Claude.
        fun localString(key: String, default: String = "") =
            "\"${localProperties.getProperty(key, default)}\""
        buildConfigField("String", "OPENAI_API_KEY", localString("openai.apiKey"))
        buildConfigField("String", "OPENAI_MODEL", localString("openai.model", "gpt-6-luna"))
        buildConfigField("String", "ANTHROPIC_API_KEY", localString("anthropic.apiKey"))
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
