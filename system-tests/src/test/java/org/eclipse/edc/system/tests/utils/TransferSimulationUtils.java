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

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.doWhileDuring;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;
import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.system.tests.local.TransferLocalSimulation.CONSUMER_ID;
import static org.eclipse.edc.system.tests.local.TransferLocalSimulation.PROVIDER_ID;

/**
 * Utility methods for building a Gatling simulation for performing contract negotiation and file transfer.
 */
public abstract class TransferSimulationUtils {

    private static final TypeManager TYPE_MANAGER = new TypeManager();

    public static final String CONTRACT_NEGOTIATION_ID = "contractNegotiationId";
    public static final String CONTRACT_NEGOTIATION_STATE = "contractNegotiationState";
    public static final String TRANSFER_PROCESS_STATE = "transferProcessState";
    public static final String CONTRACT_AGREEMENT_ID = "contractAgreementId";
    public static final String TRANSFER_PROCESS_ID = "transferProcessId";

    public static final String DESCRIPTION = "[Contract negotiation and file transfer]";

    public static final String PROVIDER_ASSET_FILE = "text-document.txt";
    public static final String TRANSFER_SUCCESSFUL = "Transfer successful";

    // Related to Postman seed data
    public static final ContractId CONTRACT_DEFINITION_ID = ContractId.create("def-test-document_company1", "test-document_company1");

    private TransferSimulationUtils() {
    }

    /**
     * Gatling chain for performing contract negotiation and file transfer.
     *
     * @param providerDspAddress DSP address of the data provider.
     * @param requestFactory     Factory for creating transfer request payloads.
     */
    public static ChainBuilder contractNegotiationAndTransfer(String providerDspAddress, TransferRequestFactory requestFactory) {
        return initiateNegotiation(providerDspAddress)
                .exec(waitForContractNegotiationToBeFinalized())
                .exec(getContractAgreementId())
                .exec(startTransfer(providerDspAddress, requestFactory))
                .exec(waitForTransferProcessToBeCompleted());
    }

    private static ChainBuilder initiateNegotiation(String providerDspUrl) {
        return group("Contract negotiation")
                .on(exec(sendNegotiationRequest(providerDspUrl)));
    }

    @NotNull
    private static HttpRequestActionBuilder sendNegotiationRequest(String providerDspAddress) {
        // TODO: this policy must be retrieve from a call to the catalog
        var policy = Map.of(
                "@context", Map.of("odrl", "http://www.w3.org/ns/odrl/2/"),
                "@id", CONTRACT_DEFINITION_ID.toString(),
                "@type", "odrl:Set",
                "odrl:target", CONTRACT_DEFINITION_ID.assetIdPart()
        );
        var request = Map.of(
                TYPE, EDC_NAMESPACE + "NegotiationInitiateRequestDto",
                EDC_NAMESPACE + "connectorId", PROVIDER_ID,
                EDC_NAMESPACE + "consumerId", CONSUMER_ID,
                EDC_NAMESPACE + "providerId", PROVIDER_ID,
                EDC_NAMESPACE + "connectorAddress", providerDspAddress,
                EDC_NAMESPACE + "protocol", "dataspace-protocol-http",
                EDC_NAMESPACE + "offer", Map.of(
                        EDC_NAMESPACE + "offerId", CONTRACT_DEFINITION_ID.toString(),
                        EDC_NAMESPACE + "assetId", CONTRACT_DEFINITION_ID.assetIdPart(),
                        EDC_NAMESPACE + "policy", policy
                ));

        return http("Initiate contract negotiation")
                .post("/v2/contractnegotiations")
                .body(StringBody(TYPE_MANAGER.writeValueAsString(request)))
                .asJson()
                .check(status().is(200))
                .check(jsonPath("$.@id")
                        .notNull()
                        .saveAs(CONTRACT_NEGOTIATION_ID));
    }

