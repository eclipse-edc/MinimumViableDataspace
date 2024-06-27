/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import org.eclipse.edc.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.edc.identityhub.core.creators.JwtPresentationGenerator;
import org.eclipse.edc.identityhub.core.creators.LdpPresentationGenerator;
import org.eclipse.edc.identityhub.spi.model.IdentityHubConstants;
import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.identitytrust.verification.SignatureSuiteRegistry;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.security.signature.jws2020.JwsSignature2020Suite;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpIssuer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.eclipse.edc.iam.identitytrust.verification.Statistics.runWithStatistics;
import static org.eclipse.edc.iam.identitytrust.verification.TestFunctions.createSignedCredential;
import static org.eclipse.edc.identitytrust.VcConstants.DID_CONTEXT_URL;
import static org.eclipse.edc.identitytrust.VcConstants.DCP_CONTEXT_URL;
import static org.eclipse.edc.identitytrust.VcConstants.JWS_2020_URL;
import static org.eclipse.edc.identitytrust.VcConstants.PRESENTATION_EXCHANGE_URL;
import static org.eclipse.edc.identitytrust.VcConstants.W3C_CREDENTIALS_URL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The purpose of this test is to be executed using a profiler to inspect the performance differences between
 * LDP and JWT
 */

@Disabled("This should only run on-demand and never during normal test execution")
public class VpCreationPerformanceTest {
    public static final String KEY_ID = "https://test.com/test-keys#key-1";
    private static final ObjectMapper MAPPER = JacksonJsonLd.createObjectMapper();
    private static final JwsSignature2020Suite JWS_SIGNATURE_SUITE = new JwsSignature2020Suite(MAPPER);
    private static final SignatureSuiteRegistry SIGNATURE_SUITE_REGISTRY = mock();
    private static final int NUM_TEST_RUNS = 10;
    private static OctetKeyPair vpSigningKey;
    private static TitaniumJsonLd jsonLd;
    private final Map<String, Object> types = Map.of("types", List.of("VerifiablePresentation", "SomeOtherPresentationType"));
    private final PrivateKeyResolver keyResolverMock = mock();

    @BeforeEach
    void setup() {
        when(keyResolverMock.resolvePrivateKey(eq(KEY_ID), any())).thenReturn(new OctetKeyPairWrapper(vpSigningKey));
    }

    @DisplayName("Create LDP-VP for several VCs")
    @ParameterizedTest
    @ArgumentsSource(CredentialProvider.class)
    void createVp_linkedData(List<VerifiableCredentialContainer> credentials) {

        var issuer = LdpIssuer.Builder.newInstance()
                .jsonLd(jsonLd)
                .monitor(mock())
                .build();
        var creator = new LdpPresentationGenerator(keyResolverMock, "did:web:my-own", SIGNATURE_SUITE_REGISTRY, IdentityHubConstants.JWS_2020_SIGNATURE_SUITE, issuer, MAPPER);

        runWithStatistics(NUM_TEST_RUNS, () -> creator.generatePresentation(credentials, KEY_ID, types), credentials.size());
    }

    @DisplayName("Create JWT-VP for several VCs")
    @ParameterizedTest
    @ArgumentsSource(CredentialProvider.class)
    void createVp_jwt(List<VerifiableCredentialContainer> credentials) {
        var creator = new JwtPresentationGenerator(keyResolverMock, Clock.systemUTC(), "did:web:my-own");
        runWithStatistics(NUM_TEST_RUNS, () -> creator.generatePresentation(credentials, KEY_ID, Map.of("aud", "did:web:me:myself:and_I")), credentials.size());
    }

    private static OctetKeyPair createKey(String keyId) {
        try {
            return new OctetKeyPairGenerator(Curve.Ed25519)
                    .keyID(keyId)
                    .generate();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeAll
    static void prepare() {
        when(SIGNATURE_SUITE_REGISTRY.getAllSuites()).thenReturn(Collections.singleton(JWS_SIGNATURE_SUITE));
        when(SIGNATURE_SUITE_REGISTRY.getForId(any())).thenReturn(JWS_SIGNATURE_SUITE);
        jsonLd = new TitaniumJsonLd(mock());
        jsonLd.registerCachedDocument("https://www.w3.org/ns/odrl.jsonld", TestUtils.getResource("odrl.jsonld"));
        jsonLd.registerCachedDocument(DID_CONTEXT_URL, TestUtils.getResource("did.json"));
        jsonLd.registerCachedDocument(JWS_2020_URL, TestUtils.getResource("jws2020.json"));
        jsonLd.registerCachedDocument(W3C_CREDENTIALS_URL, TestUtils.getResource("credentials.v1.json"));
        jsonLd.registerCachedDocument(DCP_CONTEXT_URL, TestUtils.getResource("presentation-query.v08.json"));
        jsonLd.registerCachedDocument(PRESENTATION_EXCHANGE_URL, TestUtils.getResource("presentation-exchange.v1.json"));
        jsonLd.registerCachedDocument("https://www.w3.org/2018/credentials/examples/v1", TestUtils.getResource("examples.v1.json"));

        vpSigningKey = createKey(KEY_ID);
    }

    private static class CredentialProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(Named.named("VCs: 1", IntStream.range(0, 1).mapToObj(i -> createSignedCredential()).toList())),
                    Arguments.of(Named.named("VCs: 1000", IntStream.range(0, 1000).mapToObj(i -> createSignedCredential()).toList())),
                    Arguments.of(Named.named("VCs: 10000", IntStream.range(0, 10000).mapToObj(i -> createSignedCredential()).toList()))
            );
        }

    }

    private record OctetKeyPairWrapper(OctetKeyPair privateKey) implements PrivateKeyWrapper {

        @Override
        public JWEDecrypter decrypter() {
            return null; // not needed here
        }

        @Override
        public JWSSigner signer() {
            try {
                return new Ed25519Signer(privateKey);
            } catch (JOSEException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
