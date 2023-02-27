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

val edcGroup: String by project

dependencies {
    implementation(project(":extensions:refresh-catalog"))
    implementation(project(":extensions:policies"))

    implementation(edc.core.controlplane)
    implementation(edc.api.observability)
    implementation(edc.api.dataManagement)
    implementation(edc.config.filesystem)
    implementation(edc.ext.http)
    
    // IDS
    implementation(edc.ids) {
        // Workaround for https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1387
        exclude(group = edcGroup, module = "ids-token-validation")
    }

    // API key authentication for Data Management API (also used for CORS support)

    implementation(edc.ext.auth.tokenBased)

    // DID authentication for IDS API
    implementation(edc.bundles.identity)

    // Blob storage container provisioning
    implementation(edc.ext.azure.blob.core)
    implementation(edc.provision.blob)
    // To use FileSystem vault e.g. -DuseFsVault="true".Only for non-production usages.
    val useFsVault: Boolean = System.getProperty("useFsVault", "false").toBoolean()
    if (useFsVault) {
        implementation(edc.vault.filesystem)
    } else {
        implementation(edc.vault.azure)
    }

    // Embedded DPF
    implementation(edc.bundles.dpf)

    // Federated catalog
    implementation(fcc.core)
    implementation(fcc.api)

    // Identity Hub
    implementation(identityHub.core)
    implementation(identityHub.ext.api)
    implementation(identityHub.ext.selfdescription.api)
    implementation(identityHub.core.verifier)
    implementation(identityHub.ext.credentials.jwt)
    implementation(identityHub.ext.verifier.jwt)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("app.jar")
}
