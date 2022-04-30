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

package org.eclipse.dataspaceconnector.system.tests.local;

import io.gatling.javaapi.core.Simulation;
import org.eclipse.dataspaceconnector.system.tests.utils.TransferRequestFactory;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;
import static org.eclipse.dataspaceconnector.system.tests.utils.TestUtils.requiredPropOrEnv;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.DESCRIPTION;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.TRANSFER_SUCCESSFUL;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.contractNegotiationAndTransfer;

/**
 * Runs a single iteration of contract negotiation and file transfer, getting settings from
 * {@see FileTransferIntegrationTest}.
 */
public class TransferLocalSimulation extends Simulation {
    public static final String CONSUMER_MANAGEMENT_PATH = "/api/v1/data";

    private static final int REPEAT = Integer.parseInt(propOrEnv("repeat", "1"));
    private static final int AT_ONCE_USERS = Integer.parseInt(propOrEnv("at.once.users", "1"));
    private static final int MAX_RESPONSE_TIME = Integer.parseInt(propOrEnv("max.response.time", "5000"));
    private static final double SUCCESS_PERCENTAGE = Double.parseDouble(propOrEnv("success.percentage", "100.0"));

    public TransferLocalSimulation(TransferRequestFactory requestFactory) {
        setUp(scenario(DESCRIPTION)
                .repeat(REPEAT)
                .on(contractNegotiationAndTransfer(requiredPropOrEnv("PROVIDER_IDS_URL"), requestFactory))
                .injectOpen(atOnceUsers(AT_ONCE_USERS)))
                .protocols(http.baseUrl(requiredPropOrEnv("CONSUMER_MANAGEMENT_URL") + "/" + CONSUMER_MANAGEMENT_PATH))
                .assertions(
                        details(TRANSFER_SUCCESSFUL).successfulRequests().count().is((long) AT_ONCE_USERS * REPEAT),
                        global().responseTime().max().lt(MAX_RESPONSE_TIME),
                        global().successfulRequests().percent().is(SUCCESS_PERCENTAGE)
                );
    }
}
