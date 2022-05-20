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

package org.eclipse.dataspaceconnector.system.tests.catalog;

import io.restassured.common.mapper.TypeRef;
import org.eclipse.dataspaceconnector.catalog.spi.model.FederatedCatalogCacheQuery;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.system.tests.utils.TestUtils.requiredPropOrEnv;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.PROVIDER_ASSET_ID;

class CatalogClientTest {
    static final String CONSUMER_CATALOG_URL = requiredPropOrEnv("CONSUMER_CATALOG_URL", "http://localhost:8182/api/federatedcatalog");

    @Test
    void containsAsset() {
        await().atMost(10, MINUTES).untilAsserted(() -> verifyAsset());
    }

    private void verifyAsset() {
        var nodes = given()
                .contentType("application/json")
                .body(FederatedCatalogCacheQuery.Builder.newInstance().build())
                .when()
                .post(CONSUMER_CATALOG_URL)
                .then()
                .statusCode(200)
                .extract().body().as(new TypeRef<List<ContractOffer>>() {
                });
        assertThat(nodes).anySatisfy(
                n -> assertThat(n.getAsset().getProperty(Asset.PROPERTY_ID)).isEqualTo(PROVIDER_ASSET_ID));
    }
}
