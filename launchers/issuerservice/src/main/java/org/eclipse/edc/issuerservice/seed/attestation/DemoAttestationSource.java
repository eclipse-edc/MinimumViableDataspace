/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.seed.attestation;

import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationContext;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSource;
import org.eclipse.edc.spi.result.Result;

import java.util.Map;

public class DemoAttestationSource implements AttestationSource {
    @Override
    public Result<Map<String, Object>> execute(AttestationContext attestationContext) {
        return Result.success(Map.of("onboarding", Map.of("signedDocuments", true), "participant", Map.of("name", "Alice")));
    }
}
