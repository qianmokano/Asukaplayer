import com.asuka.player.versioning.readAppVersion
import java.util.Locale
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val appVersion = project.readAppVersion()
val debugBuildLabel = providers.gradleProperty("appBuildLabel")
    .orElse(providers.environmentVariable("ASUKA_BUILD_LABEL"))
    .map(::sanitizeBuildLabel)
    .orElse("")
val debugInstallId = providers.gradleProperty("appInstallId")
    .orElse(providers.environmentVariable("ASUKA_INSTALL_ID"))
    .map(::sanitizeInstallId)
    .orElse("")
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

android {
    namespace = "com.asuka.player"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.asuka.player"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = appVersion.versionCode
        versionName = appVersion.versionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = localProperties.getProperty("release.storeFile")
                ?.takeIf { it.isNotBlank() }
                ?.let(rootProject::file)
            storePassword = localProperties.getProperty("release.storePassword")
                ?.takeIf { it.isNotBlank() }
            keyAlias = localProperties.getProperty("release.keyAlias")
                ?.takeIf { it.isNotBlank() }
            keyPassword = localProperties.getProperty("release.keyPassword")
                ?.takeIf { it.isNotBlank() }
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        debug {
            val buildLabel = debugBuildLabel.get()
            versionNameSuffix = if (buildLabel.isNotEmpty()) "-debug+$buildLabel" else "-debug"

            val installId = debugInstallId.get()
            applicationIdSuffix = if (installId.isNotEmpty()) ".b$installId" else ".debug"
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
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
    implementation(project(":player-data"))
    implementation(project(":player-engine"))
    implementation(project(":player-platform"))
    implementation(project(":player-renderer"))
    implementation(project(":player-runtime"))

    implementation(libs.activity.compose)
    implementation(libs.appcompat)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.compose.materialmotion)
    implementation(libs.materialkolor)
    implementation(libs.room.runtime)

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}

private fun sanitizeBuildLabel(value: String): String {
    return value
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9._-]+"), "-")
        .trim('.', '-', '_')
}

private fun sanitizeInstallId(value: String): String {
    return value
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "")
        .take(24)
}
