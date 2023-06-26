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
 *       Microsoft Corporation - initial implementation
 *
 */

package org.eclipse.edc.mvd;

import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import static org.eclipse.edc.connector.contract.spi.offer.ContractDefinitionResolver.CATALOGING_SCOPE;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

/**
 * Extension to initialize the policies.
 */
public class SeedPoliciesExtension implements ServiceExtension {

    private static final String REGION_LOCATION = "regionLocation";
    private static final String REGION_LOCATION_EVALUATION_KEY = EDC_NAMESPACE + REGION_LOCATION;

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;

    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private TypeManager typeManager;

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return "Seed policies.";
    }

    /**
     * Initializes the extension by binding the policies to the rule binding registry.
     *
     * @param context service extension context.
     */
    @Override
    public void initialize(ServiceExtensionContext context) {
        ruleBindingRegistry.bind("USE", CATALOGING_SCOPE);
        ruleBindingRegistry.bind(ODRL_SCHEMA + "use", CATALOGING_SCOPE);
        ruleBindingRegistry.bind(REGION_LOCATION_EVALUATION_KEY, CATALOGING_SCOPE);
        policyEngine.registerFunction(CATALOGING_SCOPE, Permission.class, REGION_LOCATION_EVALUATION_KEY, new RegionConstraintFunction());
    }

}
