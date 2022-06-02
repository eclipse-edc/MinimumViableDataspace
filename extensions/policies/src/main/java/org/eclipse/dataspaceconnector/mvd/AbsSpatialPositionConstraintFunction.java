package org.eclipse.dataspaceconnector.mvd;

import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.spi.policy.AtomicConstraintFunction;
import org.eclipse.dataspaceconnector.spi.policy.PolicyContext;

import java.util.Collection;
import java.util.Objects;

public class AbsSpatialPositionConstraintFunction implements AtomicConstraintFunction<Permission> {

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, PolicyContext context) {
        var region = context.getParticipantAgent().getClaims().get("region");
        switch (operator) {
            case EQ:
                return Objects.equals(region, rightValue);
            case NEQ:
                return !Objects.equals(region, rightValue);
            case IN:
                return ((Collection<?>) rightValue).contains(region);
            default:
                return false;
        }
    }

}
