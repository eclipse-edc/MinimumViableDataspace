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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.core.base.policy.PolicyContextImpl;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FieldValueConstraintTest {

    private static final String HEADQUARTER_ADDRESS_PATH = "selfDescriptionCredential.selfDescription.gx-participant:headquarterAddress.gx-participant:country.@value";

    private FieldValueConstraintFunction constraint;

    @BeforeEach
    public void setUp() {
        var monitor = mock(Monitor.class);
        constraint = new FieldValueConstraintFunction(monitor, HEADQUARTER_ADDRESS_PATH);
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideConfigs")
    void success(String name, Operator operator, List<Object> values, boolean expected) throws IOException {
        var sdd = loadFile("participant-fr.json");
        var participantAgent = new ParticipantAgent(sdd, Collections.emptyMap());

        var result = constraint.evaluate(operator, values, null, new PolicyContextImpl(participantAgent));

        assertThat(result).isEqualTo(expected);
    }

    private static Stream<Arguments> provideConfigs() {
        return Stream.of(
                Arguments.of("IS IN LIST", Operator.IN, Arrays.asList(250, "FR", "FRA"), true),
                Arguments.of("IS NOT IN LIST", Operator.IN, Arrays.asList(276, "DE", "DEU"), false)
        );
    }

    private static Map<String, Object> loadFile(String fileName) throws IOException {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            return new ObjectMapper().readValue(in, Map.class);
        }
    }
}