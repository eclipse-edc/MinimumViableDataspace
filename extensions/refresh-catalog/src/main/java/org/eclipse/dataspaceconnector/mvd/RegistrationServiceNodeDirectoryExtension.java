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

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.registration.client.ApiClientFactory;
import org.eclipse.dataspaceconnector.registration.client.api.RegistryApi;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

/**
 * Extension to set up federated cache directory using Registration Service API as backend.
 */
public class RegistrationServiceNodeDirectoryExtension implements ServiceExtension {

    @EdcSetting
    private static final String REGISTRATION_SERVICE_API_URL = "registration.service.api.url";
    private static final String REGISTRATION_SERVICE_API_URL_DEFAULT = "http://localhost:8182/authority";

    @Inject
    private Monitor monitor;

    @Inject
    private TypeManager typeManager;

    @Inject
    private IdentityService identityService;

    private String registrationServiceApiUrl;

    @Override
    public void initialize(ServiceExtensionContext context) {
        registrationServiceApiUrl = context.getSetting(
                REGISTRATION_SERVICE_API_URL, REGISTRATION_SERVICE_API_URL_DEFAULT);
    }

    @Provider
    public FederatedCacheNodeDirectory federatedCacheNodeDirectory() {
        var apiClient = ApiClientFactory.createApiClient(registrationServiceApiUrl, identityService::obtainClientCredentials);
        var registryApiClient = new RegistryApi(apiClient);
        return new RegistrationServiceNodeDirectory(registryApiClient);
    }
}


