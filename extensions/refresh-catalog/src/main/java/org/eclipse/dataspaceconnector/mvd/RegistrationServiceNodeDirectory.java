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

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.registration.client.api.RegistryApi;
import org.eclipse.dataspaceconnector.registration.client.models.ParticipantDto;
import org.eclipse.dataspaceconnector.spi.result.AbstractResult;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Federated cache directory using Registration Service as backend.
 */
public class RegistrationServiceNodeDirectory implements FederatedCacheNodeDirectory {

    private final RegistryApi apiClient;
    private final FederatedCacheNodeResolver resolver;

    /**
     * Constructs {@link RegistrationServiceNodeDirectory}
     *
     * @param apiClient RegistrationService API client.
     * @param resolver gets {@link FederatedCacheNode} from {@link ParticipantDto}
     */
    public RegistrationServiceNodeDirectory(RegistryApi apiClient, FederatedCacheNodeResolver resolver) {
        this.apiClient = apiClient;
        this.resolver = resolver;
    }

    @Override
    public List<FederatedCacheNode> getAll() {
        return apiClient.listParticipants().stream()
                .filter(participant -> participant.getStatus() == ParticipantDto.StatusEnum.ONBOARDED)
                .map(resolver::toFederatedCacheNode)
                .filter(AbstractResult::succeeded)
                .map(AbstractResult::getContent)
                .collect(Collectors.toList());
    }

    @Override
    public void insert(FederatedCacheNode federatedCacheNode) {
        throw new UnsupportedOperationException();
    }
}
