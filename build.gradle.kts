import com.asuka.player.build.PrintAppVersionTask
import com.github.jk1.license.render.JsonReportRenderer
import com.github.jk1.license.render.ReportRenderer
import com.github.jk1.license.render.TextReportRenderer
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    `jvm-toolchains`
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.license.report)
}

subprojects {
    tasks.withType<Test>().configureEach {
        javaLauncher.set(
            javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(21))
            },
        )
        dependsOn(
            rootProject.tasks.named("verifyArchitectureBoundaries"),
            rootProject.tasks.named("verifySourceFileSizes"),
        )
        testLogging {
            events("passed", "failed", "skipped")
        }
    }

    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(
            rootProject.tasks.named("verifyArchitectureBoundaries"),
            rootProject.tasks.named("verifySourceFileSizes"),
        )
    }
}

licenseReport {
    outputDir = "${layout.buildDirectory.get()}/reports/third-party-notices"
    renderers = arrayOf<ReportRenderer>(
        TextReportRenderer("THIRD-PARTY-NOTICES.txt"),
        JsonReportRenderer(),
    )
}

tasks.register("generateThirdPartyNotices") {
    group = "reporting"
    description = "Generates third-party notices for all resolved dependencies."
    dependsOn("generateLicenseReport")
}

tasks.register<PrintAppVersionTask>("printAppVersion") {
    group = "help"
    description = "Prints the centralized app versionName and versionCode."
    major.set(providers.gradleProperty("appVersionMajor").map { it.toInt() })
    minor.set(providers.gradleProperty("appVersionMinor").map { it.toInt() })
    patch.set(providers.gradleProperty("appVersionPatch").map { it.toInt() })
}

val architectureRootPackageMap = mapOf(
    "app/src/main/java" to "com.asuka.player.app",
    "player-contract/src/main/java" to "com.asuka.player.contract",
    "player-data/src/main/java" to "com.asuka.player.data",
    "player-domain/src/main/java" to "com.asuka.player.domain",
    "player-platform/src/main/java" to "com.asuka.player.platform",
    "player-render-api/src/main/java" to "com.asuka.player.render.api",
    "player-renderer/src/main/java" to "com.asuka.player.renderer",
    "player-runtime/src/main/java" to "com.asuka.player.runtime",
    "player-engine/src/main/java" to "com.asuka.player.engine",
    "player-ui/src/main/java" to "com.asuka.player.ui",
)

tasks.register<VerifyArchitectureBoundariesTask>("verifyArchitectureBoundaries") {
    group = "verification"
    description = "Checks module boundary and package ownership rules."
    projectRootPath.set(layout.projectDirectory.asFile.absolutePath)
    architectureRootPackages.putAll(architectureRootPackageMap)
    trackedFiles.from(
        architectureRootPackageMap.keys.map(layout.projectDirectory::dir),
        layout.projectDirectory.file("player-ui/build.gradle.kts"),
        layout.projectDirectory.file("player-platform/build.gradle.kts"),
        layout.projectDirectory.file("player-render-api/build.gradle.kts"),
        layout.projectDirectory.file("player-renderer/build.gradle.kts"),
        layout.projectDirectory.file("player-contract/build.gradle.kts"),
        layout.projectDirectory.file("player-engine/build.gradle.kts"),
        layout.projectDirectory.file("player-data/build.gradle.kts"),
        layout.projectDirectory.file("player-domain/build.gradle.kts"),
        layout.projectDirectory.file("player-runtime/build.gradle.kts"),
        layout.projectDirectory.file("app/build.gradle.kts"),
        layout.projectDirectory.file("app/src/main/AndroidManifest.xml"),
    )
}

tasks.register<VerifySourceFileSizesTask>("verifySourceFileSizes") {
    group = "verification"
    description = "Checks file-size budgets for state/orchestration and page-level source files."
    projectRootPath.set(layout.projectDirectory.asFile.absolutePath)
    explicitBudgetPaths.set(
        architectureRootPackageMap.keys.toList(),
    )
    trackedFiles.from(
        explicitBudgetPaths.get().map(layout.projectDirectory::dir),
        layout.projectDirectory.file("tools/architecture/file-size-baselines.properties"),
    )
}
