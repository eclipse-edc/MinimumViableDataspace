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

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobServiceClient;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.system.tests.utils.TransferSimulationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

@EndToEndTest
@EnabledIfEnvironmentVariable(named = "TEST_ENVIRONMENT", matches = "local")
public class BlobTransferIntegrationTest extends AbstractBlobTransferTest {

    private static final String PROVIDER_CONTAINER_NAME = "src-container";
    public static final String LOCAL_BLOB_STORE_ENDPOINT_TEMPLATE = "http://127.0.0.1:10000/%s";
    public static final String LOCAL_SOURCE_BLOB_STORE_ACCOUNT = "company1assets";
    public static final String LOCAL_SOURCE_BLOB_STORE_ACCOUNT_KEY = "key1";
    public static final String LOCAL_DESTINATION_BLOB_STORE_ACCOUNT = "company2assets";
    public static final String LOCAL_DESTINATION_BLOB_STORE_ACCOUNT_KEY = "key2";

    private final List<Runnable> containerCleanup = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        var srcBlobServiceClient = getBlobServiceClient(
                format(LOCAL_BLOB_STORE_ENDPOINT_TEMPLATE, LOCAL_SOURCE_BLOB_STORE_ACCOUNT),
                LOCAL_SOURCE_BLOB_STORE_ACCOUNT,
                LOCAL_SOURCE_BLOB_STORE_ACCOUNT_KEY
        );
        // Upload a blob with test data on provider blob container.
        createContainer(srcBlobServiceClient, PROVIDER_CONTAINER_NAME);
        srcBlobServiceClient.getBlobContainerClient(PROVIDER_CONTAINER_NAME)
                .getBlobClient(TransferSimulationUtils.PROVIDER_ASSET_FILE)
                .upload(BinaryData.fromString(UUID.randomUUID().toString()), true);
    }

    @AfterEach
    public void teardown() {
        containerCleanup.parallelStream().forEach(Runnable::run);
    }

    @Test
    void transferBlob_success() {
        var dstBlobServiceClient = getBlobServiceClient(
                format(LOCAL_BLOB_STORE_ENDPOINT_TEMPLATE, LOCAL_DESTINATION_BLOB_STORE_ACCOUNT),
                LOCAL_DESTINATION_BLOB_STORE_ACCOUNT,
                LOCAL_DESTINATION_BLOB_STORE_ACCOUNT_KEY
        );

        initiateTransfer(dstBlobServiceClient);
    }


    private void createContainer(BlobServiceClient client, String containerName) {
        assertThat(client.getBlobContainerClient(containerName).exists()).isFalse();

        var blobContainerClient = client.createBlobContainer(containerName);
        assertThat(blobContainerClient.exists()).isTrue();
        containerCleanup.add(() -> client.deleteBlobContainer(containerName));
    }

}
