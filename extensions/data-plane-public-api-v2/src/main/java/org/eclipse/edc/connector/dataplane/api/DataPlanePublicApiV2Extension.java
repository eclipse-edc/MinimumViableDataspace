/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.api;

import org.eclipse.edc.connector.dataplane.api.controller.DataPlanePublicApiV2Controller;
import org.eclipse.edc.connector.dataplane.spi.Endpoint;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;

import java.util.concurrent.Executors;

/**
 * This extension provides generic endpoints which are open to public participants of the Dataspace to execute
 * requests on the actual data source.
 */
@Extension(value = DataPlanePublicApiV2Extension.NAME)
public class DataPlanePublicApiV2Extension implements ServiceExtension {
    public static final String NAME = "Data Plane Public API";

    public static final String API_CONTEXT = "public";
    private static final int DEFAULT_PUBLIC_PORT = 8185;
    private static final String DEFAULT_PUBLIC_PATH = "/api/public";
    private static final int DEFAULT_THREAD_POOL = 10;
    @Setting(description = "Base url of the public API endpoint without the trailing slash. This should point to the public endpoint configured.",
            required = false,
            key = "edc.dataplane.api.public.baseurl", warnOnMissingConfig = true)
    private String publicBaseUrl;
    @Setting(description = "Optional base url of the response channel endpoint without the trailing slash. A common practice is to use <PUBLIC_ENDPOINT>/responseChannel", key = "edc.dataplane.api.public.response.baseurl", required = false)
    private String publicApiResponseUrl;
    @Configuration
    private PublicApiConfiguration apiConfiguration;
    @Inject
    private PortMappingRegistry portMappingRegistry;
    @Inject
    private PipelineService pipelineService;
    @Inject
    private WebService webService;
    @Inject
    private ExecutorInstrumentation executorInstrumentation;
    @Inject
    private DataPlaneAuthorizationService authorizationService;
    @Inject
    private PublicEndpointGeneratorService generatorService;
    @Inject
    private Hostname hostname;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.getMonitor().warning("The `data-plane-public-api-v2` has been deprecated, please provide an" +
                "alternative implementation for Http Proxy if needed");

        var portMapping = new PortMapping(API_CONTEXT, apiConfiguration.port(), apiConfiguration.path());
        portMappingRegistry.register(portMapping);
        var executorService = executorInstrumentation.instrument(
                Executors.newFixedThreadPool(DEFAULT_THREAD_POOL),
                "Data plane proxy transfers"
        );

        if (publicBaseUrl == null) {
            publicBaseUrl = "http://%s:%d%s".formatted(hostname.get(), portMapping.port(), portMapping.path());
            context.getMonitor().warning("The public API endpoint was not explicitly configured, the default '%s' will be used.".formatted(publicBaseUrl));
        }
        var endpoint = Endpoint.url(publicBaseUrl);
        generatorService.addGeneratorFunction("HttpData", dataAddress -> endpoint);

        if (publicApiResponseUrl != null) {
            generatorService.addResponseGeneratorFunction("HttpData", () -> Endpoint.url(publicApiResponseUrl));
        }

        var publicApiController = new DataPlanePublicApiV2Controller(pipelineService, executorService, authorizationService);
        webService.registerResource(API_CONTEXT, publicApiController);
    }

    @Settings
    record PublicApiConfiguration(
            @Setting(key = "web.http." + API_CONTEXT + ".port", description = "Port for " + API_CONTEXT + " api context", defaultValue = DEFAULT_PUBLIC_PORT + "")
            int port,
            @Setting(key = "web.http." + API_CONTEXT + ".path", description = "Path for " + API_CONTEXT + " api context", defaultValue = DEFAULT_PUBLIC_PATH)
            String path
    ) {

    }
}
