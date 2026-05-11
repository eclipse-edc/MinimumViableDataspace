/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.mvd.dataplane.signaling;

import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import org.eclipse.dataplane.Dataplane;
import org.eclipse.dataplane.domain.DataAddress;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlow;
import org.eclipse.dataplane.domain.registration.Authorization;
import org.eclipse.dataplane.domain.registration.Oauth2ClientCredentialsAuthorization;
import org.eclipse.dataplane.logic.OnPrepare;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.mvd.dataplane.data.DataPlanePublicApiController;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.util.Collections.emptyList;

@Extension(value = SignalingDataPlaneRuntimeExtension.NAME)
public class SignalingDataPlaneRuntimeExtension implements ServiceExtension {

    public static final String NAME = "Signaling pata plane Extension";
    @Setting(key = "dataplane.id")
    private String dataplaneId;
    @Configuration
    private SignalingApiConfig signalingApiConfig;

    @Configuration
    private PublicApiConfig publicApiConfig;

    @Inject
    private WebService webService;
    @Inject
    private Monitor monitor;

    private Dataplane dataplane;
    @Inject
    private PortMappingRegistry portMappingRegistry;

    @Inject
    private Hostname hostname;

    @Inject
    private EdcHttpClient httpClient;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var builder = Dataplane.newInstance()
                .id(dataplaneId)
                .registerAuthorization(new Oauth2ClientCredentialsAuthorization())
                .registerAuthorization(new Authorization() {
                    @Override
                    public String type() {
                        return "none";
                    }

                    @Override
                    public Result<String> authorizationHeader(org.eclipse.dataplane.domain.registration.AuthorizationProfile profile) {
                        return Result.success("Bearer dummy-token");
                    }

                    @Override
                    public Result<String> extractCallerId(String authorizationHeader) {
                        return Result.success("anonymous");
                    }
                })
                .endpoint(signalingApiConfig.dataFlowEndpoint(hostname.get()))
                .transferType("HttpData-PULL")
                .onPrepare(new DataplaneOnPrepare())
                .onStart(this::startDataFlow)
                .onStarted(this::receiveDataFlow)
                .onSuspend(this::stopDataFlow)
                .onResume(flow -> flow.getType() == DataFlow.Type.PROVIDER
                        ? startDataFlow(flow)
                        : receiveDataFlow(flow))
                .onCompleted(Result::success)
                .onTerminate(this::stopDataFlow);

        dataplane = builder.build();

        var portMapping = new PortMapping(ApiContext.SIGNALING, signalingApiConfig.port(), signalingApiConfig.path());
        portMappingRegistry.register(portMapping);

        var publicPortMapping = new PortMapping("public", publicApiConfig.port(), publicApiConfig.path());
        portMappingRegistry.register(publicPortMapping);

        //        var typeTransformerRegistry = transformerRegistry.forContext("signaling-api");
        //        typeTransformerRegistry.register(new DataAddressToDspDataAddressTransformer());
        //        typeTransformerRegistry.register(new DataFlowStatusMessageToDataFlowResponseTransformer());
        //        typeTransformerRegistry.register(new DspDataAddressToDataAddressTransformer());

        webService.registerResource(ApiContext.SIGNALING, dataplane.controller());
        webService.registerResource(ApiContext.SIGNALING, dataplane.registrationController());
        webService.registerResource("public", new DataPlanePublicApiController(httpClient, monitor));
        // webService.registerResource(new ControlController(monitor, dataplane, apiConfiguration));
    }

    private @NotNull Result<DataFlow> startDataFlow(DataFlow dataFlow) {
        switch (dataFlow.getTransferType()) {
            case "NonFinite-PULL", "Finite-PULL", "HttpData-PULL" -> {
                var dataAddress = new DataAddress(dataFlow.getTransferType(), "http", publicApiConfig.dataSourceEndpoint(hostname.get()), emptyList());
                dataFlow.setDataAddress(dataAddress);
                return Result.success(dataFlow);
            }
            default -> {
                return Result.failure(new RuntimeException("TransferType %s not supported".formatted(dataFlow.getTransferType())));
            }
        }
    }

    private Result<DataFlow> receiveDataFlow(DataFlow dataFlow) {
        if (dataFlow.getTransferType().equals("HttpData-PULL")) {
            var sourceUri = URI.create(dataFlow.getDataAddress().endpoint());
            requestData(dataFlow, sourceUri)
                    .whenComplete((response, throwable) -> notifyCompletion(dataFlow, response, throwable));
        } else {
            monitor.warning("TransferType %s not supported".formatted(dataFlow.getTransferType()));
        }

        return Result.success(dataFlow);
    }

    private CompletableFuture<HttpResponse<String>> requestData(DataFlow dataFlow, URI sourceUri) {
        var request = HttpRequest.newBuilder(sourceUri).GET().build();
        return HttpClient.newHttpClient().sendAsync(request, ofString()).whenComplete((response, throwable) -> {
            if (throwable == null) {
                if (response.statusCode() == 200) {
                    var body = response.body();
                    monitor.info("Received data for data flow %s: %s".formatted(dataFlow.getId(), body));
                } else {
                    monitor.severe("Error retrieving data: %s: %s".formatted(response.statusCode(), response.body()));
                }
            } else {
                monitor.severe("Error retrieving data", throwable);
            }
        });
    }

    private static class DataplaneOnPrepare implements OnPrepare {
        @Override
        public Result<DataFlow> action(DataFlow dataFlow) {
            return dataFlow.getTransferType().equals("HttpData-PULL")
                    ? Result.success(dataFlow)
                    : Result.failure(new UnsupportedOperationException("unsupported transfer type: " + dataFlow.getTransferType()));
        }
    }

    private @NotNull Result<DataFlow> stopDataFlow(DataFlow dataFlow) {
        return Result.success(dataFlow);
    }

    private void notifyCompletion(DataFlow dataFlow, HttpResponse<?> response, Throwable throwable) {
        if (throwable == null) {
            var statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                notifyCompleted(dataFlow);
            } else {
                dataplane.notifyErrored(dataFlow.getId(), new RuntimeException("Data source/destination endpoint responded with " + statusCode));
            }
        } else {
            dataplane.notifyErrored(dataFlow.getId(), throwable);
        }
    }

    private void notifyCompleted(DataFlow dataFlow) {
        var retryPolicy = RetryPolicy.builder().withMaxRetries(5).withDelay(Duration.ofSeconds(1)).build();
        Failsafe.with(retryPolicy).run(context -> {
            var notifyCompleted = dataplane.notifyCompleted(dataFlow.getId());
            if (notifyCompleted.failed()) {
                throw new RuntimeException("Notify Completed failed: " + notifyCompleted);
            }
        });
    }

    @Settings
    public record SignalingApiConfig(
            @Setting(key = "web.http." + ApiContext.SIGNALING + ".path") String path,
            @Setting(key = "web.http." + ApiContext.SIGNALING + ".port") int port
    ) {

        public URI dataFlowEndpoint(String hostname) {
            return URI.create("http://%s:%d%s/v1/dataflows".formatted(hostname, port, path));
        }
    }

    @Settings
    public record PublicApiConfig(
            @Setting(key = "web.http.public.path", defaultValue = "/api/public") String path,
            @Setting(key = "web.http.public.port", defaultValue = "11002") int port) {
        public String dataSourceEndpoint(String hostname) {
            return "http://%s:%d%s/data/source".formatted(hostname, port, path);
        }
    }
}
