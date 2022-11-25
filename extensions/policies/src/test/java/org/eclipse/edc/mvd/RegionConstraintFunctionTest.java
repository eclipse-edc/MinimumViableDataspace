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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.policy.engine.PolicyContextImpl;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.spi.credentials.VerifiableCredentialsJwtService.VERIFIABLE_CREDENTIALS_KEY;

public class RegionConstraintFunctionTest {
    private static final String VERIFIABLE_CREDENTIAL_ID_KEY = "id";
    private static final String CREDENTIAL_SUBJECT_KEY = "credentialSubject";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Monitor MONITOR = new ConsoleMonitor();
    private static final RegionConstraintFunction CONSTRAINT_FUNCTION = new RegionConstraintFunction(OBJECT_MAPPER, MONITOR);
    private static final Permission PERMISSION = Permission.Builder.newInstance().build();
    private static final String REGION_KEY = "region";
    private static final String EXPECTED_REGION = "eu";
    private static final String ISSUER_KEY = "iss";

    @Test
    public void verifyPolicy_validRegion() {
        var claims = toMappedVerifiableCredentials(Map.of(REGION_KEY, EXPECTED_REGION));
        var policyContext = getPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.EQ, EXPECTED_REGION, PERMISSION, policyContext)).isTrue();
    }

    @Test
    public void verifyPolicy_invalidRegion() {
        var claims = toMappedVerifiableCredentials(Map.of(REGION_KEY, "us"));
        var policyContext = getPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.EQ, EXPECTED_REGION, PERMISSION, policyContext)).isFalse();
    }

    @Test
    public void verifyPolicy_invalidClaimFormat() {
        var claims = Map.of(UUID.randomUUID().toString(), (Object) UUID.randomUUID().toString());
        var policyContext = getPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.EQ, EXPECTED_REGION, PERMISSION, policyContext)).isFalse();
    }

    @Test
    public void verifyPolicy_invalidRegionFormat() {
        // Region is a map instead of a string.
        var claims = toMappedVerifiableCredentials(Map.of(REGION_KEY, Map.of()));
        var policyContext = getPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.EQ, EXPECTED_REGION, PERMISSION, policyContext)).isFalse();
    }

    @Test
    public void verifyPolicy_unsupportedOperator() {
        var claims = toMappedVerifiableCredentials(Map.of(REGION_KEY, EXPECTED_REGION));
        var policyContext = getPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.GT, EXPECTED_REGION, PERMISSION, policyContext)).isFalse();
    }

    @Test
    public void verifyPolicy_NeqOperatorValidRegion() {
        var claims = toMappedVerifiableCredentials(Map.of(REGION_KEY, "us"));
        var policyContext = getPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.NEQ, EXPECTED_REGION, PERMISSION, policyContext)).isTrue();
    }

    @Test
    public void verifyPolicy_NeqOperatorInvalidRegion() {
        var claims = toMappedVerifiableCredentials(Map.of(REGION_KEY, EXPECTED_REGION));
        var policyContext = getPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.NEQ, EXPECTED_REGION, PERMISSION, policyContext)).isFalse();
    }

    @Test
    public void verifyPolicy_InOperatorValidRegion() {
        var claims = toMappedVerifiableCredentials(Map.of(REGION_KEY, EXPECTED_REGION));
        var policyContext = getPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.IN, List.of(EXPECTED_REGION), PERMISSION, policyContext)).isTrue();
    }

    @Test
    public void verifyPolicy_InOperatorInValidRegion() {
        var claims = toMappedVerifiableCredentials(Map.of(REGION_KEY, "us"));
        var policyContext = getPolicyContext(claims);
        assertThat(CONSTRAINT_FUNCTION.evaluate(Operator.IN, List.of(EXPECTED_REGION), PERMISSION, policyContext)).isFalse();
    }

    private PolicyContext getPolicyContext(Map<String, Object> claims) {
        return new PolicyContextImpl(new ParticipantAgent(claims, Map.of()), Map.of());
    }

    private Map<String, Object> toMappedVerifiableCredentials(Map<String, Object> regionClaims) {
        var vcId = UUID.randomUUID().toString();
        return Map.of(vcId,
                Map.of(VERIFIABLE_CREDENTIALS_KEY, Map.of(CREDENTIAL_SUBJECT_KEY, regionClaims,
                                VERIFIABLE_CREDENTIAL_ID_KEY, vcId),
                        // issuer will be ignored when applying policies for now.
                        ISSUER_KEY, String.join("did:web:", UUID.randomUUID().toString())));
    }
}
