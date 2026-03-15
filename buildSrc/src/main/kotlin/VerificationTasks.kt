import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.work.DisableCachingByDefault

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

        checkModuleBoundary(rootDir, violations, "player-platform",
            forbiddenBuildTokens = listOf("project(\":player-ui\")", "project(\":player-renderer\")", "project(\":app\")"),
            forbiddenImportPrefixes = listOf("import com.asuka.player.ui.", "import com.asuka.player.renderer.", "import com.asuka.player.app."),
        )

        checkModuleBoundary(rootDir, violations, "player-renderer",
            forbiddenBuildTokens = listOf("project(\":app\")", "project(\":player-data\")", "project(\":player-runtime\")", "project(\":player-engine\")"),
            forbiddenImportPrefixes = listOf("import com.asuka.player.app."),
        )

        checkModuleBoundary(rootDir, violations, "player-render-api",
            forbiddenBuildTokens = listOf("project(\":player-ui\")", "project(\":player-renderer\")", "project(\":player-platform\")", "project(\":app\")", "media3", "activity-compose"),
            forbiddenImportPrefixes = listOf("import androidx.media3.", "import androidx.activity.", "import com.asuka.player.ui.", "import com.asuka.player.renderer.", "import com.asuka.player.platform.", "import com.asuka.player.app."),
        )

        val appBuild = rootDir.resolve("app/build.gradle.kts").readText()
        if ("project(\":player-ui\")" in appBuild) {
            violations += ArchitectureViolation(
                file = "app/build.gradle.kts",
                message = "app should depend on :player-renderer rather than :player-ui directly for playback UI entrypoints",
            )
        }

        val appManifest = rootDir.resolve("app/src/main/AndroidManifest.xml").readText()

        val appSource = rootDir.resolve("app/src/main/java/com/asuka/player/app/AsuraPlayerApp.kt")
        if (appSource.exists()) {
            val appBody = appSource.readText()
            listOf("PlaybackDependenciesProvider", "MainActivityDependenciesProvider").forEach { iface ->
                if (iface !in appBody) {
                    violations += ArchitectureViolation(
                        file = appSource.relativeTo(rootDir).path,
                        message = "Application class must implement $iface for framework component dependency resolution",
                    )
                }
            }
        }

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
            "import android.", "import androidx.media3.", "import androidx.room.",
            "import androidx.datastore.", "import kotlinx.parcelize.",
            "import com.asuka.player.platform.", "import com.asuka.player.engine.",
            "import com.asuka.player.runtime.", "import com.asuka.player.app.",
            "import com.asuka.player.ui.",
        )
        checkModuleBoundary(rootDir, violations, "player-contract",
            forbiddenBuildTokens = listOf("android.application", "android.library", "kotlin.android", "kotlin-parcelize", "media3", "room", "datastore", "project(\":player-platform\")", "project(\":player-engine\")", "project(\":player-runtime\")", "project(\":app\")", "project(\":player-ui\")"),
            forbiddenImportPrefixes = forbiddenContractImportPrefixes,
        )

        checkModuleBoundary(rootDir, violations, "player-domain",
            forbiddenBuildTokens = listOf("android.application", "android.library"),
            forbiddenImportPrefixes = listOf("import android.", "import androidx."),
        )

        checkModuleBoundary(rootDir, violations, "player-engine",
            forbiddenBuildTokens = listOf("project(\":player-ui\")", "project(\":player-renderer\")", "project(\":app\")", "project(\":player-runtime\")", "project(\":player-render-api\")", "project(\":player-domain\")"),
            forbiddenImportPrefixes = listOf("import com.asuka.player.ui.", "import com.asuka.player.renderer.", "import com.asuka.player.app.", "import com.asuka.player.runtime."),
        )

        checkModuleBoundary(rootDir, violations, "player-data",
            forbiddenBuildTokens = listOf("project(\":player-ui\")", "project(\":player-renderer\")", "project(\":app\")", "project(\":player-runtime\")", "project(\":player-engine\")", "project(\":player-platform\")", "project(\":player-render-api\")", "project(\":player-domain\")"),
            forbiddenImportPrefixes = listOf("import com.asuka.player.ui.", "import com.asuka.player.renderer.", "import com.asuka.player.app.", "import com.asuka.player.runtime.", "import com.asuka.player.engine.", "import com.asuka.player.platform."),
        )

        checkModuleBoundary(rootDir, violations, "player-runtime",
            forbiddenBuildTokens = listOf("project(\":player-ui\")", "project(\":player-renderer\")", "project(\":app\")", "project(\":player-render-api\")", "project(\":player-domain\")", "project(\":player-engine\")"),
            forbiddenImportPrefixes = listOf("import com.asuka.player.ui.", "import com.asuka.player.renderer.", "import com.asuka.player.app.", "import com.asuka.player.engine."),
        )

        if (violations.isNotEmpty()) {
            val report = violations.joinToString("\n") { "- ${it.file}: ${it.message}" }
            throw GradleException("Architecture boundary violations found:\n$report")
        }
    }

    private fun checkModuleBoundary(
        rootDir: java.io.File,
        violations: MutableList<ArchitectureViolation>,
        module: String,
        forbiddenBuildTokens: List<String>,
        forbiddenImportPrefixes: List<String>,
    ) {
        val buildFile = rootDir.resolve("$module/build.gradle.kts")
        if (buildFile.exists()) {
            val buildText = buildFile.readText()
            forbiddenBuildTokens.forEach { token ->
                if (token in buildText) {
                    violations += ArchitectureViolation(
                        file = "$module/build.gradle.kts",
                        message = "$module must not contain forbidden token '$token'",
                    )
                }
            }
        }
        kotlinFiles(rootDir.resolve("$module/src/main/java")).forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (forbiddenImportPrefixes.any(trimmed::startsWith)) {
                    violations += ArchitectureViolation(
                        file = "${file.relativeTo(rootDir).path}:${index + 1}",
                        message = "$module must not import forbidden types",
                    )
                }
            }
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
