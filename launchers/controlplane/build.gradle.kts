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
    implementation(project(":extensions:did-example-resolver"))
    implementation(project(":extensions:dcp-impl")) // some patches/impls for DCP
    runtimeOnly(project(":extensions:catalog-node-resolver")) // to trigger the federated catalog
    implementation(libs.edc.spi.core) // we need some constants

    implementation(libs.bundles.controlplane)
    implementation(libs.bundles.dcp)
    implementation(libs.edc.core.connector)

    if (project.properties.getOrDefault("persistence", "false") == "true") {
        runtimeOnly(libs.edc.vault.hashicorp)
        runtimeOnly(libs.bundles.sql.edc)
        runtimeOnly(libs.bundles.sql.fc)
        runtimeOnly(libs.edc.sts.remote.client)
        println("This runtime compiles with a remote STS client, Hashicorp Vault and PostgreSQL. You will need properly configured Postgres and HCV instances.")
    }
    runtimeOnly(libs.bundles.dpf)
    runtimeOnly(libs.edc.api.version)

}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xml")
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

edcBuild {
    publish.set(false)
}