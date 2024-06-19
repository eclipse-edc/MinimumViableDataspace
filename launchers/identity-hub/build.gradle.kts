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
    if (project.properties.getOrDefault("useHashicorp", "false") == "true") {
        runtimeOnly(libs.edc.vault.hashicorp)
        println("This runtime compiles with Hashicorp Vault. You will need a properly configured HCV instance.")
    }
    runtimeOnly(project(":extensions:superuser-seed"))

    runtimeOnly(libs.bundles.management.api)
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
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("identity-hub.jar")
}

edcBuild {
    publish.set(false)
}
