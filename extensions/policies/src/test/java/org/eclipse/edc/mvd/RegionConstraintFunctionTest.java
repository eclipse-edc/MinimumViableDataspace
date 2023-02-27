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

package org.eclipse.edc.mvd;

import org.eclipse.edc.identityhub.spi.credentials.model.Credential;
import org.eclipse.edc.identityhub.spi.credentials.model.CredentialSubject;
import org.eclipse.edc.policy.engine.PolicyContextImpl;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RegionConstraintFunctionTest {

    private static final Monitor MONITOR = mock(Monitor.class);
    private static final RegionConstraintFunction CONSTRAINT_FUNCTION = new RegionConstraintFunction(MONITOR);
    private static final Permission PERMISSION = Permission.Builder.newInstance().build();
    private static final String REGION_KEY = "region";
    private static final String EXPECTED_REGION = "eu";

    @Test
    void verifyPolicy_validRegion() {
        var claims = toMappedVerifiableCredentials(REGION_KEY, EXPECTED_REGION);
        var policyContext = getPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.EQ, EXPECTED_REGION, PERMISSION, policyContext)).isTrue();
    }

    @Test
    void verifyPolicy_invalidRegion() {
        var claims = toMappedVerifiableCredentials(REGION_KEY, "us");
        var policyContext = getPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.EQ, EXPECTED_REGION, PERMISSION, policyContext)).isFalse();
    }

    @Test
    void verifyPolicy_invalidClaimFormat() {
        var claims = Map.of(UUID.randomUUID().toString(), (Object) UUID.randomUUID().toString());
        var policyContext = getPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.EQ, EXPECTED_REGION, PERMISSION, policyContext)).isFalse();
    }

    @Test
    void verifyPolicy_invalidRegionFormat() {
        // Region is a map instead of a string.
        var claims = toMappedVerifiableCredentials(REGION_KEY, Map.of());
        var policyContext = getPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.EQ, EXPECTED_REGION, PERMISSION, policyContext)).isFalse();
    }

    @Test
    void verifyPolicy_unsupportedOperator() {
        var claims = toMappedVerifiableCredentials(REGION_KEY, EXPECTED_REGION);
        var policyContext = getPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.GT, EXPECTED_REGION, PERMISSION, policyContext)).isFalse();
    }

    @Test
    void verifyPolicy_NeqOperatorValidRegion() {
        var claims = toMappedVerifiableCredentials(REGION_KEY, "us");
        var policyContext = getPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.NEQ, EXPECTED_REGION, PERMISSION, policyContext)).isTrue();
    }

    @Test
    void verifyPolicy_NeqOperatorInvalidRegion() {
        var claims = toMappedVerifiableCredentials(REGION_KEY, EXPECTED_REGION);
        var policyContext = getPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.NEQ, EXPECTED_REGION, PERMISSION, policyContext)).isFalse();
    }

    @Test
    void verifyPolicy_InOperatorValidRegion() {
        var claims = toMappedVerifiableCredentials(REGION_KEY, EXPECTED_REGION);
        var policyContext = getPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.IN, List.of(EXPECTED_REGION), PERMISSION, policyContext)).isTrue();
    }

    @Test
    void verifyPolicy_InOperatorInValidRegion() {
        var claims = toMappedVerifiableCredentials(REGION_KEY, "us");
        var policyContext = getPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.IN, List.of(EXPECTED_REGION), PERMISSION, policyContext)).isFalse();
    }

    private PolicyContext getPolicyContext(Map<String, Object> claims) {
        return new PolicyContextImpl(new ParticipantAgent(claims, Map.of()), Map.of());
    }

    private Map<String, Object> toMappedVerifiableCredentials(String key, Object value) {
        var credentialId = UUID.randomUUID().toString();
        var credential = Credential.Builder.newInstance()
                .id("test")
                .context("test")
                .type("VerifiableCredential")
                .issuer("did:web:" + UUID.randomUUID())
                .issuanceDate(Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS)))
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id("test")
                        .claim(key, value)
                        .build())
                .build();
        return Map.of(credentialId, credential);
    }
}
