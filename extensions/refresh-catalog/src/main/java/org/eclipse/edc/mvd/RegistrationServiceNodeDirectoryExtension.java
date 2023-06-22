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
import org.eclipse.edc.registration.client.RegistryApiClientFactory;
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

    @Inject
    private Monitor monitor;

    @Inject
    private TypeManager typeManager;

    @Inject
    private IdentityService identityService;

    @Inject
    private DidResolverRegistry didResolverRegistry;

    @Provider
    public FederatedCacheNodeDirectory federatedCacheNodeDirectory(ServiceExtensionContext context) {
        var registrationServiceApiUrl = context.getConfig().getString(REGISTRATION_SERVICE_API_URL);
        var apiClient = RegistryApiClientFactory.createApiClient(registrationServiceApiUrl, identityService::obtainClientCredentials, monitor, typeManager.getMapper());
        var resolver = new FederatedCacheNodeResolver(didResolverRegistry, monitor);
        return new RegistrationServiceNodeDirectory(apiClient, resolver, monitor);
    }
}


