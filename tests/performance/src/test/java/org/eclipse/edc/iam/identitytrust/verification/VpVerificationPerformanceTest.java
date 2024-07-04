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

import com.apicatalog.jsonld.loader.SchemeRouter;
import com.apicatalog.vc.integrity.DataIntegrityProofOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.VerifiablePresentationContainer;
import org.eclipse.edc.identitytrust.verification.SignatureSuiteRegistry;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.security.signature.jws2020.JwsSignature2020Suite;
import org.eclipse.edc.security.signature.jws2020.TestDocumentLoader;
import org.eclipse.edc.security.signature.jws2020.TestFunctions;
import org.eclipse.edc.verifiablecredentials.jwt.JwtCreationUtils;
import org.eclipse.edc.verifiablecredentials.jwt.JwtPresentationVerifier;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpVerifier;
import org.eclipse.edc.verifiablecredentials.verfiablecredentials.LdpCreationUtils;
import org.eclipse.edc.verifiablecredentials.verfiablecredentials.TestData;
import org.eclipse.edc.verification.jwt.SelfIssuedIdTokenVerifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.Result.success;
import static org.eclipse.edc.verifiablecredentials.TestFunctions.createPublicKeyWrapper;
import static org.eclipse.edc.verifiablecredentials.jwt.TestConstants.CENTRAL_ISSUER_DID;
import static org.eclipse.edc.verifiablecredentials.jwt.TestConstants.CENTRAL_ISSUER_KEY_ID;
import static org.eclipse.edc.verifiablecredentials.jwt.TestConstants.MY_OWN_DID;
import static org.eclipse.edc.verifiablecredentials.jwt.TestConstants.PRESENTER_KEY_ID;
import static org.eclipse.edc.verifiablecredentials.jwt.TestConstants.VP_HOLDER_ID;
import static org.eclipse.edc.verifiablecredentials.verfiablecredentials.TestData.VP_CONTENT_TEMPLATE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Disabled("This should only run on-demand and never during normal test execution")
public class VpVerificationPerformanceTest {

    private static final SignatureSuiteRegistry SIGNATURE_SUITE_REGISTRY = mock();
    private static final ObjectMapper MAPPER = JacksonJsonLd.createObjectMapper();
    private static final JwsSignature2020Suite JWS_SIGNATURE_SUITE = new JwsSignature2020Suite(MAPPER);
    private static final int NUM_TEST_RUNS = 10;
    private static ECKey vpSigningKey;
    private static ECKey vcSigningKey;
    private static TitaniumJsonLd jsonLd;
    private final DidPublicKeyResolver publicKeyResolverMock = mock();
    private MultiFormatPresentationVerifier multiFormatVerifier;
    private LdpVerifier ldpVerifier;

    @BeforeEach
    void setup() {
        when(publicKeyResolverMock.resolvePublicKey(any(), eq(PRESENTER_KEY_ID))).thenReturn(success(createPublicKeyWrapper(vpSigningKey.toPublicJWK())));
        when(publicKeyResolverMock.resolvePublicKey(any(), eq(CENTRAL_ISSUER_KEY_ID))).thenReturn(success(createPublicKeyWrapper(vcSigningKey.toPublicJWK())));

        ldpVerifier = LdpVerifier.Builder.newInstance()
                .signatureSuites(SIGNATURE_SUITE_REGISTRY)
                .jsonLd(jsonLd)
                .objectMapper(MAPPER)
                .build();
    }

