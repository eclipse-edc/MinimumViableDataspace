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
    implementation(libs.edc.aws.s3.core)
    implementation(libs.edc.aws.data.plane.s3)

    runtimeOnly(libs.edc.bom.dataplane)
    runtimeOnly(libs.edc.dataplane.v2)


    
    // S3 support for data plane
    // Note: This may need to be adjusted based on actual EDC version and available extensions
    // If data-plane-s3 extension is not available, HTTP can be used as fallback
    runtimeOnly(libs.edc.aws.validator.data.address.s3)
    
    runtimeOnly(project(":extensions:otel-monitor"))
    if (project.properties.getOrDefault("persistence", "false") == "true") {
        runtimeOnly(libs.edc.vault.hashicorp)
        runtimeOnly(libs.edc.bom.dataplane.sql)
        println("This runtime compiles with a remote STS client, Hashicorp Vault and PostgreSQL. You will need properly configured Postgres and HCV instances.")
    }
}

tasks.shadowJar {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

edcBuild {
    publish.set(false)
}
