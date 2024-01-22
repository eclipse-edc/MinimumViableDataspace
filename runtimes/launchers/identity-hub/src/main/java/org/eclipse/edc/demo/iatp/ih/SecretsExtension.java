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

package org.eclipse.edc.demo.iatp.ih;

import org.eclipse.edc.identityhub.spi.generator.PresentationCreatorRegistry;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;


public class SecretsExtension implements ServiceExtension {

    @Inject
    private Vault vault;

    @Inject
    private PresentationCreatorRegistry registry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        seedKeys(context);
    }

    private void seedKeys(ServiceExtensionContext context) {
        var privateKey = """
                -----BEGIN EC PRIVATE KEY-----
                MHcCAQEEIARDUGJgKy1yzxkueIJ1k3MPUWQ/tbQWQNqW6TjyHpdcoAoGCCqGSM49
                AwEHoUQDQgAE1l0Lof0a1yBc8KXhesAnoBvxZw5roYnkAXuqCYfNK3ex+hMWFuiX
                GUxHlzShAehR6wvwzV23bbC0tcFcVgW//A==
                -----END EC PRIVATE KEY-----
                """;

        var alias1 = "did:web:alice-identityhub%3A7083:connector1#key1";
        var alias2 = "did:web:bob-identityhub%3A7083:connector2#key1";

        addKey(alias1, privateKey);
        addKey(alias2, privateKey);

    }

    private void addKey(String privateKeyAlias, String privateKey) {
        vault.storeSecret(privateKeyAlias, privateKey);
        registry.addKeyId(privateKeyAlias, CredentialFormat.JSON_LD);
        registry.addKeyId(privateKeyAlias, CredentialFormat.JWT);
    }
}
