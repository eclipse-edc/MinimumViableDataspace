/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.mvd.dataplane.keyseed;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.Date;
import java.util.UUID;

public class KeySeedExtension implements ServiceExtension {

    @Setting(key = "edc.transfer.proxy.token.signer.privatekey.alias")
    private String tokenSignerPrivateKeyAlias;

    @Setting(key = "edc.transfer.proxy.token.verifier.publickey.alias")
    private String tokenVerifierPublicKeyAlias;

    @Inject
    private Vault vault;

    @Override
    public void initialize(ServiceExtensionContext context) {
        try {
            var jwk = new OctetKeyPairGenerator(Curve.Ed25519)
                    .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key (optional)
                    .keyID(UUID.randomUUID().toString()) // give the key a unique ID (optional)
                    .issueTime(new Date())
                    .generate();
            vault.storeSecret(tokenSignerPrivateKeyAlias, jwk.toJSONString());
            vault.storeSecret(tokenVerifierPublicKeyAlias, jwk.toPublicJWK().toJSONString());
            context.getMonitor().info("Key seed extension initialized: private key: %s, public key: %s".formatted(tokenSignerPrivateKeyAlias, tokenVerifierPublicKeyAlias));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
