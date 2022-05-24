/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

val edcVersion: String by project
val edcGroup: String by project

dependencies {
    implementation(project(":extensions:refresh-catalog"))
    implementation(project(":extensions:mock-credentials-verifier"))
    implementation("${edcGroup}:core:${edcVersion}")
    implementation("${edcGroup}:ids:${edcVersion}")
    implementation("${edcGroup}:observability-api:${edcVersion}")
    implementation("${edcGroup}:data-management-api:${edcVersion}")
    implementation("${edcGroup}:filesystem-configuration:${edcVersion}")
    implementation("${edcGroup}:http:${edcVersion}")

    // API key authentication for Data Management API (also used for CORS support)
    implementation("${edcGroup}:auth-tokenbased:${edcVersion}")

    // DID authentication for IDS API
    implementation("${edcGroup}:identity-did-core:${edcVersion}")
    implementation("${edcGroup}:identity-did-service:${edcVersion}")
    implementation("${edcGroup}:identity-did-web:${edcVersion}")

    // Blob storage container provisioning
    implementation("${edcGroup}:blobstorage:${edcVersion}")
    implementation("${edcGroup}:azure-vault:${edcVersion}")

    // Embedded DPF
    implementation("${edcGroup}:data-plane-transfer-client:${edcVersion}")
    implementation("${edcGroup}:data-plane-selector-client:${edcVersion}")
    implementation("${edcGroup}:data-plane-selector-store:${edcVersion}")
    implementation("${edcGroup}:data-plane-selector-core:${edcVersion}")
    implementation("${edcGroup}:data-plane-framework:${edcVersion}")
    implementation("${edcGroup}:data-plane-azure-storage:${edcVersion}")

    // Federated catalog
    implementation("${edcGroup}:catalog-cache:${edcVersion}")
}

application {
    mainClass.set("org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("app.jar")
}
