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

package org.eclipse.dataspaceconnector.mvd;

import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.registration.client.models.ParticipantDto;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FederatedCacheNodeResolverTest {

    static final String IDS_MESSAGING = "IDSMessaging";
    public static final String SUPPORTED_PROTOCOL = "ids-multipart";
    static String did = "did:web:" + "test-domainname";
    static String idsUrl = "test.ids.url";

    FederatedCacheNodeResolver resolver;
    DidResolverRegistry didResolver = mock(DidResolverRegistry.class);
    Monitor monitor = mock(Monitor.class);

    @ParameterizedTest
    @MethodSource("argumentsStreamSuccess")
    void getNode_success(List<Service> services) {
        when(didResolver.resolve(did)).thenReturn(Result.success(getDidDocument(services)));

        resolver = new FederatedCacheNodeResolver(didResolver, monitor);

        var result = resolver.toFederatedCacheNode(new ParticipantDto().did(did));

        assertThat(result.succeeded()).isTrue();
        var node = result.getContent();
        assertThat(node.getName()).isEqualTo(did);
        assertThat(node.getTargetUrl()).isEqualTo(idsUrl);
        assertThat(node.getSupportedProtocols()).containsExactly(SUPPORTED_PROTOCOL);
    }

    @ParameterizedTest
    @MethodSource("argumentsStreamFailure")
    void getNode_failure(Result result) {
        when(didResolver.resolve(did)).thenReturn(result);

        resolver = new FederatedCacheNodeResolver(didResolver, monitor);

        var nodeResult = resolver.toFederatedCacheNode(new ParticipantDto().did(did));

        assertThat(nodeResult.failed()).isTrue();
    }

    @Test
    void getNode_success_twoIdsMessagingServices() {
        String url1 = "url1.com";
        String url2 = "url2.com";
        when(didResolver.resolve(did)).thenReturn(Result.success(getDidDocument(of(idsMessagingService(url2), idsMessagingService(url1)))));

        resolver = new FederatedCacheNodeResolver(didResolver, monitor);

        var result = resolver.toFederatedCacheNode(new ParticipantDto().did(did));

        assertThat(result.succeeded()).isTrue();
        var node = result.getContent();
        assertThat(node.getName()).isEqualTo(did);
        assertThat(node.getTargetUrl()).isIn(url1, url2);
        assertThat(node.getSupportedProtocols()).containsExactly(SUPPORTED_PROTOCOL);
    }

    @NotNull
    private static DidDocument getDidDocument(List<Service> services) {
        return DidDocument.Builder.newInstance().id(did).service(services).build();
    }

    private static Stream<Arguments> argumentsStreamSuccess() {
        return Stream.of(
                arguments(of(idsMessagingService(idsUrl))),
                arguments(of(idsMessagingService(idsUrl), idsMessagingService(idsUrl))),
                arguments(of(idsMessagingService(idsUrl), fakeService()))
        );
    }

    private static Stream<Arguments> argumentsStreamFailure() {
        return Stream.of(
                arguments(Result.failure("failure")),
                arguments(Result.success(getDidDocument(of(fakeService(), fakeService())))),
                arguments(Result.success(getDidDocument(of(service(idsUrl, "test-type"))))),
                arguments(Result.success(getDidDocument(of())))
        );
    }

    @NotNull
    private static Service fakeService() {
        return service("test-url", "test-type");
    }

    @NotNull
    private static Service idsMessagingService(String url) {
        return service(url, IDS_MESSAGING);
    }

    @NotNull
    private static Service service(String url, String type) {
        return new Service("test-url", type, url);
    }

}