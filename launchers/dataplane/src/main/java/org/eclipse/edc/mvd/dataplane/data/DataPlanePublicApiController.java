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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import okhttp3.Request;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

/**
 * This controller serves as the public endpoint for the data plane. Its endpoints are intended for consumers to download data
 */
@Path("/data")
public class DataPlanePublicApiController {

    private final EdcHttpClient client;
    private final Monitor monitor;
    private final String expectedAuthHeader;

    public DataPlanePublicApiController(EdcHttpClient client, Monitor monitor, String expectedAuthHeader) {
        this.client = client;
        this.monitor = monitor;
        this.expectedAuthHeader = expectedAuthHeader;
    }

    @GET
    @Path("/source")
    public Response dataSource(@Context HttpHeaders headers) {
        if (!isAuthorized(headers)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(downloadJsonFromUrl("https://jsonplaceholder.typicode.com/todos")).build();
    }



    @GET
    @Path("/source/{resource}")
    public Response dataSource(@PathParam("resource") String resource, @Context HttpHeaders headers) {
        if (!isAuthorized(headers)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        var formatted = "https://jsonplaceholder.typicode.com/%s".formatted(resource);
        return Response.ok(downloadJsonFromUrl(formatted)).build();
    }

    @GET
    @Path("/source/{resource}/{id}")
    public Response dataSource(@PathParam("resource") String resource, @PathParam("id") String id, @Context HttpHeaders headers) {
        if (!isAuthorized(headers)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        var formatted = "https://jsonplaceholder.typicode.com/%s/%s".formatted(resource, id);
        return Response.ok(downloadJsonFromUrl(formatted)).build();
    }

    private boolean isAuthorized(HttpHeaders headers) {
        var authHeader = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return expectedAuthHeader.equals(authHeader);
    }

    private @NotNull String downloadJsonFromUrl(String formatted) {
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
