plugins {
    `java-library`
}

val edcVersion: String by project
val edcGroup: String by project
val registrationServiceVersion: String by project
val registrationServiceGroup: String by project
val jupiterVersion: String by project
val mockitoVersion: String by project
val assertj: String by project

dependencies {
    implementation("${edcGroup}:common-util:${edcVersion}")
    implementation("${edcGroup}:federated-catalog-spi:${edcVersion}")
    implementation("${edcGroup}:identity-did-core:${edcVersion}")
    implementation("${edcGroup}:identity-did-web:${edcVersion}")
    implementation("${edcGroup}:ids-spi:${edcVersion}")
    implementation("${registrationServiceGroup}:registration-service-client:${registrationServiceVersion}")

    testImplementation("org.assertj:assertj-core:${assertj}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${jupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
}
