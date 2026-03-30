/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.issuerservice.seed.attestation.membership;

import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSource;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactory;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;

public class MembershipAttestationSourceFactory implements AttestationSourceFactory {
    @Override
    public AttestationSource createSource(AttestationDefinition definition) {
        var config = definition.getConfiguration();
        return new MembershipAttestationSource();
    }

}
