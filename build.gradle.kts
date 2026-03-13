import com.github.jk1.license.render.JsonReportRenderer
import com.github.jk1.license.render.ReportRenderer
import com.github.jk1.license.render.TextReportRenderer
import org.gradle.api.GradleException
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.work.DisableCachingByDefault

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

data class ArchitectureViolation(
    val file: String,
    val message: String,
)

@DisableCachingByDefault(because = "Fast verification task")
abstract class VerifyArchitectureBoundariesTask : DefaultTask() {
    @get:Input
    abstract val projectRootPath: Property<String>

    @get:Input
    abstract val architectureRootPackages: MapProperty<String, String>

    @get:InputFiles
    abstract val trackedFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val rootDir = java.io.File(projectRootPath.get())
        val violations = mutableListOf<ArchitectureViolation>()

        architectureRootPackages.get().forEach { (dir, packagePrefix) ->
            kotlinFiles(rootDir.resolve(dir)).forEach { file ->
                val packageLine = file.useLines { lines ->
                    lines.firstOrNull { it.startsWith("package ") }?.removePrefix("package ")?.trim()
                }
                if (packageLine == null || !packageLine.startsWith(packagePrefix)) {
                    violations += ArchitectureViolation(
                        file = file.relativeTo(rootDir).path,
                        message = "package must start with $packagePrefix (found: ${packageLine ?: "<missing>"})",
                    )
                }
            }
        }

        val playerUiBuild = rootDir.resolve("player-ui/build.gradle.kts").readText()
        if ("project(\":player-engine\")" in playerUiBuild) {
            violations += ArchitectureViolation(
                file = "player-ui/build.gradle.kts",
                message = "player-ui must not depend directly on :player-engine",
            )
        }
        if ("project(\":player-platform\")" in playerUiBuild) {
            violations += ArchitectureViolation(
                file = "player-ui/build.gradle.kts",
                message = "player-ui must consume playback ports from :player-contract instead of depending on :player-platform",
            )
        }
        listOf("libs.media3.session", "libs.media3.common").forEach { token ->
            if (token in playerUiBuild) {
                violations += ArchitectureViolation(
                    file = "player-ui/build.gradle.kts",
                    message = "player-ui should stay Media3-free; renderer/platform adapters own '$token'",
                )
            }
        }
        if ("libs.media3.ui.compose" in playerUiBuild) {
            violations += ArchitectureViolation(
                file = "player-ui/build.gradle.kts",
                message = "player-ui should not depend on media3-ui-compose directly; renderer adapters belong in :player-renderer",
            )
        }
        if ("libs.activity.compose" in playerUiBuild) {
            violations += ArchitectureViolation(
                file = "player-ui/build.gradle.kts",
                message = "player-ui should not depend on androidx.activity; Android activity entrypoints belong in :player-renderer",
            )
        }

