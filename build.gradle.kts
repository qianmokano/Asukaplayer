plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

subprojects {
    tasks.withType<Test>().configureEach {
        testLogging {
            events("passed", "failed", "skipped")
        }
    }
}
