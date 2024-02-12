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

package org.eclipse.edc.demo.iatp.policy;

import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.agent.ParticipantAgent;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class MembershipCredentialEvaluationFunction implements AtomicConstraintFunction<Permission> {
    public static final String MEMBERSHIP_CONSTRAINT_KEY = "MembershipCredential";

    private static final String MEMBERSHIP_CLAIM = "https://w3id.org/catenax/credentials/membership";
    private static final String MEMBERSHIP_SINCE_CLAIM = "https://w3id.org/catenax/credentials/since";

    @SuppressWarnings("unchecked")
    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Permission permission, PolicyContext policyContext) {
        if (!operator.equals(Operator.EQ)) {
            policyContext.reportProblem("Invalid operator '%s', only accepts '%s'".formatted(operator, Operator.EQ));
            return false;
        }
        var pa = policyContext.getContextData(ParticipantAgent.class);
        if (pa == null) {
            policyContext.reportProblem("No ParticipantAgent found on context.");
            return false;
        }
        var claims = pa.getClaims();
        Map<String, List<?>> membership = (Map<String, List<?>>) claims.get(MEMBERSHIP_CLAIM);
        if ("active".equalsIgnoreCase(rightOperand.toString())) {
            String since = getArrayValue(membership.get(MEMBERSHIP_SINCE_CLAIM));
            var membershipStartDate = Instant.parse(since);

            return membershipStartDate.isBefore(Instant.now());
        }
        return false;
    }

    private <T> T getArrayValue(List entry) {
        return (T) ((Map) entry.get(0)).get(JsonLdKeywords.VALUE);
    }

}
