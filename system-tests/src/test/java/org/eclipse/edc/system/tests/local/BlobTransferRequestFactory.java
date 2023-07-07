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

package org.eclipse.edc.system.tests.local;

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.system.tests.utils.TransferInitiationData;
import org.eclipse.edc.system.tests.utils.TransferRequestFactory;

import java.util.Map;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.system.tests.local.TransferLocalSimulation.PROVIDER_ID;


public class BlobTransferRequestFactory implements TransferRequestFactory {

    private final String accountName;

    public BlobTransferRequestFactory(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public String apply(TransferInitiationData transferInitiationData) {
        var destination = Map.of(
                TYPE, EDC_NAMESPACE + "DataAddress",
                EDC_NAMESPACE + "type", AzureBlobStoreSchema.TYPE,
                EDC_NAMESPACE + "properties", Map.of(
                        AzureBlobStoreSchema.ACCOUNT_NAME, accountName
                )
        );

        var request = Map.of(
                CONTEXT, Map.of(EDC_PREFIX, EDC_NAMESPACE),
                TYPE, "TransferRequestDto",
                "dataDestination", destination,
                "protocol", "dataspace-protocol-http",
                "assetId", transferInitiationData.getAssetId(),
                "contractId", transferInitiationData.getContractAgreementId(),
                "connectorAddress", transferInitiationData.getProviderDspUrl(),
                "connectorId", PROVIDER_ID
        );

        return new TypeManager().writeValueAsString(request);
    }
}
