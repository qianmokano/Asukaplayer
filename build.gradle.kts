plugins {
    id("org.jetbrains.kotlin.jvm") version "2.3.0" apply false
    id("org.jetbrains.kotlin.android") version "2.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0" apply false
    id("com.android.library") version "8.13.2" apply false
}

subprojects {
    tasks.withType<Test>().configureEach {
        testLogging {
            events("passed", "failed", "skipped")
        }
    }
}
