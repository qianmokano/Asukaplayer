import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.asuka.player.core"
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
    implementation(libs.annotation)
    implementation(libs.coroutines.core)
    api(libs.media3.common)
    api(libs.media3.session)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    testImplementation(project(":player-data"))
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
