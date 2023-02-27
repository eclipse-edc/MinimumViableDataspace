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

import org.eclipse.edc.identityhub.spi.credentials.model.Credential;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RegionConstraintFunction implements AtomicConstraintFunction<Permission> {
    
    private static final String REGION_KEY = "region";
    private final Monitor monitor;

    public RegionConstraintFunction(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, PolicyContext context) {
        var regions = getRegions(context.getParticipantAgent().getClaims());
        switch (operator) {
            case EQ:
                return regions.contains(rightValue);
            case NEQ:
                return !regions.contains(rightValue);
            case IN:
                return !Collections.disjoint((Collection<?>) rightValue, regions);
            default:
                return false;
        }
    }

    private List<String> getRegions(Map<String, Object> claims) {
        List<String> regions = new ArrayList<>();
        var verifiableCredentials = claims.values();
        for (Object vc : verifiableCredentials) {
            try {
                var region = getRegion(vc);
                regions.add(region);
            } catch (ClassCastException | IllegalArgumentException e) {
                monitor.warning("Failed getting region from verifiableCredential", e);
            }
        }
        return regions;
    }

    private String getRegion(Object object) {
        var credential = (Credential) object;
        var claims = credential.getCredentialSubject().getClaims();
        return (String) claims.get(REGION_KEY);
    }
}
