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

val edcVersion: String by project
val edcGroup: String by project
val gatlingVersion: String by project
val jupiterVersion: String by project
val assertj: String by project

dependencies {
    testImplementation("io.gatling.highcharts:gatling-charts-highcharts:${gatlingVersion}") {
        exclude(group = "io.gatling", module="gatling-jms")
        exclude(group = "io.gatling", module="gatling-jms-java")
        exclude(group = "io.gatling", module="gatling-mqtt")
        exclude(group = "io.gatling", module="gatling-mqtt-java")
        exclude(group = "io.gatling", module="gatling-jdbc")
        exclude(group = "io.gatling", module="gatling-jdbc-java")
        exclude(group = "io.gatling", module="gatling-redis")
        exclude(group = "io.gatling", module="gatling-redis-java")
        exclude(group = "io.gatling", module="gatling-graphite")
    }

    testImplementation("org.apache.commons:commons-lang3:3.12.0")
    testImplementation("${edcGroup}:spi:${edcVersion}")
    testImplementation("org.assertj:assertj-core:${assertj}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
}

