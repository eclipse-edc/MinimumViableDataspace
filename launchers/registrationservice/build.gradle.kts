/*
 *  Copyright (c) 2022 Microsoft Corporation
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

dependencies {
    implementation(registrationService.core)
    implementation(registrationService.core.credential.service)
    implementation(registrationService.ext.api)

    implementation(edc.identity.did.web)
    implementation(edc.identity.did.core)
    implementation(edc.core.connector)
    runtimeOnly(edc.boot)
    implementation(edc.core.controlplane)
    implementation(edc.api.observability)
    implementation(edc.core.micrometer)
    runtimeOnly(edc.micrometer.jetty)
    runtimeOnly(edc.micrometer.jersey)
    implementation(edc.config.filesystem)
    implementation(identityHub.ext.api)
    implementation(identityHub.ext.selfdescription.api)
    implementation(identityHub.core.verifier)
    implementation(identityHub.ext.credentials.jwt)
    implementation(identityHub.ext.verifier.jwt)

    // To use FileSystem vault e.g. -DuseFsVault="true".Only for non-production usages.
    val useFsVault: Boolean = System.getProperty("useFsVault", "false").toBoolean()
    if (useFsVault) {
        implementation(edc.vault.filesystem)
    } else {
        implementation(edc.vault.azure)
    }
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("app.jar")
}
