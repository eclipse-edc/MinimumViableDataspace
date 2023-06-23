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

import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.system.tests.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static java.lang.String.format;

@EndToEndTest
@EnabledIfEnvironmentVariable(named = "TEST_ENVIRONMENT", matches = "cloud")
class CloudBlobTransferIntegrationTest extends AbstractBlobTransferTest {
    private static final String DST_KEY_VAULT_NAME = TestUtils.requiredPropOrEnv("consumer.eu.key.vault", null);
    private static final String BLOB_STORE_ENDPOINT_TEMPLATE = "https://%s.blob.core.windows.net";
    private static final String KEY_VAULT_ENDPOINT_TEMPLATE = "https://%s.vault.azure.net";

    @Test
    void transferBlob_success() {
        var blobAccountDetails = blobAccount(DST_KEY_VAULT_NAME);
        var storageAccountName = blobAccountDetails.get(0);
        var storageAccountKey = blobAccountDetails.get(1);
        var dstBlobServiceClient = getBlobServiceClient(
                format(BLOB_STORE_ENDPOINT_TEMPLATE, storageAccountName),
                storageAccountName,
                storageAccountKey
        );

        initiateTransfer(dstBlobServiceClient);
    }

    /**
     * Provides Blob storage account name and key.
     *
     * @param keyVaultName Key Vault name. This key vault must have storage account key secrets.
     * @return storage account name and account key on first and second position of list.
     */
    private List<String> blobAccount(String keyVaultName) {
        // Not using DefaultAzureCredentialBuilder because of agent issue https://github.com/orgs/github-community/discussions/20830
        var credential = new AzureCliCredentialBuilder().build();
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

        return List.of(accountName, accountKey.getValue());
    }

}
