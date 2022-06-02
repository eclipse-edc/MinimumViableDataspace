plugins {
    `java-library`
}

val edcVersion: String by project
val edcGroup: String by project

dependencies {
    api("${edcGroup}:ids-spi:${edcVersion}")
    api("${edcGroup}:contract-spi:${edcVersion}")
}