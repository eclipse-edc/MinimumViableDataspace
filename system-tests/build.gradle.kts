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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

plugins {
    `java-library`
}

dependencies {
    testImplementation(libs.gatling.highcharts) {
        exclude(group = "io.gatling", module = "gatling-jms")
        exclude(group = "io.gatling", module = "gatling-jms-java")
        exclude(group = "io.gatling", module = "gatling-mqtt")
        exclude(group = "io.gatling", module = "gatling-mqtt-java")
        exclude(group = "io.gatling", module = "gatling-jdbc")
        exclude(group = "io.gatling", module = "gatling-jdbc-java")
        exclude(group = "io.gatling", module = "gatling-redis")
        exclude(group = "io.gatling", module = "gatling-redis-java")
        exclude(group = "io.gatling", module = "gatling-graphite")
    }

    testImplementation(libs.apache.commons.lang3)

    testImplementation(libs.edc.azure.core.blob)
    testImplementation(libs.edc.core.util)
    testImplementation(libs.edc.core.transform.core)
    testImplementation(libs.edc.azure.ext.dpf.storage)
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
    testImplementation(libs.okhttp)

    testImplementation(libs.azure.identity)
    testImplementation(libs.azure.keyvault)
    testImplementation(libs.azure.storageblob)
    testImplementation(libs.edc.azure.ext.vault)
    testImplementation(libs.edc.spi.contract)
    testImplementation(libs.fc.spi.core)

    testImplementation(libs.edc.spi.jsonld)
    testImplementation(libs.edc.ext.jsonld)

    // Identity Hub
    testImplementation(libs.ih.core.client)
    testImplementation(libs.ih.ext.credentials.jwt)
    testImplementation(libs.edc.core.junit)
}