    @NotNull
    private static HttpRequestActionBuilder getContractAgreementId() {
        return http("Get the contract agreement id")
                .get(session -> format("/v2/contractnegotiations/%s", session.getString(CONTRACT_NEGOTIATION_ID)))
                .check(status().is(200))
                .check(jsonPath("$.edc:contractAgreementId").notNull().saveAs(CONTRACT_AGREEMENT_ID));
    }

    private static ChainBuilder startTransfer(String providerDspUrl, TransferRequestFactory requestFactory) {
        return group("Initiate transfer")
                .on(exec(initiateTransfer(requestFactory, providerDspUrl)));
    }

    @NotNull
    private static HttpRequestActionBuilder initiateTransfer(TransferRequestFactory requestFactory, String providerDspUrl) {
        return http("Initiate file transfer")
                .post("/v2/transferprocesses")
                .body(StringBody(session -> requestFactory.apply(new TransferInitiationData(
                        providerDspUrl,
                        CONTRACT_DEFINITION_ID.assetIdPart(),
                        session.getString(CONTRACT_AGREEMENT_ID)))))
                .asJson()
                .check(status().is(200))
                .check(jsonPath("$.@id")
                        .notNull()
                        .saveAs(TRANSFER_PROCESS_ID));
    }

    /**
     * Gatling chain for calling ContractNegotiation status endpoint repeatedly until a FINALIZED state is
     * attained, or a timeout is reached.
     */
    private static ChainBuilder waitForContractNegotiationToBeFinalized() {
        return exec(session -> session.set(CONTRACT_NEGOTIATION_STATE, -1))
                .group("Wait for contract negotiation to be FINALIZED")
                .on(doWhileDuring(TransferSimulationUtils::contractNegotiationNotFinalized, Duration.ofSeconds(30))
                        .on(exec(getContractNegotiationStatus()).pace(Duration.ofSeconds(1)))
                )
                .exitHereIf(TransferSimulationUtils::contractNegotiationNotFinalized);
    }

    /**
     * Gatling chain for calling the transfer status endpoint repeatedly until a COMPLETED state is
     * attained, or a timeout is reached.
     */
    private static ChainBuilder waitForTransferProcessToBeCompleted() {
        return group("Wait for transfer process to be COMPLETED")
                .on(exec(session -> session.set(TRANSFER_PROCESS_STATE, TransferProcessStates.INITIAL))
                        .doWhileDuring(TransferSimulationUtils::transferProcessNotCompleted,
                                Duration.ofSeconds(30))
                        .on(exec(getTransferProcessStatus()).pace(Duration.ofSeconds(1))))
                .exitHereIf(TransferSimulationUtils::transferProcessNotCompleted)
                // Perform one additional request if the transfer successful.
                // This allows running Gatling assertions to validate that the transfer actually succeeded
                // (and timeout was not reached).
                .group(TRANSFER_SUCCESSFUL)
                .on(exec(getTransferProcessStatus()));
    }

    @NotNull
    private static HttpRequestActionBuilder getContractNegotiationStatus() {
        return http("Get status of contract negotiation")
                .get(session -> format("/v2/contractnegotiations/%s/state", session.getString(CONTRACT_NEGOTIATION_ID)))
                .check(status().is(200))
                .check(jsonPath("$.edc:state").notNull().saveAs(CONTRACT_NEGOTIATION_STATE));
    }

    @NotNull
    private static HttpRequestActionBuilder getTransferProcessStatus() {
        return http("Get transfer process status")
                .get(session -> format("/v2/transferprocesses/%s/state", session.getString(TRANSFER_PROCESS_ID)))
                .check(status().is(200))
                .check(jsonPath("$.edc:state").notNull().saveAs(TRANSFER_PROCESS_STATE));
    }

    private static boolean contractNegotiationNotFinalized(Session session) {
        return !ContractNegotiationStates.FINALIZED.name().equalsIgnoreCase(session.getString(CONTRACT_NEGOTIATION_STATE));
    }

    private static boolean transferProcessNotCompleted(Session session) {
        return !TransferProcessStates.COMPLETED.name().equalsIgnoreCase(session.getString(TRANSFER_PROCESS_STATE));
    }
}