        kotlinFiles(rootDir.resolve("player-ui/src/main/java")).forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("import com.asuka.player.engine.")) {
                    violations += ArchitectureViolation(
                        file = "${file.relativeTo(rootDir).path}:${index + 1}",
                        message = "player-ui main source must not import engine implementation types",
                    )
                }
                if (trimmed.startsWith("import androidx.media3.")) {
                    violations += ArchitectureViolation(
                        file = "${file.relativeTo(rootDir).path}:${index + 1}",
                        message = "player-ui main source must not import Media3 implementation types",
                    )
                }
                if (trimmed.startsWith("import androidx.activity.")) {
                    violations += ArchitectureViolation(
                        file = "${file.relativeTo(rootDir).path}:${index + 1}",
                        message = "player-ui main source must not import androidx.activity entrypoint types",
                    )
                }
                if (trimmed.startsWith("import com.asuka.player.platform.")) {
                    violations += ArchitectureViolation(
                        file = "${file.relativeTo(rootDir).path}:${index + 1}",
                        message = "player-ui main source must depend on contract/render-api/domain ports, not platform adapters",
                    )
                }
            }
        }

        val playerUiContract = rootDir.resolve("player-ui/src/main/java/com/asuka/player/ui/PlayerScreenContract.kt")
        if (playerUiContract.exists()) {
            playerUiContract.readLines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("import androidx.media3.")) {
                    violations += ArchitectureViolation(
                        file = "player-ui/src/main/java/com/asuka/player/ui/PlayerScreenContract.kt:${index + 1}",
                        message = "player-ui public screen contracts must not expose Media3 types",
                    )
                }
            }
            val contractBody = playerUiContract.readText()
            if ("interface PlaybackSurfaceState" in contractBody || "interface PlaybackSurfaceRenderer" in contractBody) {
                violations += ArchitectureViolation(
                    file = "player-ui/src/main/java/com/asuka/player/ui/PlayerScreenContract.kt",
                    message = "player-ui should consume surface contracts from :player-render-api rather than define them locally",
                )
            }
        }

        val platformBuild = rootDir.resolve("player-platform/build.gradle.kts").readText()
        listOf("project(\":player-ui\")", "project(\":player-renderer\")", "project(\":app\")").forEach { token ->
            if (token in platformBuild) {
                violations += ArchitectureViolation(
                    file = "player-platform/build.gradle.kts",
                    message = "player-platform must stay below UI/renderer/app layers; found forbidden token '$token'",
                )
            }
        }
        kotlinFiles(rootDir.resolve("player-platform/src/main/java")).forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("import com.asuka.player.ui.") ||
                    trimmed.startsWith("import com.asuka.player.renderer.") ||
                    trimmed.startsWith("import com.asuka.player.app.")
                ) {
                    violations += ArchitectureViolation(
                        file = "${file.relativeTo(rootDir).path}:${index + 1}",
                        message = "player-platform must not import UI, renderer, or app-layer types",
                    )
                }
            }
        }

        val rendererBuild = rootDir.resolve("player-renderer/build.gradle.kts").readText()
        listOf("project(\":app\")", "project(\":player-data\")", "project(\":player-runtime\")", "project(\":player-engine\")").forEach { token ->
            if (token in rendererBuild) {
                violations += ArchitectureViolation(
                    file = "player-renderer/build.gradle.kts",
                    message = "player-renderer must assemble playback UI without depending on app/data/runtime/engine layers; found forbidden token '$token'",
                )
            }
        }
        kotlinFiles(rootDir.resolve("player-renderer/src/main/java")).forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("import com.asuka.player.app.")) {
                    violations += ArchitectureViolation(
                        file = "${file.relativeTo(rootDir).path}:${index + 1}",
                        message = "player-renderer main source must not import app-layer types",
                    )
                }
            }
        }

        val renderApiBuild = rootDir.resolve("player-render-api/build.gradle.kts").readText()
        listOf(
            "project(\":player-ui\")",
            "project(\":player-renderer\")",
            "project(\":player-platform\")",
            "project(\":app\")",
            "media3",
            "activity-compose",
        ).forEach { token ->
            if (token in renderApiBuild) {
                violations += ArchitectureViolation(
                    file = "player-render-api/build.gradle.kts",
                    message = "player-render-api must stay implementation-free; found forbidden token '$token'",
                )
            }
        }
        kotlinFiles(rootDir.resolve("player-render-api/src/main/java")).forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (
                    trimmed.startsWith("import androidx.media3.") ||
                    trimmed.startsWith("import androidx.activity.") ||
                    trimmed.startsWith("import com.asuka.player.ui.") ||
                    trimmed.startsWith("import com.asuka.player.renderer.") ||
                    trimmed.startsWith("import com.asuka.player.platform.") ||
                    trimmed.startsWith("import com.asuka.player.app.")
                ) {
                    violations += ArchitectureViolation(
                        file = "${file.relativeTo(rootDir).path}:${index + 1}",
                        message = "player-render-api must not import implementation-layer types",
                    )
                }
            }
        }

        val appBuild = rootDir.resolve("app/build.gradle.kts").readText()
        if ("project(\":player-ui\")" in appBuild) {
            violations += ArchitectureViolation(
                file = "app/build.gradle.kts",
                message = "app should depend on :player-renderer rather than :player-ui directly for playback UI entrypoints",
            )
        }

        val appManifest = rootDir.resolve("app/src/main/AndroidManifest.xml").readText()
        if ("com.asuka.player.ui.activity.PlaybackActivity" in appManifest) {
            violations += ArchitectureViolation(
                file = "app/src/main/AndroidManifest.xml",
                message = "Playback activity manifest entrypoint must come from :player-renderer, not :player-ui",
            )
        }
        if ("com.asuka.player.renderer.activity.PlaybackActivity" !in appManifest) {
            violations += ArchitectureViolation(
                file = "app/src/main/AndroidManifest.xml",
                message = "App manifest must register com.asuka.player.renderer.activity.PlaybackActivity as the playback entrypoint",
            )
        }

        val forbiddenContractImportPrefixes = listOf(
            "import android.",
            "import androidx.media3.",
            "import androidx.room.",
            "import androidx.datastore.",
            "import kotlinx.parcelize.",
            "import com.asuka.player.platform.",
            "import com.asuka.player.engine.",
            "import com.asuka.player.runtime.",
            "import com.asuka.player.app.",
            "import com.asuka.player.ui.",
        )

        val contractBuild = rootDir.resolve("player-contract/build.gradle.kts").readText()
        val forbiddenContractBuildTokens = listOf(
            "android.application",
            "android.library",
            "kotlin.android",
            "kotlin-parcelize",
            "media3",
            "room",
            "datastore",
            "project(\":player-platform\")",
            "project(\":player-engine\")",
            "project(\":player-runtime\")",
            "project(\":app\")",
            "project(\":player-ui\")",
        )
        forbiddenContractBuildTokens.forEach { token ->
            if (token in contractBuild) {
                violations += ArchitectureViolation(
                    file = "player-contract/build.gradle.kts",
                    message = "player-contract build must stay platform-free; found forbidden token '$token'",
                )
            }
        }

        kotlinFiles(rootDir.resolve("player-contract/src/main/java")).forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (forbiddenContractImportPrefixes.any(trimmed::startsWith)) {
                    violations += ArchitectureViolation(
                        file = "${file.relativeTo(rootDir).path}:${index + 1}",
                        message = "player-contract must not import platform or higher-layer types",
                    )
                }
            }
        }

        if (violations.isNotEmpty()) {
            val report = violations.joinToString("\n") { "- ${it.file}: ${it.message}" }
            throw GradleException("Architecture boundary violations found:\n$report")
        }
    }

    private fun kotlinFiles(dir: java.io.File): Sequence<java.io.File> {
        if (!dir.exists()) return emptySequence()
        return dir.walkTopDown().filter { it.isFile && it.extension == "kt" }
    }
}

