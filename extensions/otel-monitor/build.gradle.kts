plugins {
    `java-library`
}

dependencies {
    implementation(libs.edc.core.runtime)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.exporter.otlp)
}
