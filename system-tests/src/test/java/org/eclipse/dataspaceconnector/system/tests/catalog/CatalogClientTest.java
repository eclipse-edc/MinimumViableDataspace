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

import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.dataspaceconnector.catalog.spi.model.FederatedCatalogCacheQuery;
import org.eclipse.dataspaceconnector.policy.model.PolicyRegistrationTypes;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.system.tests.utils.TestUtils.requiredPropOrEnv;

class CatalogClientTest {
    static final String CONSUMER_EU_CATALOG_URL = requiredPropOrEnv("CONSUMER_EU_CATALOG_URL", "http://localhost:8182/api/federatedcatalog");
    static final String CONSUMER_US_CATALOG_URL = requiredPropOrEnv("CONSUMER_US_CATALOG_URL", "http://localhost:8183/api/federatedcatalog");
    static final String NON_RESTRICTED_ASSET_PREFIX = "test-document_";
    static final String RESTRICTED_ASSET_PREFIX = "test-document-2_";

    static TypeManager typeManager = new TypeManager();

    @BeforeAll
    static void setUp() {
        PolicyRegistrationTypes.TYPES.forEach(typeManager::registerTypes);
    }

    @Test
    void containsOnlyNonRestrictedAsset() {
        await().atMost(2, MINUTES).untilAsserted(() -> {
            var nodes = getNodesFromCatalog(CONSUMER_US_CATALOG_URL);
            assertThat(nodes)
                    .isNotEmpty()
                    .allSatisfy(
                        n -> assertThat(n.getAsset().getProperty(Asset.PROPERTY_ID)).asString().startsWith(NON_RESTRICTED_ASSET_PREFIX));
        });
    }

    @Test
    void containsAllAssets() {
        await().atMost(2, MINUTES).untilAsserted(() -> {
            var nodes = getNodesFromCatalog(CONSUMER_EU_CATALOG_URL);
            assertThat(nodes)
                    .isNotEmpty()
                    .allSatisfy(
                        n -> assertThat(n.getAsset().getId()).asString()
                                .satisfiesAnyOf(
                                        s -> assertThat(s).startsWith(NON_RESTRICTED_ASSET_PREFIX),
                                        s -> assertThat(s).startsWith(RESTRICTED_ASSET_PREFIX)));
        });
    }

    private List<ContractOffer> getNodesFromCatalog(String consumerCatalogUrl) {
        var nodesJson = given()
                .contentType("application/json")
                .body(FederatedCatalogCacheQuery.Builder.newInstance().build())
                .when()
                .post(consumerCatalogUrl)
                .then()
                .statusCode(200)
                .extract().body().asString();
        return typeManager.readValue(nodesJson, new TypeReference<>() {
        });
    }
}
