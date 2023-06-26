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

import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.Simulation;
import org.eclipse.edc.system.tests.utils.TestUtils;
import org.eclipse.edc.system.tests.utils.TransferRequestFactory;
import org.eclipse.edc.system.tests.utils.TransferSimulationUtils;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static org.eclipse.edc.util.configuration.ConfigurationFunctions.propOrEnv;

/**
 * Runs a single iteration of contract negotiation and file transfer, getting settings from
 * {@see FileTransferIntegrationTest}.
 */
public class TransferLocalSimulation extends Simulation {

    public static final String API_KEY_HEADER = "x-api-key";
    public static final String API_KEY = TestUtils.requiredPropOrEnv("API_KEY", "ApiKeyDefaultValue");
    public static final String CONSUMER_MANAGEMENT_URL = TestUtils.requiredPropOrEnv("CONSUMER_MANAGEMENT_URL", "http://localhost:9192") + "/api/management";
    public static final String CONSUMER_ID = "did:web:did-server:company2";
    public static final String PROVIDER_ID = "did:web:did-server:company1";
    public static final String PROVIDER_DSP_URL = TestUtils.requiredPropOrEnv("PROVIDER_IDS_URL", "http://company1:8282/api/dsp");
    private static final int REPEAT = Integer.parseInt(propOrEnv("repeat", "1"));
    private static final int AT_ONCE_USERS = Integer.parseInt(propOrEnv("at.once.users", "1"));
    private static final int MAX_RESPONSE_TIME = Integer.parseInt(propOrEnv("max.response.time", "5000"));
    private static final double SUCCESS_PERCENTAGE = Double.parseDouble(propOrEnv("success.percentage", "100.0"));

    public TransferLocalSimulation(TransferRequestFactory requestFactory) {
        var httpProtocol = http
                .baseUrl(CONSUMER_MANAGEMENT_URL)
                .header(API_KEY_HEADER, s -> API_KEY); // use the Function form to avoid special characters to be interpreted as Gatling Expression Language
        setUp(scenario(TransferSimulationUtils.DESCRIPTION)
                .repeat(REPEAT)
                .on(TransferSimulationUtils.contractNegotiationAndTransfer(PROVIDER_DSP_URL, requestFactory))
                .injectOpen(atOnceUsers(AT_ONCE_USERS)))
                .protocols(httpProtocol)
                .assertions(
                        CoreDsl.details(TransferSimulationUtils.TRANSFER_SUCCESSFUL).successfulRequests().count().is((long) AT_ONCE_USERS * REPEAT),
                        global().responseTime().max().lt(MAX_RESPONSE_TIME),
                        global().successfulRequests().percent().is(SUCCESS_PERCENTAGE)
                );
    }
}
