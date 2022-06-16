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

import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.AtomicConstraintFunction;
import org.eclipse.dataspaceconnector.spi.policy.PolicyContext;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class FieldValueConstraintFunction implements AtomicConstraintFunction<Permission> {

    public static final String TYPE = "field";

    private final Monitor monitor;
    private final EvaluationFunction function;

    public FieldValueConstraintFunction(Monitor monitor, String path) {
        this.monitor = monitor;
        this.function = new EvaluationFunction(path);
    }

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, PolicyContext context) {
        var result = function.apply(context.getParticipantAgent().getClaims());
        if (result.failed()) {
            monitor.warning("Failed to retrieve value: " + String.join(",", result.getFailureMessages()));
            return false;
        }

        var value = result.getContent();
        switch (operator) {
            case EQ:
                return Objects.equals(value, rightValue);
            case NEQ:
                return !Objects.equals(value, rightValue);
            case IN:
                return ((Collection<?>) rightValue).contains(value);
            default:
                return false;
        }
    }

    private static final class EvaluationFunction implements Function<Map<String, Object>, Result<String>> {
        private final List<String> path;

        public EvaluationFunction(String p) {
            if (p.isBlank()) {
                throw new EdcException("Path cannot be empty");
            }
            this.path = List.of(p.split("\\."));
        }

        @Override
        public Result<String> apply(Map<String, Object> claims) {
            Object value = claims;
            for (String s : path) {
                if (value instanceof Map) {
                    var result = next((Map<String, Object>) value, s);
                    if (result.failed()) {
                        return Result.failure(result.getFailureMessages());
                    }
                    value = result.getContent();
                } else {
                    return Result.failure("Not a node: " + value);
                }
            }
            if (value instanceof String) {
                return Result.success((String) value);
            } else {
                return Result.failure("Evaluation result must be a String, node: " + value);
            }
        }


        private static Result<Object> next(Map<String, Object> node, String key) {
            return Optional.ofNullable(node.get(key))
                    .map(Result::success)
                    .orElse(Result.failure(String.format("Cannot find key %s in %s", key, node)));
        }
    }
}
