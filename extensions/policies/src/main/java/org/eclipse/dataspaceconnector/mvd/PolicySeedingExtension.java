/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.mvd;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.mvd.function.ConstraintFunctionFactory;
import org.eclipse.dataspaceconnector.mvd.model.PolicyFunctionEntry;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.policy.RuleBindingRegistry;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PolicySeedingExtension implements ServiceExtension {
    @EdcSetting
    private static final String POLICY_FILE_NAME_SETTING = "edc.mvd.seeding.policies.file";
    private static final Action USE_ACTION = Action.Builder.newInstance().type("USE").build();

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;
    @Inject
    private PolicyEngine policyEngine;


    @Override
    public String name() {
        return "Gaia-X Hackathon #4 Policy Seeding";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        // This is a common setting. Should be in a dedicated extension
        ruleBindingRegistry.bind(USE_ACTION.getType(), ContractDefinitionService.NEGOTIATION_SCOPE);

        var configFileName = context.getSetting(POLICY_FILE_NAME_SETTING, "default.json");
        var entries = loadPolicyEntries(context.getTypeManager().getMapper(), configFileName);

        entries.forEach(entry -> {
            var function = ConstraintFunctionFactory.create(context.getMonitor(), entry.getLeftOperand());
            context.getMonitor().info(String.format("Create policy function with type %s, scope %s, and leftOperand %s", entry.getType(), entry.getScope(), entry.getLeftOperand()));
            policyEngine.registerFunction(entry.getScope(), Permission.class, entry.getType(), function);
            ruleBindingRegistry.bind(entry.getType(), entry.getScope());
        });
    }

    private List<PolicyFunctionEntry> loadPolicyEntries(ObjectMapper mapper, String resourceName) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            var listType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, PolicyFunctionEntry.class);
            return mapper.readValue(in, listType);
        } catch (Exception e) {
            throw new EdcException(e);
        }
    }
}


