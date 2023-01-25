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

import io.restassured.RestAssured;
import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.api.ThrowingConsumer;
import org.eclipse.edc.identityhub.client.IdentityHubClientImpl;
import org.eclipse.edc.identityhub.credentials.jwt.JwtCredentialEnvelopeTransformer;
import org.eclipse.edc.identityhub.spi.credentials.model.CredentialEnvelope;
import org.eclipse.edc.identityhub.spi.credentials.transformer.CredentialEnvelopeTransformerRegistryImpl;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.system.tests.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.junit.testfixtures.TestUtils.testHttpClient;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ComponentTest
class IdentityHubIntegrationTest {

    private static final String COMPANY1_IDENTITY_HUB_URL = TestUtils.requiredPropOrEnv("COMPANY1_IDENTITY_HUB_URL", "http://localhost:7171/api/v1/identity/identity-hub");
    private static final String COMPANY2_IDENTITY_HUB_URL = TestUtils.requiredPropOrEnv("COMPANY2_IDENTITY_HUB_URL", "http://localhost:7172/api/v1/identity/identity-hub");
    private static final String COMPANY3_IDENTITY_HUB_URL = TestUtils.requiredPropOrEnv("COMPANY3_IDENTITY_HUB_URL", "http://localhost:7173/api/v1/identity/identity-hub");
    private static final String AUTHORITY_IDENTITY_HUB_URL = TestUtils.requiredPropOrEnv("AUTHORITY_IDENTITY_HUB_URL", "http://localhost:7174/api/v1/identity/identity-hub");

    private static final EdcHttpClient HTTP_CLIENT = testHttpClient();
    private static final TypeManager TYPE_MANAGER = new TypeManager();

    private IdentityHubClientImpl client;

    @BeforeEach
    void setUp() {
        var transformerRegistry = new CredentialEnvelopeTransformerRegistryImpl();
        transformerRegistry.register(new JwtCredentialEnvelopeTransformer(TYPE_MANAGER.getMapper()));
        client = new IdentityHubClientImpl(HTTP_CLIENT, TYPE_MANAGER, transformerRegistry);
    }

    @ParameterizedTest
    @ArgumentsSource(ParticipantsIdentityHubsUrlProvider.class)
    void retrieveVerifiableCredentials(String hubUrl, String region, String country) {
        await().atMost(20, SECONDS)
                .pollInterval(2, SECONDS)
                .untilAsserted(() -> twoCredentialsInIdentityHub(hubUrl));

        twoCredentialsInIdentityHub(hubUrl).anySatisfy(vcRequirements("region", region));
    }

    @ParameterizedTest
    @ArgumentsSource(DataspaceIdentityHubsUrlProvider.class)
    void getSelfDescription(String hubUrl, String region, String country) throws IOException {
        RestAssured.given()
                .baseUri(hubUrl)
                .get("/self-description")
                .then()
                .assertThat()
                .statusCode(200)
                .body("selfDescriptionCredential.credentialSubject.gx-participant:headquarterAddress.gx-participant:country.@value", equalTo(country));
    }

    private static final class ParticipantsIdentityHubsUrlProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    arguments(COMPANY1_IDENTITY_HUB_URL, "eu", "FR"),
                    arguments(COMPANY2_IDENTITY_HUB_URL, "eu", "DE"),
                    arguments(COMPANY3_IDENTITY_HUB_URL, "us", "US")
            );
        }
    }

    private static final class DataspaceIdentityHubsUrlProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
            return Stream.concat(
                    new ParticipantsIdentityHubsUrlProvider().provideArguments(extensionContext),
                    Stream.of(
                            arguments(AUTHORITY_IDENTITY_HUB_URL, "eu", "ES")
                    )
            );
        }
    }

    private ThrowingConsumer<CredentialEnvelope> vcRequirements(String name, String value) {
        return envelope -> {
            var vcResult = envelope.toVerifiableCredential(TYPE_MANAGER.getMapper());
            assertThat(vcResult.succeeded()).isTrue();
            var credential = vcResult.getContent().getItem();
            assertThat(credential.getIssuer()).as("Issuer is a Web DID").startsWith("did:web:");
            assertThat(credential.getCredentialSubject().getId()).as("Subject is a Web DID").startsWith("did:web:");
            assertThat(credential.getCredentialSubject().getClaims())
                    .extractingByKey(name)
                    .satisfies(o -> {
                        assertThat(o).isInstanceOf(String.class);
                        assertThat((String) o).isEqualTo(value);
                    });
        };
    }

    private AbstractCollectionAssert<?, Collection<? extends CredentialEnvelope>, CredentialEnvelope, ObjectAssert<CredentialEnvelope>> twoCredentialsInIdentityHub(String hubUrl) {
        var vcs = client.getVerifiableCredentials(hubUrl);
        assertThat(vcs.succeeded()).isTrue();
        return assertThat(vcs.getContent()).hasSize(2);
    }
}
