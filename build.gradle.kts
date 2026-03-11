import com.github.jk1.license.render.JsonReportRenderer
import com.github.jk1.license.render.ReportRenderer
import com.github.jk1.license.render.TextReportRenderer
import org.gradle.api.GradleException
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.license.report)
}

subprojects {
    tasks.withType<Test>().configureEach {
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

val architectureRootPackages = mapOf(
    "app/src/main/java" to "com.asuka.player.app",
    "player-contract/src/main/java" to "com.asuka.player.contract",
    "player-platform/src/main/java" to "com.asuka.player.platform",
    "player-runtime/src/main/java" to "com.asuka.player.runtime",
    "player-engine/src/main/java" to "com.asuka.player.engine",
    "player-ui/src/main/java" to "com.asuka.player.ui",
)

tasks.register("verifyArchitectureBoundaries") {
    group = "verification"
    description = "Checks module boundary and package ownership rules."

    doLast {
        val violations = mutableListOf<ArchitectureViolation>()

        architectureRootPackages.forEach { (dir, packagePrefix) ->
            fileTree(dir).matching { include("**/*.kt") }.files.forEach { file ->
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

        val playerUiBuild = file("player-ui/build.gradle.kts").readText()
        if ("project(\":player-engine\")" in playerUiBuild) {
            violations += ArchitectureViolation(
                file = "player-ui/build.gradle.kts",
                message = "player-ui must not depend directly on :player-engine",
            )
        }

        fileTree("player-ui/src/main/java").matching { include("**/*.kt") }.files.forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("import com.asuka.player.engine.")) {
                    violations += ArchitectureViolation(
                        file = "${file.relativeTo(rootDir).path}:${index + 1}",
                        message = "player-ui main source must not import engine implementation types",
                    )
                }
            }
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

        val contractBuild = file("player-contract/build.gradle.kts").readText()
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

        fileTree("player-contract/src/main/java").matching { include("**/*.kt") }.files.forEach { file ->
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
}

tasks.register("verifySourceFileSizes") {
    group = "verification"
    description = "Checks file-size budgets for state/orchestration and page-level source files."

    doLast {
        val baselineFile = file("tools/architecture/file-size-baselines.properties")
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
        val orchestrationRegex = Regex(""".*(ViewModel|Coordinator|Host|Activity|Repositories)\.kt$""")
        val explicitBudgetPaths = setOf(
            "app/src/main/java/com/asuka/player/app",
            "player-ui/src/main/java/com/asuka/player/ui",
            "player-runtime/src/main/java/com/asuka/player/runtime",
        )

        val violations = mutableListOf<String>()

        explicitBudgetPaths.forEach { dir ->
            fileTree(dir).matching { include("**/*.kt") }.files.forEach { file ->
                val relativePath = file.relativeTo(rootDir).path
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

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Source file size budgets exceeded:\n" + violations.joinToString("\n"),
            )
        }
    }
}
