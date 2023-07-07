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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.system.tests.utils;

public class TransferInitiationData {
    private final String providerDspUrl;
    private final String assetId;
    private final String contractAgreementId;

    TransferInitiationData(String providerDspUrl, String assetId, String contractAgreementId) {
        this.providerDspUrl = providerDspUrl;
        this.assetId = assetId;
        this.contractAgreementId = contractAgreementId;
    }

    public String getProviderDspUrl() {
        return providerDspUrl;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getContractAgreementId() {
        return contractAgreementId;
    }
}
