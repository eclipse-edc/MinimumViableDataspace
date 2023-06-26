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
    alias(libs.plugins.shadow)
}

var distTar = tasks.getByName("distTar")
var distZip = tasks.getByName("distZip")

dependencies {
    runtimeOnly(libs.bundles.connector)
    runtimeOnly(libs.bundles.identity)
    runtimeOnly(libs.edc.core.micrometer)
    runtimeOnly(libs.edc.ext.micrometer.jetty)
    runtimeOnly(libs.edc.ext.micrometer.jersey)
    runtimeOnly(libs.edc.ext.configuration.filesystem)

    runtimeOnly(libs.rs.core)
    runtimeOnly(libs.rs.core.credential.service)
    runtimeOnly(libs.rs.ext.api)

    runtimeOnly(libs.ih.ext.api)
    runtimeOnly(libs.ih.ext.api.selfdescription)
    runtimeOnly(libs.ih.core.verifier)
    runtimeOnly(libs.ih.ext.credentials.jwt)
    runtimeOnly(libs.ih.ext.verifier.jwt)

    // To use FileSystem vault e.g. -DuseFsVault="true".Only for non-production usages.
    val useFsVault: Boolean = System.getProperty("useFsVault", "false").toBoolean()
    if (useFsVault) {
        runtimeOnly(libs.edc.ext.vault.filesystem)
    } else {
        runtimeOnly(libs.edc.azure.ext.vault)
    }
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("registrationservice.jar")
    dependsOn(distTar, distZip)
    mustRunAfter(distTar, distZip)
}
