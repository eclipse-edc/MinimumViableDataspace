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

package org.eclipse.edc.demo.iatp.core;

import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.time.Clock;

import static org.eclipse.edc.iam.identitytrust.core.IatpDefaultServicesExtension.STS_PRIVATE_KEY_ALIAS;
import static org.eclipse.edc.iam.identitytrust.core.IatpDefaultServicesExtension.STS_PUBLIC_KEY_ALIAS;

public class SecretsExtension implements ServiceExtension {

    @Inject
    private Vault vault;

    @Override
    public void initialize(ServiceExtensionContext context) {
        seedKeys(context);
    }

    @Provider
    public Clock clock() {
        // THIS IS A DIRTY HACK, so this extension is intialized before the IatpDefaultServicesExtension, which needs the secrets!
        return Clock.systemUTC();
    }

    private void seedKeys(ServiceExtensionContext context) {
        var publickey = """
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
        vault.storeSecret(context.getConfig().getString(STS_PUBLIC_KEY_ALIAS), publickey);
    }
}
