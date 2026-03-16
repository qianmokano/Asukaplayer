package com.asuka.player.versioning

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder

class AppVersioningTest {
    @Test
    fun `parse computes version name and version code`() {
        val version = AppVersion.parse(
            major = "2",
            minor = "4",
            patch = "15",
        )

        assertEquals("2.4.15", version.versionName)
        assertEquals(20_415, version.versionCode)
    }

    @Test
    fun `readAppVersion reads root properties`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.extraProperties["appVersionMajor"] = "1"
        project.extensions.extraProperties["appVersionMinor"] = "0"
        project.extensions.extraProperties["appVersionPatch"] = "7"

        val version = project.readAppVersion()

        assertEquals("1.0.7", version.versionName)
        assertEquals(10_007, version.versionCode)
    }

    @Test
    fun `parse rejects non integer version parts`() {
        val error = assertFailsWith<GradleException> {
            AppVersion.parse(
                major = "1",
                minor = "beta",
                patch = "0",
            )
        }

        assertEquals(
            "appVersionMinor must be an integer, but was 'beta'",
            error.message,
        )
    }

    @Test
    fun `parse rejects parts that exceed two digits`() {
        val error = assertFailsWith<GradleException> {
            AppVersion.parse(
                major = "1",
                minor = "100",
                patch = "0",
            )
        }

        assertEquals(
            "appVersionMinor must be between 0 and 99, but was 100",
            error.message,
        )
    }

    @Test
    fun `readAppVersion requires all properties`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.extraProperties["appVersionMajor"] = "0"
        project.extensions.extraProperties["appVersionMinor"] = "1"

        val error = assertFailsWith<GradleException> {
            project.readAppVersion()
        }

        assertEquals(
            "Missing required Gradle property 'appVersionPatch'",
            error.message,
        )
    }
}
