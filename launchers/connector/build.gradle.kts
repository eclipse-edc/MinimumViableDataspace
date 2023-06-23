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
    alias(libs.plugins.shadow)
}

var distTar = tasks.getByName("distTar")
var distZip = tasks.getByName("distZip")

dependencies {
    runtimeOnly(project(":extensions:refresh-catalog"))
    runtimeOnly(project(":extensions:policies"))

    runtimeOnly(libs.bundles.connector)
    runtimeOnly(libs.edc.core.controlplane)
    runtimeOnly(libs.edc.ext.api.management)
    runtimeOnly(libs.edc.ext.api.management.config)
    runtimeOnly(libs.edc.ext.configuration.filesystem)
    runtimeOnly(libs.edc.ext.http)

    // DSP protocol
    runtimeOnly(libs.edc.protocol.dsp)

    // API key authentication for Data Management API (also used for CORS support)
    runtimeOnly(libs.edc.ext.auth.tokenbased)

    // DID authentication
    runtimeOnly(libs.bundles.identity)

    // Blob storage container provisioning
    runtimeOnly(libs.edc.azure.core.blob)
    runtimeOnly(libs.edc.azure.ext.provision.blob)
    // To use FileSystem vault e.g. -DuseFsVault="true".Only for non-production usages.
    val useFsVault: Boolean = System.getProperty("useFsVault", "false").toBoolean()
    if (useFsVault) {
        runtimeOnly(libs.edc.ext.vault.filesystem)
    } else {
        runtimeOnly(libs.edc.azure.ext.vault)
    }

    runtimeOnly(libs.bundles.transfer.dpf)

    runtimeOnly(libs.edc.core.dpf.selector)
    runtimeOnly(libs.edc.ext.dpf.selector.api)

    // Embedded DPF
    runtimeOnly(libs.bundles.dpf)

    // Federated catalog
    runtimeOnly(libs.fc.core)
    runtimeOnly(libs.fc.ext.api)

    // Identity Hub
    runtimeOnly(libs.ih.core)
    runtimeOnly(libs.ih.ext.api)
    runtimeOnly(libs.ih.ext.api.selfdescription)
    runtimeOnly(libs.ih.core.verifier)
    runtimeOnly(libs.ih.ext.credentials.jwt)
    runtimeOnly(libs.ih.ext.verifier.jwt)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("connector.jar")
    dependsOn(distTar, distZip)
    mustRunAfter(distTar, distZip)
}
