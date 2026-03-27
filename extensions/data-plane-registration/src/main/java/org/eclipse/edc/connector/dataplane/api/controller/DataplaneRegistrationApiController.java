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

package org.eclipse.edc.connector.dataplane.api.controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v4beta/dataplanes")
public class DataplaneRegistrationApiController {

    private final DataPlaneSelectorService dataPlaneSelectorService;

    public DataplaneRegistrationApiController(DataPlaneSelectorService dataPlaneSelectorService) {
        this.dataPlaneSelectorService = dataPlaneSelectorService;
    }

    @POST
    public void registerDataplane(DataPlaneInstance instance) {
        dataPlaneSelectorService.register(instance.toBuilder().build()) // ugly, but will initialize all internal objects e.g. clock
                .orElseThrow(exceptionMapper(DataPlaneInstance.class));

    }
}
