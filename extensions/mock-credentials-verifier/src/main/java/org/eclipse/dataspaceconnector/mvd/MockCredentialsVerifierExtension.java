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
 *       Microsoft Corporation - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.mvd;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * Extension to set up the {@link MockCredentialsVerifier} service to generate stub claims.
 */
@Provides(CredentialsVerifier.class)
public class MockCredentialsVerifierExtension implements ServiceExtension {

    @Inject
    private OkHttpClient httpClient;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var credentialsVerifier = new MockCredentialsVerifier(context.getMonitor(), httpClient, context.getTypeManager());
        context.registerService(CredentialsVerifier.class, credentialsVerifier);
    }
}
