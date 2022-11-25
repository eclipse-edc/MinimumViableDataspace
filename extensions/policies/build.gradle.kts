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
 *       Microsoft Corporation - initial implementation
 *
 */

plugins {
    `java-library`
}

val edcVersion: String by project
val edcGroup: String by project
val jupiterVersion: String by project
val assertj: String by project
val identityHubGroup: String by project
val identityHubVersion: String by project

dependencies {
    api("${edcGroup}:ids-spi:${edcVersion}")
    api("${edcGroup}:contract-spi:${edcVersion}")
    api("${edcGroup}:connector-core:${edcVersion}")
    implementation("${identityHubGroup}:identity-hub-spi:${identityHubVersion}")

    testImplementation("${edcGroup}:policy-engine:${edcVersion}")
    testImplementation("org.assertj:assertj-core:${assertj}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
}
