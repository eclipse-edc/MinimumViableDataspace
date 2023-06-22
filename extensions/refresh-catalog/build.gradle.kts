plugins {
    `java-library`
}

dependencies {
    api(libs.rs.spi.core)
    api(libs.fc.spi.core)

    implementation(libs.edc.ext.identity.did.core)
    implementation(libs.edc.ext.identity.did.web)
    implementation(libs.rs.core.client)

    testImplementation(libs.edc.core.junit)
}
