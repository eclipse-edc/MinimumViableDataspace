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

import org.eclipse.edc.connector.dataplane.api.controller.DataplaneRegistrationApiController;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

/**
 * This extension provides generic endpoints which are open to public participants of the Dataspace to execute
 * requests on the actual data source.
 */
@Extension(value = DataplaneRegistrationExtension.NAME)
public class DataplaneRegistrationExtension implements ServiceExtension {
    public static final String NAME = "Data Plane Registration API";

    @Inject
    private WebService webService;
    @Inject
    private DataPlaneSelectorService selectorService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        webService.registerResource(ApiContext.MANAGEMENT, new DataplaneRegistrationApiController(selectorService));
    }
}
