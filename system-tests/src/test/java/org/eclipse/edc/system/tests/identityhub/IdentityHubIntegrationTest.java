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

package org.eclipse.edc.system.tests.identityhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.nimbusds.jose.shaded.json.JSONObject;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.OkHttpClient;
import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.api.IterableAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.api.ThrowingConsumer;
import org.eclipse.edc.identityhub.client.IdentityHubClientImpl;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.system.tests.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class IdentityHubIntegrationTest {

    static final String COMPANY1_IDENTITY_HUB_URL = TestUtils.requiredPropOrEnv("COMPANY1_IDENTITY_HUB_URL", "http://localhost:7171/api/v1/identity/identity-hub");
    static final String COMPANY2_IDENTITY_HUB_URL = TestUtils.requiredPropOrEnv("COMPANY2_IDENTITY_HUB_URL", "http://localhost:7172/api/v1/identity/identity-hub");
    static final String COMPANY3_IDENTITY_HUB_URL = TestUtils.requiredPropOrEnv("COMPANY3_IDENTITY_HUB_URL", "http://localhost:7173/api/v1/identity/identity-hub");
    static final String AUTHORITY_IDENTITY_HUB_URL = TestUtils.requiredPropOrEnv("AUTHORITY_IDENTITY_HUB_URL", "http://localhost:7174/api/v1/identity/identity-hub");

    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ConsoleMonitor CONSOLE_MONITOR = new ConsoleMonitor();

    private IdentityHubClientImpl client;

    private static JsonNode getOrThrow(JsonNode node, String key) {
        var value = node.get(key);
        assertThat(value).as("Get value for key: %s", key).isNotNull();
        return value;
    }

    private static Stream<Arguments> provideParticipantHubUrls() {
        return Stream.of(
                arguments(COMPANY1_IDENTITY_HUB_URL, "eu", "FR"),
                arguments(COMPANY2_IDENTITY_HUB_URL, "eu", "DE"),
                arguments(COMPANY3_IDENTITY_HUB_URL, "us", "US")
        );
    }

    private static Stream<Arguments> provideAllHubUrls() {
        return Stream.concat(
                provideParticipantHubUrls(),
                Stream.of(
                        arguments(AUTHORITY_IDENTITY_HUB_URL, "eu", "ES")
                )
        );
    }

    @BeforeEach
    void setUp() {
        client = new IdentityHubClientImpl(OK_HTTP_CLIENT, OBJECT_MAPPER, CONSOLE_MONITOR);
    }

    @ParameterizedTest
    @MethodSource("provideParticipantHubUrls")
    void retrieveVerifiableCredentials(String hubUrl, String region, String country) {
        await()
                .atMost(20, SECONDS)
                .pollInterval(2, SECONDS)
                .untilAsserted(() -> twoCredentialsInIdentityHub(hubUrl));

        twoCredentialsInIdentityHub(hubUrl)
                .anySatisfy(vcRequirements("region", region))
                .anySatisfy(vcRequirements("gaiaXMember", "true"));
    }

    @ParameterizedTest
    @MethodSource("provideAllHubUrls")
    void getSelfDescription(String hubUrl, String region, String country) {
        await().atMost(20, SECONDS)
                .pollInterval(2, SECONDS)
                .untilAsserted(() -> selfDescriptionRetrieved(hubUrl));

        selfDescriptionRetrieved(hubUrl).anySatisfy(selfDescriptionRequirements(country));
    }

    private ThrowingConsumer<JsonNode> selfDescriptionRequirements(String country) {
        return json -> {
            var credentialSubject = getOrThrow(json, "credentialSubject");
            var headquarterAddress = getOrThrow(credentialSubject, "gx-participant:headquarterAddress");
            var participantCountry = getOrThrow(headquarterAddress, "gx-participant:country");
            var countryValue = getOrThrow(participantCountry, "@value");
            assertThat(countryValue).isInstanceOf(TextNode.class);
            assertThat(countryValue.asText()).isEqualTo(country);
        };
    }

    private ThrowingConsumer<SignedJWT> vcRequirements(String name, String value) {
        return jwt -> {
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
                                .isInstanceOfSatisfying(JSONObject.class, s -> assertThat(s.get(name))
                                        .as(name)
                                        .isInstanceOfSatisfying(String.class,
                                                r -> assertThat(r).isEqualTo(value)));
                    });
        };
    }

    private AbstractCollectionAssert<?, Collection<? extends SignedJWT>, SignedJWT, ObjectAssert<SignedJWT>> twoCredentialsInIdentityHub(String hubUrl) {
        var vcs = client.getVerifiableCredentials(hubUrl);

        assertThat(vcs.succeeded()).isTrue();
        return assertThat(vcs.getContent()).hasSize(3);
    }

    private IterableAssert<JsonNode> selfDescriptionRetrieved(String hubUrl) {
        var vcs = client.getSelfDescription(hubUrl);

        assertThat(vcs.succeeded()).withFailMessage(ofNullable(vcs.getFailureDetail()).orElse("")).isTrue();
        return assertThat(vcs.getContent());
    }
}