    @DisplayName("JWT-VP contains JWT-VCs")
    @ParameterizedTest(name = "Verify JWT-VP with {1} JWT-VCs")
    @ArgumentsSource(JwtVpProvider.class)
    void verify_jwtVp_hasJwtVcs_success(String vpJwt, int numVc) {
        Statistics.runWithStatistics(NUM_TEST_RUNS, () -> {
            multiFormatVerifier = new MultiFormatPresentationVerifier(MY_OWN_DID, new JwtPresentationVerifier(new SelfIssuedIdTokenVerifier(publicKeyResolverMock), MAPPER), ldpVerifier);
            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpJwt, CredentialFormat.JWT, null))).isSucceeded();
        }, numVc);
    }

    @DisplayName("JWT-VP contains LDP-VCs")
    @ParameterizedTest(name = "Verify JWT-VP with {1} LDP-VCs")
    @ArgumentsSource(JwtVpWithLdpVcProvider.class)
    void verify_jwtVp_hasLdpVcs_success(String vpJwt, int numVc) {
        Statistics.runWithStatistics(NUM_TEST_RUNS, () -> {
            multiFormatVerifier = new MultiFormatPresentationVerifier(MY_OWN_DID, new JwtPresentationVerifier(new SelfIssuedIdTokenVerifier(publicKeyResolverMock), MAPPER), ldpVerifier);
            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpJwt, CredentialFormat.JWT, null))).isSucceeded();
        }, numVc);
    }

    @DisplayName("LDP-VP contains LDP-VCs")
    @ParameterizedTest(name = "Verify JWT-VP with {1} LDP-VCs")
    @ArgumentsSource(LdpVpWithLdpVcProvider.class)
    void verify_ldpVp_hasLdpVcs_success(String vpJwt, int numVc) {
        Statistics.runWithStatistics(NUM_TEST_RUNS, () -> {
            multiFormatVerifier = new MultiFormatPresentationVerifier(MY_OWN_DID, new JwtPresentationVerifier(new SelfIssuedIdTokenVerifier(publicKeyResolverMock), MAPPER), ldpVerifier);
            assertThat(multiFormatVerifier.verifyPresentation(new VerifiablePresentationContainer(vpJwt, CredentialFormat.JWT, null))).isSucceeded();
        }, numVc);
    }

    @BeforeAll
    static void prepare() throws URISyntaxException, ParseException {
        when(SIGNATURE_SUITE_REGISTRY.getAllSuites()).thenReturn(Collections.singleton(JWS_SIGNATURE_SUITE));
        jsonLd = new TitaniumJsonLd(mock());
        var contextClassLoader = Thread.currentThread().getContextClassLoader();
        jsonLd.registerCachedDocument("https://www.w3.org/ns/odrl.jsonld", contextClassLoader.getResource("odrl.jsonld").toURI());
        jsonLd.registerCachedDocument("https://www.w3.org/ns/did/v1", contextClassLoader.getResource("jws2020.json").toURI());
        jsonLd.registerCachedDocument("https://w3id.org/security/suites/jws-2020/v1", contextClassLoader.getResource("jws2020.json").toURI());
        jsonLd.registerCachedDocument("https://www.w3.org/2018/credentials/v1", contextClassLoader.getResource("credentials.v1.json").toURI());
        jsonLd.registerCachedDocument("https://www.w3.org/2018/credentials/examples/v1", contextClassLoader.getResource("examples.v1.json").toURI());

        vpSigningKey = (ECKey) JWK.parse(TestUtils.getResourceFileContentAsString("vpSignKey.json"));
        vcSigningKey = (ECKey) JWK.parse(TestUtils.getResourceFileContentAsString("vcSignKey.json"));
    }

    private static class JwtVpProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(createVpWithVcs(50), 50)
//                    Arguments.of(createVpWithVcs(1000), 1000),
//                    Arguments.of(createVpWithVcs(10000), 10000)
            );
        }

        protected String createVc() {
            return JwtCreationUtils.createJwt(vcSigningKey, CENTRAL_ISSUER_DID, "degreeSub", VP_HOLDER_ID, Map.of("vc", TestData.VC_CONTENT_DEGREE_EXAMPLE));
        }

        protected String createVpWithVcs(int howMany) {
            var vcs = IntStream.range(0, howMany)
                    .mapToObj(i -> "\"%s\"".formatted(createVc()))
                    .collect(Collectors.joining(", "));

            return JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "testSub", MY_OWN_DID, Map.of("vp", VP_CONTENT_TEMPLATE.formatted(vcs)));
        }
    }

    private static class JwtVpWithLdpVcProvider extends JwtVpProvider {

        @Override
        protected String createVpWithVcs(int howMany) {
            var vcs = IntStream.range(0, howMany)
                    .mapToObj(i -> "%s".formatted(createVc()))
                    .collect(Collectors.joining(", "));

            return JwtCreationUtils.createJwt(vpSigningKey, VP_HOLDER_ID, "testSub", MY_OWN_DID, Map.of("vp", VP_CONTENT_TEMPLATE.formatted(vcs), "exp", Date.from(Instant.now().plusSeconds(3600L))));
        }

        @Override
        protected String createVc() {
            return TestUtils.getResourceFileContentAsString("verifiable-credential-signed.json");
        }

    }

    private static class LdpVpWithLdpVcProvider extends JwtVpWithLdpVcProvider {
        private final TestDocumentLoader testDocLoader = new TestDocumentLoader("https://org.eclipse.edc/", "", SchemeRouter.defaultInstance());

        @Override
        protected String createVpWithVcs(int howMany) {
            var vcs = IntStream.range(0, howMany)
                    .mapToObj(i -> "%s".formatted(createVc()))
                    .collect(Collectors.joining(", "));

            var vpContent = VP_CONTENT_TEMPLATE.formatted("%s".formatted(vcs));
            return LdpCreationUtils.signDocument(vpContent, vpSigningKey, generateEmbeddedProofOptions(vpSigningKey, VP_HOLDER_ID), testDocLoader);
        }

        private DataIntegrityProofOptions generateEmbeddedProofOptions(ECKey vcKey, String proofPurpose) {
            return JWS_SIGNATURE_SUITE
                    .createOptions()
                    .created(Instant.now())
                    .verificationMethod(TestFunctions.createKeyPair(vcKey, proofPurpose)) // embedded proof
                    .purpose(URI.create("https://w3id.org/security#assertionMethod"));
        }
    }
}
