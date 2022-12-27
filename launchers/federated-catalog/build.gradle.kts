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
    runtimeOnly(edc.api.observability)
    runtimeOnly(edc.config.filesystem)
    runtimeOnly(edc.ext.http)
    runtimeOnly(edc.boot)

    // DID authentication for IDS API
    runtimeOnly(edc.bundles.identity)

    // API key authentication for Data Management API (also used for CORS support)
//    implementation(edc.ext.auth.tokenBased)

    // To use FileSystem vault e.g. -DuseFsVault="true".Only for non-production usages.
    val useFsVault: Boolean = System.getProperty("useFsVault", "false").toBoolean()
    if (useFsVault) {
        runtimeOnly(edc.vault.filesystem)
    } else {
        runtimeOnly(edc.vault.azure)
    }

    // Federated catalog
    runtimeOnly(fcc.core)
    runtimeOnly(fcc.api)
    runtimeOnly(project(":extensions:refresh-catalog"))

    // DID authentication for IDS API
    runtimeOnly(identityHub.ext.credentialsVerifier)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("app.jar")
}
