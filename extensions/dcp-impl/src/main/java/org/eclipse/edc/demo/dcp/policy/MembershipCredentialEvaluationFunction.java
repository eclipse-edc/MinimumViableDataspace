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

package org.eclipse.edc.demo.dcp.policy;

import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.agent.ParticipantAgent;

import java.time.Instant;
import java.util.Map;

public class MembershipCredentialEvaluationFunction extends AbstractCredentialEvaluationFunction implements AtomicConstraintFunction<Permission> {
    public static final String MEMBERSHIP_CONSTRAINT_KEY = "MembershipCredential";

    private static final String MEMBERSHIP_CLAIM = "membership";
    private static final String SINCE_CLAIM = "since";
    private static final String ACTIVE = "active";

    @SuppressWarnings("unchecked")
    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Permission permission, PolicyContext policyContext) {
        if (!operator.equals(Operator.EQ)) {
            policyContext.reportProblem("Invalid operator '%s', only accepts '%s'".formatted(operator, Operator.EQ));
            return false;
        }
        if (!ACTIVE.equals(rightOperand)) {
            policyContext.reportProblem("Right-operand must be equal to '%s', but was '%s'".formatted(ACTIVE, rightOperand));
            return false;
        }

        var pa = policyContext.getContextData(ParticipantAgent.class);
        if (pa == null) {
            policyContext.reportProblem("No ParticipantAgent found on context.");
            return false;
        }
        var credentialResult = getCredentialList(pa);
        if (credentialResult.failed()) {
            policyContext.reportProblem(credentialResult.getFailureDetail());
            return false;
        }

        return credentialResult.getContent()
                .stream()
                .filter(vc -> vc.getType().stream().anyMatch(t -> t.endsWith(MEMBERSHIP_CONSTRAINT_KEY)))
                .flatMap(vc -> vc.getCredentialSubject().stream().filter(cs -> cs.getClaims().containsKey(MEMBERSHIP_CLAIM)))
                .anyMatch(credential -> {
                    var membershipClaim = (Map<String, ?>) credential.getClaim(MVD_NAMESPACE, MEMBERSHIP_CLAIM);
                    var membershipStartDate = Instant.parse(membershipClaim.get(SINCE_CLAIM).toString());
                    return membershipStartDate.isBefore(Instant.now());
                });
    }
}
