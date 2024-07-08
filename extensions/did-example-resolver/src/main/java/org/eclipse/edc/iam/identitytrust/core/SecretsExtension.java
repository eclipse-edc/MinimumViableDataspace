/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.core;

import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.time.Clock;


public class SecretsExtension implements ServiceExtension {
    // duplicated from DcpDefaultServicesExtension
    private static final String STS_PRIVATE_KEY_ALIAS = "edc.iam.sts.privatekey.alias";
    private static final String STS_PUBLIC_KEY_ID = "edc.iam.sts.publickey.id";
    @Inject
    private Vault vault;

    @Override
    public void initialize(ServiceExtensionContext context) {
        seedKeys(context);
    }

    @Provider
    public Clock clock() {
        // THIS IS A DIRTY HACK, so this extension is intialized before the DcpDefaultServicesExtension, which needs the secrets!
        return Clock.systemUTC();
    }

    /**
     * We need this, because we don't have a vault that is shared between Connector and IdentityHub, so this needs to be seeded to either of them.
     *
     * @param context the service extension context used for accessing configuration and other services
     */
    private void seedKeys(ServiceExtensionContext context) {
        // Let's avoid pulling in the connector-core module, just for the instanceof check
        if (vault.getClass().getSimpleName().equals("InMemoryVault")) {
            var publicKey = """
                    -----BEGIN PUBLIC KEY-----
                    MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE1l0Lof0a1yBc8KXhesAnoBvxZw5r
                    oYnkAXuqCYfNK3ex+hMWFuiXGUxHlzShAehR6wvwzV23bbC0tcFcVgW//A==
                    -----END PUBLIC KEY-----
                    """;

            var privateKey = """
                    -----BEGIN EC PRIVATE KEY-----
                    MHcCAQEEIARDUGJgKy1yzxkueIJ1k3MPUWQ/tbQWQNqW6TjyHpdcoAoGCCqGSM49
                    AwEHoUQDQgAE1l0Lof0a1yBc8KXhesAnoBvxZw5roYnkAXuqCYfNK3ex+hMWFuiX
                    GUxHlzShAehR6wvwzV23bbC0tcFcVgW//A==
                    -----END EC PRIVATE KEY-----
                    """;


            vault.storeSecret(context.getConfig().getString(STS_PRIVATE_KEY_ALIAS), privateKey);
            vault.storeSecret(context.getConfig().getString(STS_PUBLIC_KEY_ID), publicKey);

            context.getMonitor().withPrefix("DEMO").warning(">>>>>> This extension hard-codes a keypair into the vault! <<<<<<");
        }
    }
}
