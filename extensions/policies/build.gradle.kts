plugins {
    `java-library`
}

val edcVersion: String by project
val edcGroup = "org.eclipse.dataspaceconnector"

val jupiterVersion: String by project
val mockitoVersion: String by project
val assertj: String by project
val faker: String by project

dependencies {
    api("${edcGroup}:core-spi:${edcVersion}")
    api("${edcGroup}:policy-spi:${edcVersion}")
    api("${edcGroup}:contract-spi:${edcVersion}")

    testImplementation("${edcGroup}:core-base:${edcVersion}")
    //testImplementation("${edcGroup}:common-junit:${edcVersion}")

    testImplementation("org.assertj:assertj-core:${assertj}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testImplementation("com.github.javafaker:javafaker:${faker}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${jupiterVersion}")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
}
