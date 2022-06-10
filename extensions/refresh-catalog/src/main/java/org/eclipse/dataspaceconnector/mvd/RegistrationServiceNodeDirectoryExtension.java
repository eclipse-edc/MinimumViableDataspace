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
import org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions;
import org.eclipse.dataspaceconnector.registration.client.ApiClientFactory;
import org.eclipse.dataspaceconnector.registration.client.api.RegistryApi;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

/**
 * Extension to set up federated cache directory using Registration Service API as backend.
 */
@Provides(FederatedCacheNodeDirectory.class)
public class RegistrationServiceNodeDirectoryExtension implements ServiceExtension {

    static final String API_URL = "http://localhost:8181/api";

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        TypeManager typeManager = context.getTypeManager();
        var registrationServiceApiUrl = ConfigurationFunctions.propOrEnv("registration.service.api.url", API_URL);
        var service = new RegistrationServiceNodeDirectory(new RegistryApi(ApiClientFactory.createApiClient(registrationServiceApiUrl)));
        context.registerService(FederatedCacheNodeDirectory.class, service);
    }
}


