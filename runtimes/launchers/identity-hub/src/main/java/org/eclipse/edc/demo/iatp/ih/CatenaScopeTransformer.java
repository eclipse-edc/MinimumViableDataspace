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

package org.eclipse.edc.demo.iatp.ih;

import org.eclipse.edc.identityhub.defaults.EdcScopeToCriterionTransformer;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;

import java.util.List;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

public class CatenaScopeTransformer extends EdcScopeToCriterionTransformer {

    private final List<String> knownCredentialTypes;

    public CatenaScopeTransformer(List<String> knownCredentialTypes) {
        this.knownCredentialTypes = knownCredentialTypes;
    }

    @Override
    public Result<Criterion> transform(String scope) {
        var tokens = tokenize(scope);
        if (tokens.failed()) {
            return failure("Scope string cannot be converted: %s".formatted(tokens.getFailureDetail()));
        }
        var credentialType = tokens.getContent()[1];

        if (!knownCredentialTypes.contains(credentialType)) {
            //select based on the credentialSubject.useCaseType property
            // even though "claims" is a Map, we need to access it using the dot notation. See ReflectionUtil.java
            return success(new Criterion("verifiableCredential.credential.credentialSubject.claims.useCaseType", "=", credentialType));
        } else {
            return success(new Criterion(TYPE_OPERAND, CONTAINS_OPERATOR, credentialType));
        }
    }
}
