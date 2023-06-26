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

import org.eclipse.edc.catalog.spi.FederatedCacheNode;
import org.eclipse.edc.registration.client.RegistryApiClient;
import org.eclipse.edc.registration.client.model.ParticipantDto;
import org.eclipse.edc.registration.client.response.ApiResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegistrationServiceNodeDirectoryTest {

    private final RegistryApiClient registryApi = mock(RegistryApiClient.class);
    private final FederatedCacheNodeResolver resolver = mock(FederatedCacheNodeResolver.class);
    private final Monitor monitor = mock(Monitor.class);

    private final RegistrationServiceNodeDirectory directory = new RegistrationServiceNodeDirectory(registryApi, resolver, monitor);

    @Test
    void getAll_emptyList() {
        when(registryApi.listParticipants()).thenReturn(ApiResult.success(List.of()));

        var cacheNodes = directory.getAll();
        assertThat(cacheNodes).isEmpty();
    }

    @Test
    void getAll() {
        var company1 = getParticipant();
        var company2 = getParticipant();
        var node1 = node();
        var node2 = node();
        when(registryApi.listParticipants()).thenReturn(ApiResult.success(List.of(company1, company2)));
        when(resolver.toFederatedCacheNode(company1)).thenReturn(Result.success(node1));
        when(resolver.toFederatedCacheNode(company2)).thenReturn(Result.success(node2));

        var cacheNodes = directory.getAll();
        assertThat(cacheNodes)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(node1, node2);
    }

    @Test
    void getAll_failureResolvingDid() {
        var company1 = getParticipant();
        var company2 = getParticipant();
        var node1 = node();
        when(registryApi.listParticipants()).thenReturn(ApiResult.success(List.of(company1, company2)));
        when(resolver.toFederatedCacheNode(company1)).thenReturn(Result.success(node1));
        when(resolver.toFederatedCacheNode(company2)).thenReturn(Result.failure("failure"));

        var cacheNodes = directory.getAll();
        assertThat(cacheNodes)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(node1);
    }

    private FederatedCacheNode node() {
        return new FederatedCacheNode("test-name", "http://test.target.url", List.of("dataspace-protocol-http"));
    }

    @NotNull
    private ParticipantDto getParticipant() {
        return new ParticipantDto(format("did:web:%s", "test-domainname-" + UUID.randomUUID()), ParticipantDto.OnboardingStatus.ONBOARDED);
    }
}
