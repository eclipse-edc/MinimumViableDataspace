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
import org.eclipse.edc.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.edc.registration.client.RegistryApiClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.AbstractResult;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Federated cache directory using Registration Service as backend.
 */
public class RegistrationServiceNodeDirectory implements FederatedCacheNodeDirectory {

    private final RegistryApiClient apiClient;
    private final FederatedCacheNodeResolver resolver;
    private final Monitor monitor;

    /**
     * Constructs {@link RegistrationServiceNodeDirectory}
     *
     * @param monitor   monitor
     * @param apiClient RegistrationService API client.
     * @param resolver  gets {@link FederatedCacheNode} from {@link org.eclipse.edc.registration.client.model.ParticipantDto}.
     */
    public RegistrationServiceNodeDirectory(RegistryApiClient apiClient, FederatedCacheNodeResolver resolver, Monitor monitor) {
        this.apiClient = apiClient;
        this.resolver = resolver;
        this.monitor = monitor;
    }

    @Override
    public List<FederatedCacheNode> getAll() {
        try {
            return apiClient.listParticipants()
                    .map(list -> list.stream()
                            .map(resolver::toFederatedCacheNode)
                            .filter(AbstractResult::succeeded)
                            .map(AbstractResult::getContent)
                            .collect(Collectors.toList()))
                    .orElse(apiFailure -> {
                        monitor.warning("RegistrationServiceNodeDirectory.getAll() failed " + apiFailure.getFailureDetail());
                        return List.of();
                    });
        } catch (Exception ex) {
            monitor.severe("RegistrationServiceNodeDirectory.getAll() threw an exception: " + ex.getMessage());
            return List.of();
        }
    }

    @Override
    public void insert(FederatedCacheNode federatedCacheNode) {
        throw new UnsupportedOperationException();
    }
}
