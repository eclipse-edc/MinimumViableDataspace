/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.demo.tests.transfer;

import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * This test is designed to run against an MVD deployed in a Kubernetes cluster, with an active ingress controller.
 * The cluster MUST be deployed and seeded according to the README before running this test!
 */
@EndToEndTest
public class TransferEndToEndTest {
    // Management API base URL of the consumer connector, goes through Ingress controller
    private static final String CONSUMER_MANAGEMENT_URL = "http://127.0.0.1/consumer/cp";
    // Catalog Query API URL of the consumer connector, goes through ingress controller
    private static final String CONSUMER_CATALOG_URL = "http://127.0.0.1/consumer/fc";
    // DSP service URL of the provider, not reachable outside the cluster
    private static final String PROVIDER_DSP_URL = "http://provider-qna-controlplane:8082";
    // DID of the provider company
    private static final String PROVIDER_ID = "did:web:provider-identityhub%3A7083:provider";
    // public API endpoint of the provider-qna connector, goes through the incress controller
    private static final String PROVIDER_PUBLIC_URL = "http://127.0.0.1/provider-qna/public";
    private static final Duration TEST_TIMEOUT_DURATION = Duration.ofSeconds(30);
    private static final Duration TEST_POLL_DELAY = Duration.ofSeconds(2);

    private static RequestSpecification baseRequest() {
        return given()
                .header("X-Api-Key", "password")
                .contentType(JSON)
                .when();
    }

    @Test
    void transferData() {
        var emptyQueryBody = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("edc", "https://w3id.org/edc/v0.0.1/ns/"))
                .add("@type", "QuerySpec")
                .build();
        var offerId = new AtomicReference<String>();
        // get catalog, extract offer ID
        await().atMost(TEST_TIMEOUT_DURATION)
                .pollDelay(TEST_POLL_DELAY)
                .untilAsserted(() -> {
                    var oid = baseRequest()
                            .body(emptyQueryBody)
                            .post(CONSUMER_CATALOG_URL + "/api/catalog/v1alpha/catalog/query")
                            .then()
                            .log().ifError()
                            .statusCode(200)
                            // yes, it's a bit brittle with the hardcoded indexes, but it appears to work.
                            .extract().body().asString();
                    var jp = new JsonPath(oid).getString("[0]['dcat:dataset'][1]['dcat:dataset'][0]['odrl:hasPolicy']['@id']");

                    assertThat(jp).isNotNull();
                    offerId.set(jp);
                });

        // initiate negotiation
        var negotiationRequest = TestUtils.getResourceFileContentAsString("negotiation-request.json")
                .replace("{{PROVIDER_ID}}", PROVIDER_ID)
                .replace("{{PROVIDER_DSP_URL}}", PROVIDER_DSP_URL)
                .replace("{{OFFER_ID}}", offerId.get());
        var negotiationId = baseRequest()
                .body(negotiationRequest)
                .post(CONSUMER_MANAGEMENT_URL + "/api/management/v3/contractnegotiations")
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().jsonPath().getString("@id");
        assertThat(negotiationId).isNotNull();

        //wait until negotiation is FINALIZED
        var agreementId = new AtomicReference<String>();
        await().atMost(TEST_TIMEOUT_DURATION)
                .pollDelay(TEST_POLL_DELAY)
                .untilAsserted(() -> {
                    var jp = baseRequest()
                            .get(CONSUMER_MANAGEMENT_URL + "/api/management/v3/contractnegotiations/" + negotiationId)
                            .then()
                            .statusCode(200)
                            .extract().body().jsonPath();
                    var state = jp.getString("state");
                    assertThat(state).isEqualTo("FINALIZED");
                    agreementId.set(jp.getString("contractAgreementId"));

                });

        //start transfer process
        var tpRequest = TestUtils.getResourceFileContentAsString("transfer-request.json")
                .replace("{{PROVIDER_ID}}", PROVIDER_ID)
                .replace("{{PROVIDER_DSP_URL}}", PROVIDER_DSP_URL)
                .replace("{{CONTRACT_ID}}", agreementId.get());

        var transferProcessId = baseRequest()
                .body(tpRequest)
                .post(CONSUMER_MANAGEMENT_URL + "/api/management/v3/transferprocesses")
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().jsonPath().getString("@id");

        // fetch EDR for transfer process
        var endpoint = new AtomicReference<String>();
        var token = new AtomicReference<String>();
        await().atMost(TEST_TIMEOUT_DURATION)
                .pollDelay(TEST_POLL_DELAY)
                .untilAsserted(() -> {
                    var jp = baseRequest()
                            .get(CONSUMER_MANAGEMENT_URL + "/api/management/v3/edrs/%s/dataaddress".formatted(transferProcessId))
                            .then()
                            .statusCode(200)
                            .onFailMessage("Expected to find an EDR with transfer ID %s but did not!".formatted(transferProcessId))
                            .extract().body().jsonPath();

                    endpoint.set(jp.getString("endpoint"));
                    token.set(jp.getString("authorization"));

                    assertThat(endpoint.get()).isNotNull().endsWith("/api/public");
                    assertThat(token.get()).isNotNull();
                });

        //download exemplary JSON data from public endpoint
        var response = given()
                .header("Authorization", token.get())
                .get(PROVIDER_PUBLIC_URL + "/api/public")
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().asString();

        assertThat(response).isNotEmpty();
    }
}
