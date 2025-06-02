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

package org.eclipse.edc.demo.tests.issuance;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.demo.tests.TestConstants.TEST_POLL_DELAY;
import static org.eclipse.edc.demo.tests.TestConstants.TEST_TIMEOUT_DURATION;

/**
 * This test asserts the DCP Issuance Flow end-to-end where the holder makes a DCP credential request and waits for the credential
 * to be issued by the IssuerService.
 */
@EndToEndTest
public class CredentialIssuanceEndToEndTest {

    private static final String CONSUMER_IDENTITYHUB_IDENTITY_URL = "http://127.0.0.1/consumer/cs/";
    private static final String PARTICIPANT_CONTEXT_ID = "did:web:consumer-identityhub%3A7083:consumer";
    private static final String ISSUER_DID = "did:web:dataspace-issuer-service%3A10016:issuer";
    private static final String HOLDER_PID = UUID.randomUUID().toString();

    private static RequestSpecification baseRequest() {
        return given()
                .header("X-Api-Key", "c3VwZXItdXNlcg==.c3VwZXItc2VjcmV0LWtleQo=")
                .contentType(JSON)
                .baseUri(CONSUMER_IDENTITYHUB_IDENTITY_URL)
                .when();
    }

    @Test
    void makeCredentialRequest_expectCredential() {
        var requestId = baseRequest()
                .body("""
                        {
                          "issuerDid": "%s",
                          "holderPid": "%s",
                          "credentials": [
                            {"format": "VC1_0_JWT", "type": "FoobarCredential", "id": "demo-credential-def-2"}
                          ]
                        }
                        """.formatted(ISSUER_DID, HOLDER_PID))
                .post("/api/identity/v1alpha/participants/%s/credentials/request".formatted(base64(PARTICIPANT_CONTEXT_ID)))
                .then()
                .log().ifValidationFails()
                .statusCode(201)
                .extract().body().asString();

        assertThat(requestId).isEqualTo(HOLDER_PID);

        // wait for the state of the request to become ISSUED
        await().atMost(TEST_TIMEOUT_DURATION)
                .pollDelay(TEST_POLL_DELAY)
                .untilAsserted(() -> {
                    baseRequest()
                            .get("/api/identity/v1alpha/participants/%s/credentials/request/%s".formatted(base64(PARTICIPANT_CONTEXT_ID), requestId))
                            .then()
                            .log().ifValidationFails()
                            .statusCode(200)
                            .body("status", Matchers.equalTo("ISSUED"));
                });

        // check that the holder now has credentials in storage
        List<List<String>> list = baseRequest()
                .get("/api/identity/v1alpha/credentials")
                .jsonPath()
                .getList("verifiableCredential.credential.type");

        assertThat(list).anySatisfy(typesList -> assertThat(typesList).containsExactlyInAnyOrder("FoobarCredential", "VerifiableCredential"));


    }

    private String base64(String input) {
        return Base64.getUrlEncoder().encodeToString(input.getBytes());
    }
}
