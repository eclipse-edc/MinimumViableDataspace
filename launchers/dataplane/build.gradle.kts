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
    runtimeOnly(libs.bundles.connector)
    runtimeOnly(libs.edc.api.observability)
    runtimeOnly(libs.edc.dataplane.core)
    runtimeOnly(libs.edc.dataplane.api.control.config)
    runtimeOnly(libs.edc.dataplane.api.control.client)
    runtimeOnly(libs.edc.dataplane.selfregistration)
    runtimeOnly(libs.edc.dataplane.http)
    runtimeOnly(libs.edc.dataplane.http.oauth2)
    runtimeOnly(libs.edc.dataplane.api.public)
    runtimeOnly(libs.edc.dataplane.api.signaling)
    runtimeOnly(libs.edc.dataplane.iam)
    runtimeOnly(libs.edc.ext.jsonld) // needed by the DataPlaneSignalingApi
    runtimeOnly(libs.edc.dpf.selector.client) // for the selector service -> self registration

    if (project.properties.getOrDefault("persistence", "false") == "true") {
        runtimeOnly(libs.edc.vault.hashicorp)
        runtimeOnly(libs.bundles.sql.edc.dataplane)
        runtimeOnly(libs.edc.sts.remote.client)
        println("This runtime compiles with a remote STS client, Hashicorp Vault and PostgreSQL. You will need properly configured Postgres and HCV instances.")
    }

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