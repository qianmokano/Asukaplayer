pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "asuka"

include(":app", ":player-domain", ":player-contract", ":player-platform", ":player-engine", ":player-data", ":player-ui", ":player-runtime", ":player-renderer", ":player-render-api")
