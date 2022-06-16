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

package org.eclipse.dataspaceconnector.mvd;

import com.fasterxml.jackson.core.type.TypeReference;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.io.IOException;
import java.util.Map;

public class MockCredentialsVerifier implements CredentialsVerifier {
    private final Monitor monitor;
    private final OkHttpClient httpClient;
    private final TypeManager typeManager;

    public MockCredentialsVerifier(Monitor monitor, OkHttpClient httpClient, TypeManager typeManager) {
        this.monitor = monitor;
        this.httpClient = httpClient;
        this.typeManager = typeManager;
    }

    /**
     * Fetch the provided url in order to retrieve the seld-description document of the participant.
     *
     * @param hubBaseUrl      this url corresponds to the http address of the SDD document.
     * @param othersPublicKey the hub's public key to encrypt messages with
     * @return Participant self-description.
     */
    @Override
    public Result<Map<String, Object>> verifyCredentials(String hubBaseUrl, PublicKeyWrapper othersPublicKey) {
        monitor.debug("Starting (mock) retrieval of self-description against " + hubBaseUrl);

        var request = new Request.Builder().url(hubBaseUrl).get().build();
        try (var response = httpClient.newCall(request).execute()) {
            String stringBody = null;
            var body = response.body();
            if (body != null) {
                stringBody = body.string();
            }
            if (response.isSuccessful()) {
                if (stringBody == null) {
                    throw new EdcException("Received null body");
                }

                var tr = new TypeReference<Map<String, Object>>() {
                };
                return Result.success(typeManager.getMapper().readValue(stringBody, tr));
            } else {
                var errorMsg = String.format("Failed to retrieve self-description: %s - %s. %s", response.code(), response.message(), stringBody);
                monitor.severe(errorMsg);
                return Result.failure(errorMsg);
            }
        } catch (IOException e) {
            throw new EdcException("Call to self-description server failed: " + e);
        }
    }
}
