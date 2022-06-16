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

package org.eclipse.dataspaceconnector.mvd.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PolicyFunctionEntry {
    private final String type;
    private final String scope;
    private final LeftOperand leftOperand;

    @JsonCreator
    public PolicyFunctionEntry(@JsonProperty("type") String type,
                               @JsonProperty("scope") String scope,
                               @JsonProperty("leftOperand") LeftOperand leftOperand) {
        this.type = type;
        this.scope = scope;
        this.leftOperand = leftOperand;
    }

    public String getScope() {
        return scope;
    }

    public String getType() {
        return type;
    }

    public LeftOperand getLeftOperand() {
        return leftOperand;
    }

    public static final class LeftOperand {

        private final String type;
        private final String value;

        @JsonCreator
        public LeftOperand(@JsonProperty("type") String type, @JsonProperty("value") String value) {
            this.type = type;
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }
}
