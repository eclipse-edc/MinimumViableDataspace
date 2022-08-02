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

package org.eclipse.dataspaceconnector.system.tests.identityhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.json.JSONObject;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.OkHttpClient;
import org.assertj.core.api.ObjectAssert;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClientImpl;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.system.tests.utils.TestUtils.requiredPropOrEnv;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class IdentityHubIntegrationTest {

    static final String PROVIDER_IDENTITY_HUB_URL = requiredPropOrEnv("PROVIDER_IDENTITY_HUB_URL", "http://localhost:8181/api/identity-hub");
    static final String CONSUMER_EU_IDENTITY_HUB_URL = requiredPropOrEnv("CONSUMER_EU_IDENTITY_HUB_URL", "http://localhost:8182/api/identity-hub");
    static final String CONSUMER_US_IDENTITY_HUB_URL = requiredPropOrEnv("CONSUMER_US_IDENTITY_HUB_URL", "http://localhost:8183/api/identity-hub");

    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ConsoleMonitor CONSOLE_MONITOR = new ConsoleMonitor();

    private IdentityHubClientImpl client;

    @BeforeEach
    void setUp() {
        client = new IdentityHubClientImpl(OK_HTTP_CLIENT, OBJECT_MAPPER, CONSOLE_MONITOR);
    }

    @ParameterizedTest
    @MethodSource("provideHubUrls")
    void retrieveVerifiableCredentials(String hubUrl, String region) {
        await().atMost(20, SECONDS).untilAsserted(() -> singleVcInIdentityHub(hubUrl));

        singleVcInIdentityHub(hubUrl)
                .satisfies(jwt -> {
                    var claims = jwt.getJWTClaimsSet();
                    assertThat(claims.getIssuer()).as("Issuer is a Web DID").startsWith("did:web:");
                    assertThat(claims.getSubject()).as("Subject is a Web DID").startsWith("did:web:");
                    assertThat(claims.getClaim("vc")).as("VC")
                            .isInstanceOfSatisfying(JSONObject.class, t -> {

                                assertThat(t.get("id"))
                                        .as("VC ID")
                                        .isInstanceOfSatisfying(String.class, s -> assertThat(s).isNotBlank());

                                assertThat(t.get("credentialSubject"))
                                        .as("VC credentialSubject")
                                        .isInstanceOfSatisfying(JSONObject.class,
                                                s -> assertThat(s.get("region"))
                                                        .as("region")
                                                        .isInstanceOfSatisfying(String.class,
                                                                r -> assertThat(r).isEqualTo(region)));

                            })
                    ;
                });
    }

    private ObjectAssert<SignedJWT> singleVcInIdentityHub(String hubUrl) {
        var vcs = client.getVerifiableCredentials(hubUrl);

        assertThat(vcs.succeeded()).isTrue();
        return assertThat(vcs.getContent()).singleElement();
    }

    private static Stream<Arguments> provideHubUrls() {
        return Stream.of(
                arguments(PROVIDER_IDENTITY_HUB_URL, "eu"),
                arguments(CONSUMER_EU_IDENTITY_HUB_URL, "eu"),
                arguments(CONSUMER_US_IDENTITY_HUB_URL, "us")
        );
    }
}
