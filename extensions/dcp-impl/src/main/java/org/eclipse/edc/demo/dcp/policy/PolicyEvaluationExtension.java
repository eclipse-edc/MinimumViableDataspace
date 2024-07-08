/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.demo.dcp.policy;

import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.demo.dcp.policy.MembershipCredentialEvaluationFunction.MEMBERSHIP_CONSTRAINT_KEY;
import static org.eclipse.edc.demo.dcp.policy.PolicyScopes.CATALOG_SCOPE;
import static org.eclipse.edc.demo.dcp.policy.PolicyScopes.NEGOTIATION_SCOPE;
import static org.eclipse.edc.demo.dcp.policy.PolicyScopes.TRANSFER_PROCESS_SCOPE;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;

public class PolicyEvaluationExtension implements ServiceExtension {

    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var fct = new MembershipCredentialEvaluationFunction();
        this.bindPermissionFunction(fct, TRANSFER_PROCESS_SCOPE, MEMBERSHIP_CONSTRAINT_KEY);
        this.bindPermissionFunction(fct, NEGOTIATION_SCOPE, MEMBERSHIP_CONSTRAINT_KEY);
        this.bindPermissionFunction(fct, CATALOG_SCOPE, MEMBERSHIP_CONSTRAINT_KEY);

        registerUseCase("pcf");
        registerUseCase("traceability");
        registerUseCase("sustainability");
        registerUseCase("quality");
        registerUseCase("resiliency");

    }

    private void registerUseCase(String useCaseName) {
        var frameworkFunction = new UseCaseFunction(useCaseName);
        var usecase = frameworkFunction.key();

        bindDutyFunction(frameworkFunction, TRANSFER_PROCESS_SCOPE, usecase);
        bindDutyFunction(frameworkFunction, NEGOTIATION_SCOPE, usecase);
        bindDutyFunction(frameworkFunction, CATALOG_SCOPE, usecase);
    }

    private void bindPermissionFunction(AtomicConstraintFunction<Permission> function, String scope, String constraintType) {
        ruleBindingRegistry.bind("USE", scope);
        ruleBindingRegistry.bind(ODRL_SCHEMA + "use", scope);
        ruleBindingRegistry.bind(constraintType, scope);

        policyEngine.registerFunction(scope, Permission.class, constraintType, function);
    }

    private void bindDutyFunction(AtomicConstraintFunction<Duty> function, String scope, String constraintType) {
        ruleBindingRegistry.bind("USE", scope);
        ruleBindingRegistry.bind(ODRL_SCHEMA + "use", scope);
        ruleBindingRegistry.bind(constraintType, scope);

        policyEngine.registerFunction(scope, Duty.class, constraintType, function);
    }
}
