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

import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.RequestScope;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

public class DefaultScopeExtractor implements BiFunction<Policy, PolicyContext, Boolean> {
    private final Set<String> defaultScopes;

    public DefaultScopeExtractor(Set<String> defaultScopes) {
        this.defaultScopes = defaultScopes;
    }

    @Override
    public Boolean apply(Policy policy, PolicyContext policyContext) {
        var requestScopeBuilder = policyContext.getContextData(RequestScope.Builder.class);
        if (requestScopeBuilder == null) {
            throw new EdcException("%s not set in policy context".formatted(RequestScope.Builder.class));
        }
        var rq = requestScopeBuilder.build();
        var existingScope = rq.getScopes();
        var newScopes = new HashSet<>(defaultScopes);
        newScopes.addAll(existingScope);
        requestScopeBuilder.scopes(newScopes);
        return true;
    }
}
