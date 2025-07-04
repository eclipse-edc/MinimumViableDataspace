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
}

dependencies {
    implementation(libs.edc.spi.identity.did)
    implementation(libs.edc.fc.spi.crawler)
    runtimeOnly(libs.edc.fc.core)
    // todo: use 2025 once it is used everywhere
    // runtimeOnly(libs.edc.fc.core2025)
    runtimeOnly(libs.edc.fc.core08)
    runtimeOnly(libs.edc.fc.api)
}
