import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.asuka.player.runtime"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
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
    implementation(project(":player-engine"))
    implementation(project(":player-data"))

    implementation(libs.coroutines.core)

    testImplementation(project(":player-platform"))
    testImplementation(project(":player-engine"))
    testImplementation(project(":player-data"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
