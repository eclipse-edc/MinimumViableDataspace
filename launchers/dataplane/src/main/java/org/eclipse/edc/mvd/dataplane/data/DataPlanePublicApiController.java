/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.mvd.dataplane.data;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import okhttp3.Request;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;

/**
 * This controller serves as the public endpoint for the data plane. Its endpoints are intended for consumers to download data
 */
@Path("/data")
public class DataPlanePublicApiController {

    private final EdcHttpClient client;
    private final Monitor monitor;

    public DataPlanePublicApiController(EdcHttpClient client, Monitor monitor) {
        this.client = client;
        this.monitor = monitor;
    }

    @GET
    @Path("/source")
    public String dataSource(@QueryParam("resource") String resource, @QueryParam("id") String id) {
        // Use default values if parameters are not provided
        if (resource == null || resource.isEmpty()) {
            resource = "posts";
        }

        monitor.info("Data requested for resource: " + resource + ", id: " + id);

        var formatted = "https://jsonplaceholder.typicode.com/%s".formatted(resource);
        if (id != null && !id.isEmpty()) {
            formatted += "/%s".formatted(id);
        }
        var request = new Request.Builder()
                .url(formatted)
                .get()
                .build();
        try (var response = client.execute(request)) {
            if (response.isSuccessful()) {
                return response.body().string();
            }
            throw new IllegalArgumentException("Failed to fetch data from typicode: %d".formatted(response.code()));
        } catch (Exception e) {
            monitor.severe("Error fetching data from typicode", e);
            return "error: " + e.getMessage();
        }
    }
}
