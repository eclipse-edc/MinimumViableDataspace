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

package org.eclipse.edc.system.tests.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.http.ContentType;
import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.model.FederatedCatalogCacheQuery;
import org.eclipse.edc.core.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.core.transform.transformer.from.JsonObjectFromCatalogTransformer;
import org.eclipse.edc.core.transform.transformer.from.JsonObjectFromDataServiceTransformer;
import org.eclipse.edc.core.transform.transformer.from.JsonObjectFromDatasetTransformer;
import org.eclipse.edc.core.transform.transformer.from.JsonObjectFromDistributionTransformer;
import org.eclipse.edc.core.transform.transformer.from.JsonObjectFromPolicyTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToActionTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToCatalogTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToConstraintTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToDataServiceTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToDatasetTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToDistributionTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToOperatorTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToPermissionTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToPolicyTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.system.tests.utils.TestUtils.requiredPropOrEnv;
import static org.mockito.Mockito.mock;

@EndToEndTest
class CatalogClientTest {

    private static final String CONSUMER_EU_CATALOG_URL = requiredPropOrEnv("CONSUMER_EU_CATALOG_URL", "http://localhost:9192/api/management/federatedcatalog");
    private static final String CONSUMER_US_CATALOG_URL = requiredPropOrEnv("CONSUMER_US_CATALOG_URL", "http://localhost:9193/api/management/federatedcatalog");
    private static final String API_MANAGEMENT_AUTH_HEADER_KEY = "X-Api-Key";
    private static final String API_MANAGEMENT_AUTH_HEADER_CODE = "ApiKeyDefaultValue";
    private static final String NON_RESTRICTED_ASSET_PREFIX = "test-document_";
    private static final String RESTRICTED_ASSET_PREFIX = "test-document-2_";
    private static final Duration TEST_POLL_INTERVAL = Duration.ofMillis(250);
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(20);

    private final TypeTransformerRegistry typeTransformerRegistry = new TypeTransformerRegistryImpl();
    private final ObjectMapper mapper = JacksonJsonLd.createObjectMapper();
    private final JsonLd jsonLd = new TitaniumJsonLd(mock(Monitor.class));

    @BeforeEach
    public void setUp() {
        //needed for ZonedDateTime
        mapper.registerModule(new JavaTimeModule());
        JsonBuilderFactory factory = Json.createBuilderFactory(Map.of());
        typeTransformerRegistry.register(new JsonObjectFromCatalogTransformer(factory, mapper));
        typeTransformerRegistry.register(new JsonObjectFromDatasetTransformer(factory, mapper));
        typeTransformerRegistry.register(new JsonObjectFromDataServiceTransformer(factory));
        typeTransformerRegistry.register(new JsonObjectFromPolicyTransformer(factory));
        typeTransformerRegistry.register(new JsonObjectFromDistributionTransformer(factory));
        typeTransformerRegistry.register(new JsonObjectToCatalogTransformer());
        typeTransformerRegistry.register(new JsonObjectToDatasetTransformer());
        typeTransformerRegistry.register(new JsonObjectToDataServiceTransformer());
        typeTransformerRegistry.register(new JsonObjectToPolicyTransformer());
        typeTransformerRegistry.register(new JsonObjectToPermissionTransformer());
        typeTransformerRegistry.register(new JsonObjectToConstraintTransformer());
        typeTransformerRegistry.register(new JsonObjectToOperatorTransformer());
        typeTransformerRegistry.register(new JsonObjectToActionTransformer());
        typeTransformerRegistry.register(new JsonObjectToDistributionTransformer());
        typeTransformerRegistry.register(new JsonValueToGenericTypeTransformer(mapper));
    }

    @Test
    void containsOnlyNonRestrictedAsset() {
        await().atMost(TEST_TIMEOUT)
                .pollInterval(TEST_POLL_INTERVAL)
                .untilAsserted(() -> {
                    var datasets = getFederatedCatalog(CONSUMER_US_CATALOG_URL).stream()
                            .flatMap(catalog -> catalog.getDatasets().stream())
                            .toList();

                    assertThat(datasets)
                            .isNotEmpty()
                            .allSatisfy(dataset -> assertThat(dataset.getProperty(Asset.PROPERTY_ID))
                                    .isNotNull()
                                    .asString()
                                    .startsWith(NON_RESTRICTED_ASSET_PREFIX));
                });
    }

    @Test
    void containsAllAssets() {
        await().atMost(TEST_TIMEOUT)
                .pollInterval(TEST_POLL_INTERVAL)
                .untilAsserted(() -> {
                    var datasets = getFederatedCatalog(CONSUMER_EU_CATALOG_URL).stream()
                            .flatMap(catalog -> catalog.getDatasets().stream())
                            .toList();
                    assertThat(datasets)
                            .isNotEmpty()
                            .allSatisfy(
                                    dataset -> assertThat(dataset.getProperty(Asset.PROPERTY_ID)).asString()
                                            .satisfiesAnyOf(
                                                    s -> assertThat(s).startsWith(NON_RESTRICTED_ASSET_PREFIX),
                                                    s -> assertThat(s).startsWith(RESTRICTED_ASSET_PREFIX)));
                });
    }

    private List<Catalog> getFederatedCatalog(String consumerCatalogUrl) {
        var typeReference = new TypeReference<List<Map<String, Object>>>() {
        };

        var nodesJson = given()
                .contentType(ContentType.JSON)
                .header(API_MANAGEMENT_AUTH_HEADER_KEY, API_MANAGEMENT_AUTH_HEADER_CODE)
                .body(FederatedCatalogCacheQuery.Builder.newInstance().build())
                .when()
                .post(consumerCatalogUrl)
                .then()
                .statusCode(200)
                .extract().body().asString();

        try {
            var list = mapper.readValue(nodesJson, typeReference);
            return list.stream().map(m -> {
                var jsonObj = jsonLd.expand(Json.createObjectBuilder(m).build()).getContent();
                return typeTransformerRegistry.transform(jsonObj, Catalog.class).getContent();
            }).toList();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
