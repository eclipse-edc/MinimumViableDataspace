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
import org.eclipse.edc.registration.client.api.RegistryApi;
import org.eclipse.edc.registration.client.models.ParticipantDto;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegistrationServiceNodeDirectoryTest {

    private final RegistryApi registryApi = mock(RegistryApi.class);
    private final FederatedCacheNodeResolver resolver = mock(FederatedCacheNodeResolver.class);

    @Test
    void getAll_emptyList() {
        var service = new RegistrationServiceNodeDirectory(registryApi, resolver);

        when(registryApi.listParticipants()).thenReturn(List.of());

        var cacheNodes = service.getAll();
        assertThat(cacheNodes).isEmpty();
    }

    @Test
    void getAll() {
        var service = new RegistrationServiceNodeDirectory(registryApi, resolver);

        var company1 = getParticipant();
        var company2 = getParticipant();
        var node1 = node();
        var node2 = node();
        when(registryApi.listParticipants()).thenReturn(List.of(company1, company2));
        when(resolver.toFederatedCacheNode(company1)).thenReturn(Result.success(node1));
        when(resolver.toFederatedCacheNode(company2)).thenReturn(Result.success(node2));

        var cacheNodes = service.getAll();
        assertThat(cacheNodes)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(node1, node2);
    }

    @Test
    void getAll_failureResolvingDid() {
        var service = new RegistrationServiceNodeDirectory(registryApi, resolver);

        var company1 = getParticipant();
        var company2 = getParticipant();
        var node1 = node();
        when(registryApi.listParticipants()).thenReturn(List.of(company1, company2));
        when(resolver.toFederatedCacheNode(company1)).thenReturn(Result.success(node1));
        when(resolver.toFederatedCacheNode(company2)).thenReturn(Result.failure("failure"));

        var cacheNodes = service.getAll();
        assertThat(cacheNodes)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(node1);
    }

    private FederatedCacheNode node() {
        return new FederatedCacheNode("test-name", "http://test.target.url", List.of("ids-multipart"));
    }

    @NotNull
    private ParticipantDto getParticipant() {
        var participant = new ParticipantDto();
        participant.setDid(String.format("did:web:%s", "test-domainname-" + UUID.randomUUID()));
        return participant;
    }
}
