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
import jakarta.ws.rs.Produces;
import org.eclipse.dataplane.domain.DataAddress;
import org.eclipse.dataplane.port.ExceptionMapper;
import org.eclipse.edc.mvd.dataplane.signaling.ConsumerDataHandler;

import java.util.Map;

/**
 * Controller that allows consumers to transfer data from the provider by simply providing a data flow ID. This is typically
 * used on consumer data planes.
 */
@Path("/flows")
@Produces("application/json")
public class ConsumerProxyController {

    private final ConsumerDataHandler consumerDataHandler;

    public ConsumerProxyController(ConsumerDataHandler consumerDataHandler) {
        this.consumerDataHandler = consumerDataHandler;
    }

    @GET
    @Path("/{flowId}/data")
    public String handleDataFlow(@PathParam("flowId") String flowId) {
        return consumerDataHandler.downloadData(flowId).orElseThrow(ExceptionMapper.MAP_TO_WSRS);
    }

    @GET
    @Path("/{flowId}")
    public DataAddress getFlowMetadata(@PathParam("flowId") String flowId) {
        return consumerDataHandler.getFlow(flowId);
    }

    @GET
    public Map<String, DataAddress> getAll() {
        return consumerDataHandler.getAllFlows();
    }
}
