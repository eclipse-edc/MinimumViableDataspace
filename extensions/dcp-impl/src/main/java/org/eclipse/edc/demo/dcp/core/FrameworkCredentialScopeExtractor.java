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

package org.eclipse.edc.demo.dcp.core;

import org.eclipse.edc.iam.identitytrust.spi.scope.ScopeExtractor;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;

import java.util.Set;

class FrameworkCredentialScopeExtractor implements ScopeExtractor {
    private static final String FRAMEWORK_CREDENTIAL_PREFIX = "FrameworkCredential.";
    private static final String CREDENTIAL_TYPE_NAMESPACE = "org.eclipse.edc.vc.type";

    FrameworkCredentialScopeExtractor() {
    }

    @Override
    public Set<String> extractScopes(Object leftValue, Operator operator, Object rightValue, PolicyContext context) {
        Set<String> scopes = Set.of();
        if (leftValue instanceof String leftOperand) {
            if (leftOperand.startsWith(FRAMEWORK_CREDENTIAL_PREFIX)) {
                var credentialType = leftOperand.replace(FRAMEWORK_CREDENTIAL_PREFIX, "");
                credentialType = "%sCredential".formatted(capitalize(credentialType));
                scopes = Set.of("%s:%s:read".formatted(CREDENTIAL_TYPE_NAMESPACE, credentialType));
            }
        }
        return scopes;
    }

    private String capitalize(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
}
