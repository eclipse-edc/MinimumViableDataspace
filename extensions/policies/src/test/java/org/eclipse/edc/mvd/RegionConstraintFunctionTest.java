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
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegionConstraintFunctionTest {

    private static final RegionConstraintFunction CONSTRAINT_FUNCTION = new RegionConstraintFunction();
    private static final Permission PERMISSION = Permission.Builder.newInstance().build();
    private static final String REGION_KEY = "region";
    private static final String REGION_EU = "eu";

    @Test
    void verifyPolicy_validRegion() {
        var claims = toCredentialsMap(REGION_KEY, REGION_EU);
        var policyContext = toPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.EQ, REGION_EU, PERMISSION, policyContext)).isTrue();
    }

    @Test
    void verifyPolicy_invalidRegion() {
        var claims = toCredentialsMap(REGION_KEY, "us");
        var policyContext = toPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.EQ, REGION_EU, PERMISSION, policyContext)).isFalse();
    }

    @Test
    void verifyPolicy_invalidClaimFormat() {
        var claims = Map.of(UUID.randomUUID().toString(), (Object) UUID.randomUUID().toString());
        var policyContext = toPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.EQ, REGION_EU, PERMISSION, policyContext)).isFalse();
    }

    @Test
    void verifyPolicy_invalidRegionFormat() {
        // Region is a map instead of a string.
        var claims = toCredentialsMap(REGION_KEY, Map.of());
        var policyContext = toPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.EQ, REGION_EU, PERMISSION, policyContext)).isFalse();
    }

    @Test
    void verifyPolicy_unsupportedOperator() {
        var claims = toCredentialsMap(REGION_KEY, REGION_EU);
        var policyContext = toPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.GT, REGION_EU, PERMISSION, policyContext)).isFalse();
    }

    @Test
    void verifyPolicy_NeqOperatorValidRegion() {
        var claims = toCredentialsMap(REGION_KEY, "us");
        var policyContext = toPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.NEQ, REGION_EU, PERMISSION, policyContext)).isTrue();
    }

    @Test
    void verifyPolicy_NeqOperatorInvalidRegion() {
        var claims = toCredentialsMap(REGION_KEY, REGION_EU);
        var policyContext = toPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.NEQ, REGION_EU, PERMISSION, policyContext)).isFalse();
    }

    @Test
    void verifyPolicy_InOperatorValidRegion() {
        var claims = toCredentialsMap(REGION_KEY, REGION_EU);
        var policyContext = toPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.IN, List.of(REGION_EU), PERMISSION, policyContext)).isTrue();
    }

    @Test
    void verifyPolicy_InOperatorInValidRegion() {
        var claims = toCredentialsMap(REGION_KEY, "us");
        var policyContext = toPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.IN, List.of(REGION_EU), PERMISSION, policyContext)).isFalse();
    }

    private PolicyContext toPolicyContext(Map<String, Object> claims) {
        return new PolicyContextImpl(new ParticipantAgent(claims, Map.of()), Map.of());
    }

    private Map<String, Object> toCredentialsMap(String key, Object value) {
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
