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
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.agent.ParticipantAgent;

import java.util.Map;

public class UseCaseFunction implements AtomicConstraintFunction<Duty> {

    private final String usecase;

    public UseCaseFunction(String usecase) {
        this.usecase = usecase;
    }

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Duty duty, PolicyContext policyContext) {
        if (!operator.equals(Operator.EQ)) {
            policyContext.reportProblem("Cannot evaluate operator %s, only %s is supported".formatted(operator, Operator.EQ));
            return false;
        }
        if (!"active".equalsIgnoreCase(rightOperand.toString())) {
            policyContext.reportProblem("Use case credentials only support right operand 'active', but found '%s'".formatted(operator.toString()));
            return false;
        }
        var pa = policyContext.getContextData(ParticipantAgent.class);
        if (pa == null) {
            policyContext.reportProblem("ParticipantAgent not found on PolicyContext");
            return false;
        }

        var claims = pa.getClaims();

        String version = getClaim("contractVersion", claims);
        String holderIdentifier = getClaim("holderIdentifier", claims);
        String contractTemplate = getClaim("contractTemplate", claims);

        return version != null && holderIdentifier != null && contractTemplate != null &&
                contractTemplate.contains(usecase);
    }

    public String key() {
        return "FrameworkCredential.%s".formatted(usecase);
    }

    @SuppressWarnings("unchecked")
    private <T> T getClaim(String postfix, Map<String, Object> claims) {
        return (T) claims.entrySet().stream().filter(e -> e.getKey().endsWith(postfix))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
    }
}
