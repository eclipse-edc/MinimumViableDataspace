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

package org.eclipse.dataspaceconnector.mvd.function;

import org.eclipse.dataspaceconnector.mvd.model.PolicyFunctionEntry;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.AtomicConstraintFunction;

public class ConstraintFunctionFactory {

    private ConstraintFunctionFactory() {
    }

    public static AtomicConstraintFunction<Permission> create(Monitor monitor, PolicyFunctionEntry.LeftOperand leftOperand) {
        if (FieldValueConstraintFunction.TYPE.equals(leftOperand.getType())) {
            return new FieldValueConstraintFunction(monitor, leftOperand.getValue());
        }
        throw new EdcException("Unknown left operand type: " + leftOperand.getType());
    }
}
