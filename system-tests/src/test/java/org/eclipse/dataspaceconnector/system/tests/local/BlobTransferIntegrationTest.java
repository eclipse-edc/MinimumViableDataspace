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

package org.eclipse.dataspaceconnector.system.tests.local;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import io.restassured.response.ResponseBodyExtractionOptions;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.system.tests.local.BlobTransferLocalSimulation.ACCOUNT_NAME_PROPERTY;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.API_KEY;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.API_KEY_HEADER;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_MANAGEMENT_URL;
import static org.eclipse.dataspaceconnector.system.tests.utils.GatlingUtils.runGatling;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.PROVIDER_ASSET_FILE;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.TRANSFER_PROCESSES_PATH;

public class BlobTransferIntegrationTest {
    public static final String DST_KEY_VAULT_NAME = getEnv("CONSUMER_KEY_VAULT");
    public static final String BLOB_STORE_ENDPOINT_TEMPLATE = "https://%s.blob.core.windows.net";
    public static final String KEY_VAULT_ENDPOINT_TEMPLATE = "https://%s.vault.azure.net";

    @Test
    public void transferBlob_success() {
        BlobServiceClient blobServiceClient2 = getBlobServiceClient(DST_KEY_VAULT_NAME);

        // Act
        System.setProperty(ACCOUNT_NAME_PROPERTY, blobServiceClient2.getAccountName());
        runGatling(BlobTransferLocalSimulation.class, TransferSimulationUtils.DESCRIPTION);

        // Assert
        var container = getProvisionedContainerName();
        var destinationBlob = blobServiceClient2.getBlobContainerClient(container)
                .getBlobClient(PROVIDER_ASSET_FILE);
        assertThat(destinationBlob.exists())
                .withFailMessage("Destination blob %s not created", destinationBlob.getBlobUrl())
                .isTrue();
    }

    @NotNull
    private BlobServiceClient getBlobServiceClient(String keyVaultName) {
        var credential = new DefaultAzureCredentialBuilder().build();
        var vault = new SecretClientBuilder()
                .vaultUrl(format(KEY_VAULT_ENDPOINT_TEMPLATE, keyVaultName))
                .credential(credential)
                .buildClient();
        // Find the first account with a key in the key vault
        var accountKeySecret = vault.listPropertiesOfSecrets().stream().filter(s -> s.getName().endsWith("-key1")).findFirst().orElseThrow(
                () -> new AssertionError("Key vault " + keyVaultName + " should contain the storage account key")
        );
        var accountKey = vault.getSecret(accountKeySecret.getName());
        var accountName = accountKeySecret.getName().replaceFirst("-key1$", "");
        var blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(format(BLOB_STORE_ENDPOINT_TEMPLATE, accountName))
                .credential(new StorageSharedKeyCredential(accountName, accountKey.getValue()))
                .buildClient();
        return blobServiceClient;
    }

    private String getProvisionedContainerName() {
        ResponseBodyExtractionOptions body = given()
                .baseUri(CONSUMER_MANAGEMENT_URL)
                .header(API_KEY_HEADER, API_KEY)
                .when()
                .get(TRANSFER_PROCESSES_PATH)
                .then()
                .statusCode(200)
                .extract().body();
        return body
                .jsonPath().getString("[0].dataDestination.properties.container");
    }

    private static String getEnv(String key) {
        return Objects.requireNonNull(StringUtils.trimToNull(System.getenv(key)), key);
    }
}
