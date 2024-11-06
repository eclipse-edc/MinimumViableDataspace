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

package org.eclipse.edc.demo.dcp.ih;

import org.eclipse.edc.identityhub.query.EdcScopeToCriterionTransformer;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;

import java.util.List;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

public class MvdScopeTransformer extends EdcScopeToCriterionTransformer {

    private final List<String> knownCredentialTypes;

    public MvdScopeTransformer(List<String> knownCredentialTypes) {
        this.knownCredentialTypes = knownCredentialTypes;
    }

    @Override
    public Result<Criterion> transform(String scope) {
        var tokens = tokenize(scope);
        if (tokens.failed()) {
            return failure("Scope string cannot be converted: %s".formatted(tokens.getFailureDetail()));
        }
        var credentialType = tokens.getContent()[1];

        if (knownCredentialTypes.contains(credentialType)) {
            return success(new Criterion(TYPE_OPERAND, CONTAINS_OPERATOR, credentialType));
        }

        return failure("Unknown credential type: %s".formatted(credentialType));
    }
}
