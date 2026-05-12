/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.mvd.dataplane.signaling;

import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.registration.Authorization;

/**
 * NOOP authorization between control plane and data plane of a single participant.
 */
public class NoneAuthorization implements Authorization {
    @Override
    public String type() {
        return "none";
    }

    @Override
    public Result<String> authorizationHeader(org.eclipse.dataplane.domain.registration.AuthorizationProfile profile) {
        return Result.success("Bearer dummy-token");
    }

    @Override
    public Result<String> extractCallerId(String authorizationHeader) {
        return Result.success("anonymous");
    }
}
