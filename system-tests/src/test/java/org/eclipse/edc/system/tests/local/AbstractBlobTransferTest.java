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
 *       ZF Friedrichshafen AG - add management api configurations
 *       Fraunhofer Institute for Software and Systems Engineering - added IDS API context
 *
 */

package org.eclipse.edc.system.tests.local;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import io.restassured.http.ContentType;
import org.eclipse.edc.system.tests.utils.TransferSimulationUtils;
import org.jetbrains.annotations.NotNull;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.system.tests.local.BlobTransferLocalSimulation.ACCOUNT_NAME_PROPERTY;
import static org.eclipse.edc.system.tests.local.TransferLocalSimulation.API_KEY;
import static org.eclipse.edc.system.tests.local.TransferLocalSimulation.API_KEY_HEADER;
import static org.eclipse.edc.system.tests.local.TransferLocalSimulation.CONSUMER_MANAGEMENT_URL;
import static org.eclipse.edc.system.tests.utils.GatlingUtils.runGatling;
import static org.eclipse.edc.system.tests.utils.TransferSimulationUtils.PROVIDER_ASSET_FILE;

public abstract class AbstractBlobTransferTest {
    
    protected void initiateTransfer(BlobServiceClient dstBlobServiceClient) {
        // Act
        System.setProperty(ACCOUNT_NAME_PROPERTY, dstBlobServiceClient.getAccountName());
        runGatling(BlobTransferLocalSimulation.class, TransferSimulationUtils.DESCRIPTION);

        // Assert
        var container = getProvisionedContainerName();
        var destinationBlob = dstBlobServiceClient.getBlobContainerClient(container)
                .getBlobClient(PROVIDER_ASSET_FILE);
        assertThat(destinationBlob.exists())
                .withFailMessage("Destination blob %s not created", destinationBlob.getBlobUrl())
                .isTrue();
    }

    @NotNull
    protected BlobServiceClient getBlobServiceClient(String endpoint, String accountName, String accountKey) {
        return new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .credential(new StorageSharedKeyCredential(accountName, accountKey))
                .buildClient();
    }

    private String getProvisionedContainerName() {
        var body = given()
                .baseUri(CONSUMER_MANAGEMENT_URL)
                .header(API_KEY_HEADER, API_KEY)
                .when()
                .contentType(ContentType.JSON)
                .post("/v2/transferprocesses/request")
                .then()
                .statusCode(200)
                .extract().body();
        return body.jsonPath().getString("[0].'edc:dataDestination'.'edc:container'");
    }
}
