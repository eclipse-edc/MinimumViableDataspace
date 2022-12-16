plugins {
    `java-library`
}

dependencies {
    implementation(fcc.spi)
    implementation(edc.spi.ids)
    implementation(edc.util)
    implementation(edc.identity.did.core)
    implementation(edc.identity.did.web)
    implementation(registrationService.core.client)
}