@DisableCachingByDefault(because = "Fast verification task")
abstract class VerifySourceFileSizesTask : DefaultTask() {
    @get:Input
    abstract val projectRootPath: Property<String>

    @get:Input
    abstract val explicitBudgetPaths: ListProperty<String>

    @get:InputFiles
    abstract val trackedFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val rootDir = java.io.File(projectRootPath.get())
        val baselineFile = rootDir.resolve("tools/architecture/file-size-baselines.properties")
        val baselineLimits = linkedMapOf<String, Int>()
        if (baselineFile.exists()) {
            baselineFile.readLines()
                .map(String::trim)
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .forEach { line ->
                    val separatorIndex = line.indexOf('=')
                    require(separatorIndex > 0) {
                        "Invalid line in ${baselineFile.path}: $line"
                    }
                    baselineLimits[line.substring(0, separatorIndex).trim()] =
                        line.substring(separatorIndex + 1).trim().toInt()
                }
        }

        val pageRegex = Regex(""".*(Page|Pages|Screen|NavHost|Sheet|Dialog)\.kt$""")
        val orchestrationRegex = Regex(
            """.*(ViewModel|Coordinator|Host|Activity|Repositories|Store|Impl|Installer|Slice|Slices|Driver|Indexing)\.kt$""",
        )
        val violations = mutableListOf<String>()
        val trackedRelativePaths = mutableSetOf<String>()

        explicitBudgetPaths.get().forEach { dir ->
            kotlinFiles(rootDir.resolve(dir)).forEach { file ->
                val relativePath = file.relativeTo(rootDir).path
                trackedRelativePaths += relativePath
                val lineCount = file.readLines().size
                val budget = baselineLimits[relativePath]
                    ?: when {
                        pageRegex.matches(file.name) -> 320
                        orchestrationRegex.matches(file.name) -> 280
                        else -> null
                    }
                if (budget != null && lineCount > budget) {
                    violations += "- $relativePath: $lineCount lines exceeds budget $budget"
                }
            }
        }

        baselineLimits.keys
            .filterNot(trackedRelativePaths::contains)
            .forEach { stalePath ->
                violations += "- $stalePath: stale baseline entry points to a missing or untracked file"
            }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Source file size budgets exceeded:\n" + violations.joinToString("\n"),
            )
        }
    }

    private fun kotlinFiles(dir: java.io.File): Sequence<java.io.File> {
        if (!dir.exists()) return emptySequence()
        return dir.walkTopDown().filter { it.isFile && it.extension == "kt" }
    }
}

val architectureRootPackageMap = mapOf(
    "app/src/main/java" to "com.asuka.player.app",
    "player-contract/src/main/java" to "com.asuka.player.contract",
    "player-data/src/main/java" to "com.asuka.player.data",
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
