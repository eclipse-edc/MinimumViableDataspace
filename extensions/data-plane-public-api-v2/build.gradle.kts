/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

plugins {
    `java-library`
    id(libs.plugins.swagger.get().pluginId)
}

dependencies {
    api(libs.edc.spi.http)
    api(libs.edc.spi.web)
    api(libs.edc.spi.dataplane)
    implementation(libs.edc.lib.util)
    implementation(libs.edc.lib.util.dataplane)
    implementation(libs.jakarta.rsApi)

    testImplementation(libs.edc.lib.http)
    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)
    testImplementation(testFixtures(libs.edc.core.jersey))

}
edcBuild {
    swagger {
        apiGroup.set("public-api")
    }
}


