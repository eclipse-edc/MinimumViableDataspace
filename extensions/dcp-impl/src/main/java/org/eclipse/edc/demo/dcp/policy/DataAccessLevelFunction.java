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
import java.util.Objects;

public class DataAccessLevelFunction implements AtomicConstraintFunction<Duty> {

    private final String level;

    public DataAccessLevelFunction(String level) {
        this.level = level;
    }

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Duty duty, PolicyContext policyContext) {
        if (!operator.equals(Operator.EQ)) {
            policyContext.reportProblem("Cannot evaluate operator %s, only %s is supported".formatted(operator, Operator.EQ));
            return false;
        }
        if (!"level".equalsIgnoreCase(rightOperand.toString())) {
            policyContext.reportProblem("Data access credentials only support right operand 'level', but found '%s'".formatted(operator.toString()));
            return false;
        }
        var pa = policyContext.getContextData(ParticipantAgent.class);
        if (pa == null) {
            policyContext.reportProblem("ParticipantAgent not found on PolicyContext");
            return false;
        }

        var claims = pa.getClaims();

        String version = getClaim("contractVersion", claims);
        String level = getClaim("level", claims);

        return version != null && Objects.equals(level, rightOperand);
    }

    public String key() {
        return "DataAccess.level";
    }

    @SuppressWarnings("unchecked")
    private <T> T getClaim(String postfix, Map<String, Object> claims) {
        return (T) claims.entrySet().stream().filter(e -> e.getKey().endsWith(postfix))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
    }
}
