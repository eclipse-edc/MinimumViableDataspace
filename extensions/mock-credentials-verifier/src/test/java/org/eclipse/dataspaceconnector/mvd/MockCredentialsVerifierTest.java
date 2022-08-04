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

import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.identityhub.credentials.VerifiableCredentialsJwtService.VERIFIABLE_CREDENTIALS_KEY;

class MockCredentialsVerifierTest {
    private static final String CREDENTIAL_SUBJECT_KEY = "credentialSubject";
    MockCredentialsVerifier verifier = new MockCredentialsVerifier(new ConsoleMonitor());

    @Test
    void verifyCredentials() {
        Result<Map<String, Object>> actual = getVerifiedCredentials("http://dummy.site/foo?region=us&tier=GOLD");
        assertThat(actual.succeeded()).isTrue();
        assertThat(actual.getContent())
                .extracting(c -> c.values().stream().findFirst().get())
                .extracting(VERIFIABLE_CREDENTIALS_KEY)
                .extracting(CREDENTIAL_SUBJECT_KEY)
                .isEqualTo(Map.of("region", "us", "tier", "GOLD"));
    }

    @Test
    void verifyCredentials_failsOnMalformedUrl() {
        assertThatThrownBy(() -> getVerifiedCredentials("malformed_url"))
                .isInstanceOf(EdcException.class);
    }

    @Test
    void verifyCredentials_failsOnMissingIdentityHubUrl() {
        var didDocument = DidDocument.Builder.newInstance().build();
        assertThat(verifier.getVerifiedCredentials(didDocument).failed());
    }

    private Result<Map<String, Object>> getVerifiedCredentials(String identityHubUrl) {
        var didDocument = DidDocument.Builder.newInstance()
                .service(List.of(
                        new Service(UUID.randomUUID().toString(), "IdentityHub", identityHubUrl)
                )).build();
        return verifier.getVerifiedCredentials(didDocument);
    }
}