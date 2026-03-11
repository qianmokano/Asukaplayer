pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "asuka"

include(":app", ":player-domain", ":player-contract", ":player-platform", ":player-engine", ":player-data", ":player-ui", ":player-runtime")
