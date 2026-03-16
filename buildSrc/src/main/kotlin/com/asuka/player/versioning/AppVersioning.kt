package com.asuka.player.versioning

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) {
    init {
        validateNonNegative("appVersionMajor", major)
        validateTwoDigitPart("appVersionMinor", minor)
        validateTwoDigitPart("appVersionPatch", patch)
        if (versionCodeValue(major, minor, patch) > Int.MAX_VALUE) {
            throw GradleException(
                "versionCode exceeds Int.MAX_VALUE for $major.$minor.$patch",
            )
        }
    }

    val versionName: String = "$major.$minor.$patch"
    val versionCode: Int = versionCodeValue(major, minor, patch).toInt()

    companion object {
        fun parse(
            major: String,
            minor: String,
            patch: String,
        ): AppVersion {
            return AppVersion(
                major = parsePart("appVersionMajor", major),
                minor = parsePart("appVersionMinor", minor),
                patch = parsePart("appVersionPatch", patch),
            )
        }

        fun from(project: Project): AppVersion {
            return parse(
                major = project.requiredVersionProperty("appVersionMajor"),
                minor = project.requiredVersionProperty("appVersionMinor"),
                patch = project.requiredVersionProperty("appVersionPatch"),
            )
        }

        private fun parsePart(name: String, value: String): Int {
            return value.toIntOrNull()
                ?: throw GradleException("$name must be an integer, but was '$value'")
        }

        private fun validateNonNegative(name: String, value: Int) {
            if (value < 0) {
                throw GradleException("$name must be >= 0, but was $value")
            }
        }

        private fun validateTwoDigitPart(name: String, value: Int) {
            if (value !in 0..99) {
                throw GradleException("$name must be between 0 and 99, but was $value")
            }
        }

        private fun versionCodeValue(
            major: Int,
            minor: Int,
            patch: Int,
        ): Long {
            return major.toLong() * 10_000L + minor.toLong() * 100L + patch.toLong()
        }
    }
}

fun Project.readAppVersion(): AppVersion = AppVersion.from(this)

private fun Project.requiredVersionProperty(name: String): String {
    return findProperty(name)
        ?.toString()
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?: throw GradleException("Missing required Gradle property '$name'")
}

abstract class PrintAppVersionTask : DefaultTask() {
    @get:Input
    abstract val major: Property<Int>

    @get:Input
    abstract val minor: Property<Int>

    @get:Input
    abstract val patch: Property<Int>

    @TaskAction
    fun printVersion() {
        val version = AppVersion(
            major = major.get(),
            minor = minor.get(),
            patch = patch.get(),
        )
        println("versionName=${version.versionName}")
        println("versionCode=${version.versionCode}")
    }
}
