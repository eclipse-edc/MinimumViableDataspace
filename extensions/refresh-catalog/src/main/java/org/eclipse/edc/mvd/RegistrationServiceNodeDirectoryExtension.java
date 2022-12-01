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

import org.eclipse.edc.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.registration.client.ApiClientFactory;
import org.eclipse.edc.registration.client.api.RegistryApi;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;


/**
 * Extension to set up federated cache directory using Registration Service API as backend.
 */
public class RegistrationServiceNodeDirectoryExtension implements ServiceExtension {

    @Setting
    private static final String REGISTRATION_SERVICE_API_URL = "registration.service.api.url";
    private static final String REGISTRATION_SERVICE_API_URL_DEFAULT = "http://localhost:8182/authority";

    @Inject
    private Monitor monitor;

    @Inject
    private TypeManager typeManager;

    @Inject
    private IdentityService identityService;

    @Inject
    private DidResolverRegistry didResolverRegistry;

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
        var resolver = new FederatedCacheNodeResolver(didResolverRegistry, monitor);
        return new RegistrationServiceNodeDirectory(registryApiClient, resolver);
    }
}


