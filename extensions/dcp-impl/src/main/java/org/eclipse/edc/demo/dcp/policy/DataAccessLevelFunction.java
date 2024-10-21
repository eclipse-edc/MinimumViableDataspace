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

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Operator;

import java.util.Objects;

public class DataAccessLevelFunction<C extends ParticipantAgentPolicyContext> extends AbstractCredentialEvaluationFunction implements AtomicConstraintRuleFunction<Duty, C> {

    private static final String DATAPROCESSOR_CRED_TYPE = "DataProcessorCredential";

    private DataAccessLevelFunction() {

    }

    public static <C extends ParticipantAgentPolicyContext> DataAccessLevelFunction<C> create() {
        return new DataAccessLevelFunction<>() {
        };
    }

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Duty duty, C policyContext) {
        if (!operator.equals(Operator.EQ)) {
            policyContext.reportProblem("Cannot evaluate operator %s, only %s is supported".formatted(operator, Operator.EQ));
            return false;
        }
        var pa = policyContext.participantAgent();
        if (pa == null) {
            policyContext.reportProblem("ParticipantAgent not found on PolicyContext");
            return false;
        }

        var credentialResult = getCredentialList(pa);
        if (credentialResult.failed()) {
            policyContext.reportProblem(credentialResult.getFailureDetail());
            return false;
        }

        return credentialResult.getContent()
                .stream()
                .filter(vc -> vc.getType().stream().anyMatch(t -> t.endsWith(DATAPROCESSOR_CRED_TYPE)))
                .flatMap(credential -> credential.getCredentialSubject().stream())
                .anyMatch(credentialSubject -> {
                    var version = credentialSubject.getClaim(MVD_NAMESPACE, "contractVersion");
                    var level = credentialSubject.getClaim(MVD_NAMESPACE, "level");

                    return version != null && Objects.equals(level, rightOperand);
                });


    }

}
