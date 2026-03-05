import com.github.jk1.license.render.JsonReportRenderer
import com.github.jk1.license.render.ReportRenderer
import com.github.jk1.license.render.TextReportRenderer

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
        testLogging {
            events("passed", "failed", "skipped")
        }
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
