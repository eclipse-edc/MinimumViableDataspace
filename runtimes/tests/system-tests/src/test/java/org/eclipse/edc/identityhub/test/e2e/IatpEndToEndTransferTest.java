/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.test.e2e;

import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EdcClassRuntimesExtension;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;

@EndToEndTest
public class IatpEndToEndTransferTest {
    protected static final EdcRuntimeExtension aliceIdentityHub = new EdcRuntimeExtension(":launchers:identity-hub", "alice-identityhub", new HashMap<>() {
        {
            put("web.http.port", "7080");
            put("web.http.path", "/api");
            put("web.http.resolution.port", "7081");
            put("web.http.resolution.path", "/api/resolution");
            put("web.http.management.port", "7082");
            put("web.http.management.path", "/api/management");
            put("web.http.did.port", "7083");
            put("web.http.did.path", "/");
            put("edc.ih.iam.id", "BPN0000001");
            put("edc.ih.iam.publickey.path", TestUtils.getResource("ih-public.pem").getPath());
        }
    });
    protected static final EdcRuntimeExtension aliceConnector = new EdcRuntimeExtension(":launchers:connector", "alice-connector", new HashMap<>() {
        {
            put("edc.iam.issuer.id", "did:web:alice-identityhub%3A7083:connector1");
            put("web.http.port", "8080");
            put("web.http.path", "/");
            put("web.http.management.port", "8081");
            put("web.http.management.path", "/api/management/");
            put("web.http.protocol.port", "8082");
            put("web.http.protocol.path", "/api/dsp");
            put("edc.api.auth.key", "password");
            put("edc.iam.sts.privatekey.alias", "my-private-key");
            put("edc.iam.sts.publickey.alias", "my-public-key");
            put("edc.dsp.callback.address", "http://localhost:8082/api/dsp");
            put("edc.iam.credentialservice.url", "http://localhost:7091/api/resolution/v1/presentation/query");
            put("edc.participant.id", "BPN0000001");
        }
    });
    @RegisterExtension
    static EdcClassRuntimesExtension alice = new EdcClassRuntimesExtension(
            aliceConnector,
            aliceIdentityHub
    );

    @Test
    void foo() {

    }
}
