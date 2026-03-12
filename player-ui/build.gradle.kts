import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.asuka.player.ui"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.failOnNoDiscoveredTests = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":player-contract"))
    implementation(project(":player-platform"))
    implementation(project(":player-render-api"))
    implementation(project(":player-domain"))

    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.appcompat)

    implementation(libs.coroutines.core)

    testImplementation(project(":player-data"))
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
