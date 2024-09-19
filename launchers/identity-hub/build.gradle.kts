/*
*  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
*
*  This program and the accompanying materials are made available under the
*  terms of the Apache License, Version 2.0 which is available at
*  https://www.apache.org/licenses/LICENSE-2.0
*
*  SPDX-License-Identifier: Apache-2.0
*
*  Contributors:
*       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial API and Implementation
*
*/

plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    runtimeOnly(libs.bundles.identityhub)
    runtimeOnly(libs.edc.api.observability)
    if (project.properties.getOrDefault("persistence", "false") == "true") {
        runtimeOnly(libs.edc.vault.hashicorp)
        runtimeOnly(libs.bundles.sql.ih)
        runtimeOnly(libs.bundles.sts)
        println("This runtime compiles with an internal STS, Hashicorp Vault and PostgreSQL. You will need properly configured Postgres and HCV instances.")
    }
    runtimeOnly(project(":extensions:superuser-seed"))

    runtimeOnly(libs.bundles.identity.api)

    implementation(libs.bundles.did)
    implementation(project(":extensions:did-example-resolver"))
    implementation(libs.bundles.connector)
    implementation(libs.edc.ih.spi.store)
    implementation(libs.edc.identity.vc.ldp)
    implementation(libs.edc.ih.lib.credentialquery)

    testImplementation(libs.edc.lib.crypto)
    testImplementation(libs.edc.lib.keys)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xml")
    mergeServiceFiles()
    archiveFileName.set("identity-hub.jar")
}

edcBuild {
    publish.set(false)
}
