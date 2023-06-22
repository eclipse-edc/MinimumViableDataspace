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

package org.eclipse.edc.mvd;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.registration.client.model.ParticipantDto;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
import java.util.stream.Stream;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FederatedCacheNodeResolverTest {

    private static final String SUPPORTED_PROTOCOL = "dataspace-protocol-http";
    private static final String DSP_MESSAGING = "DSPMessaging";
    private static final String DID = "did:web:" + "test-domainname";
    private static final String DSP_URL = "test.dsp.url";
    
    private final DidResolverRegistry didResolver = mock(DidResolverRegistry.class);
    private final Monitor monitor = mock(Monitor.class);
    private final FederatedCacheNodeResolver resolver = new FederatedCacheNodeResolver(didResolver, monitor);

    @NotNull
    private static DidDocument createDidDocument(List<Service> services) {
        return DidDocument.Builder.newInstance()
                .id(DID)
                .service(services)
                .build();
    }

    @NotNull
    private static Service fakeService() {
        return service("test-url", "test-type");
    }

    @NotNull
    private static Service dspMessagingService(String url) {
        return service(url, DSP_MESSAGING);
    }

    @NotNull
    private static Service service(String url, String type) {
        return new Service("test-url", type, url);
    }

    @ParameterizedTest
    @ArgumentsSource(StreamSuccessProvider.class)
    void getNode_success(List<Service> services) {
        when(didResolver.resolve(DID)).thenReturn(Result.success(createDidDocument(services)));

        var result = resolver.toFederatedCacheNode(participantDto(DID));

        assertThat(result.succeeded()).isTrue();
        var node = result.getContent();
        assertThat(node.getName()).isEqualTo(DID);
        assertThat(node.getTargetUrl()).isEqualTo(DSP_URL);
        assertThat(node.getSupportedProtocols()).containsExactly(SUPPORTED_PROTOCOL);
    }

    @ParameterizedTest
    @ArgumentsSource(StreamFailureProvider.class)
    void getNode_failure(Result<DidDocument> result) {
        when(didResolver.resolve(DID)).thenReturn(result);

        var nodeResult = resolver.toFederatedCacheNode(participantDto(DID));

        assertThat(nodeResult.failed()).isTrue();
    }

    @Test
    void getNode_success_twoDspMessagingServices() {
        var url1 = "url1.com";
        var url2 = "url2.com";
        when(didResolver.resolve(DID)).thenReturn(Result.success(createDidDocument(of(dspMessagingService(url2), dspMessagingService(url1)))));

        var result = resolver.toFederatedCacheNode(participantDto(DID));

        assertThat(result.succeeded()).isTrue();
        var node = result.getContent();
        assertThat(node.getName()).isEqualTo(DID);
        assertThat(node.getTargetUrl()).isIn(url1, url2);
        assertThat(node.getSupportedProtocols()).containsExactly(SUPPORTED_PROTOCOL);
    }

    private static class StreamSuccessProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    arguments(of(dspMessagingService(DSP_URL))),
                    arguments(of(dspMessagingService(DSP_URL), dspMessagingService(DSP_URL))),
                    arguments(of(dspMessagingService(DSP_URL), fakeService()))
            );
        }
    }

    private static class StreamFailureProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    arguments(Result.failure("failure")),
                    arguments(Result.success(createDidDocument(of(fakeService(), fakeService())))),
                    arguments(Result.success(createDidDocument(of(service(DSP_URL, "test-type"))))),
                    arguments(Result.success(createDidDocument(of())))
            );
        }
    }

    private static ParticipantDto participantDto(String did) {
        return new ParticipantDto(did, ParticipantDto.OnboardingStatus.ONBOARDED);
    }

}
