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
val group = "org.eclipse.dataspaceconnector"

dependencies {
    implementation("${group}:core:${edcVersion}")
    implementation("${group}:ids:${edcVersion}")
    implementation("${group}:control-api:${edcVersion}")
    implementation("${group}:observability-api:${edcVersion}")
    implementation("${group}:data-management-api:${edcVersion}")
    implementation("${group}:assetindex-memory:${edcVersion}")
    implementation("${group}:transfer-process-store-memory:${edcVersion}")
    implementation("${group}:contractnegotiation-store-memory:${edcVersion}")
    implementation("${group}:contractdefinition-store-memory:${edcVersion}")
    implementation("${group}:iam-mock:${edcVersion}")
    implementation("${group}:filesystem-configuration:${edcVersion}")
    implementation("${group}:http:${edcVersion}")
    implementation("${group}:policy-store-memory:${edcVersion}")
}

application {
    mainClass.set("org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xml")
    mergeServiceFiles()
    archiveFileName.set("dataspaceconnector-basic.jar")
}
