import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.asuka.player"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.asuka.player"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.6.10"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation(project(":player-ui"))
    implementation(project(":player-core"))

    val composeBom = platform("androidx.compose:compose-bom:2026.01.00")
    implementation(composeBom)

    implementation("androidx.activity:activity-compose:1.12.2")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.materialkolor:material-kolor:2.0.0")
}
