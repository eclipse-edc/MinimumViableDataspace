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

package org.eclipse.edc.demo.dataplane.proxy;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static jakarta.ws.rs.core.MediaType.WILDCARD;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

@Path("{any:.*}")
@Consumes(WILDCARD)
@Produces(WILDCARD)
public class DataPlaneProxyController {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final DataPlaneAuthorizationService authorizationService;

    public DataPlaneProxyController(DataPlaneAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @GET
    public Response get(@Context ContainerRequestContext requestContext) {
        return proxy(requestContext);
    }

    @POST
    public Response post(@Context ContainerRequestContext requestContext) {
        return proxy(requestContext);
    }

    @PUT
    public Response put(@Context ContainerRequestContext requestContext) {
        return proxy(requestContext);
    }

    @DELETE
    public Response delete(@Context ContainerRequestContext requestContext) {
        return proxy(requestContext);
    }

    @PATCH
    public Response patch(@Context ContainerRequestContext requestContext) {
        return proxy(requestContext);
    }

    private Response proxy(ContainerRequestContext requestContext) {
        var token = requestContext.getHeaderString(AUTHORIZATION);
        if (token == null) {
            return Response.status(UNAUTHORIZED).build();
        }

        var authorization = authorizationService.authorize(token, Collections.emptyMap());
        if (authorization.failed()) {
            return Response.status(FORBIDDEN).build();
        }

        var sourceDataAddress = authorization.getContent();

        try {
            var baseUrl = sourceDataAddress.getStringProperty(EDC_NAMESPACE + "baseUrl");
            var path = requestContext.getUriInfo().getPath();
            var targetUrl = (path != null && !path.isEmpty()) ? baseUrl + "/" + path : baseUrl;

            var requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .method(requestContext.getMethod(), HttpRequest.BodyPublishers.ofInputStream(requestContext::getEntityStream));

            // Forward relevant headers
            requestContext.getHeaders().forEach((name, values) -> {
                if (!AUTHORIZATION.equalsIgnoreCase(name)) {
                    values.forEach(value -> requestBuilder.header(name, value));
                }
            });

            var response = HTTP_CLIENT.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
            return Response.status(response.statusCode())
                    .header(CONTENT_TYPE, response.headers().firstValue(CONTENT_TYPE).orElse(APPLICATION_OCTET_STREAM))
                    .entity(response.body())
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity("{\"error\": \"Request interrupted\"}")
                    .build();
        } catch (IOException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity("{\"error\": \"Failed to contact backend service\"}")
                    .build();
        }
    }
}
