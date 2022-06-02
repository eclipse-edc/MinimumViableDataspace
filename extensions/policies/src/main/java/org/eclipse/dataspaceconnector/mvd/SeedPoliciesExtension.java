package org.eclipse.dataspaceconnector.mvd;

import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.policy.RuleBindingRegistry;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import static org.eclipse.dataspaceconnector.spi.policy.PolicyEngine.ALL_SCOPES;

/**
 * Extension to initialize the policies.
 */
public class SeedPoliciesExtension implements ServiceExtension {

    private static final String ABS_SPATIAL_POSITION = "ids:absoluteSpatialPosition";

    /**
     * Registry that manages rule bindings to policy scopes.
     */
    @Inject
    private RuleBindingRegistry ruleBindingRegistry;

    /**
     * Policy engine.
     */
    @Inject
    private PolicyEngine policyEngine;

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
        ruleBindingRegistry.bind("USE", ContractDefinitionService.NEGOTIATION_SCOPE);
        ruleBindingRegistry.bind(ABS_SPATIAL_POSITION, ContractDefinitionService.NEGOTIATION_SCOPE);

        policyEngine.registerFunction(ALL_SCOPES, Permission.class, ABS_SPATIAL_POSITION, new AbsSpatialPositionConstraintFunction());
    }

}
