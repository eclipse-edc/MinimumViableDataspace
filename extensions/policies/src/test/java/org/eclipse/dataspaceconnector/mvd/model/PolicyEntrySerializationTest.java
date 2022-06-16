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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyEntrySerializationTest {
    private final static ObjectMapper MAPPER = new TypeManager().getMapper();

    @Test
    void deserialize() throws IOException {
        var entry = loadFile("policy-function-entry.json");

        assertThat(entry).isNotNull();
        assertThat(entry.getType()).isEqualTo("test");
        assertThat(entry.getScope()).isEqualTo("scope-test");
        assertThat(entry.getLeftOperand()).isNotNull();
        var left = entry.getLeftOperand();
        assertThat(left.getType()).isEqualTo("foo");
        assertThat(left.getValue()).isEqualTo("bar");
    }

    private static PolicyFunctionEntry loadFile(String fileName) throws IOException {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            Objects.requireNonNull(in, "Failed to open file: " + fileName);
            return MAPPER.readValue(in, PolicyFunctionEntry.class);
        }
    }
}