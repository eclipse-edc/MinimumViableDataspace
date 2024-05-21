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
    implementation(libs.edc.spi.core)
    implementation(libs.edc.lib.transform)

    // must be "implementation", since the DefaultCredentialService is still there
    implementation(libs.bundles.controlplane)
    implementation(project(":extensions:common-mocks"))
    implementation(libs.edc.lib.jws2020)
    implementation(libs.edc.identity.vc.ldp)
    implementation(libs.edc.core.connector)
    runtimeOnly(libs.edc.identity.did.web)
    if (project.properties.getOrDefault("useHashicorp", "false") == "true") {
        runtimeOnly(libs.edc.vault.hashicorp)
        println("This runtime compiles with Hashicorp Vault. You will need a properly configured HCV instance.")
    }
    runtimeOnly(libs.bundles.dpf)
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

edcBuild {
    publish.set(false)
}