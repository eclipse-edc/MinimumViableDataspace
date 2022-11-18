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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.system.tests.utils;

import java.util.Objects;

import static org.eclipse.edc.util.configuration.ConfigurationFunctions.propOrEnv;

public class TestUtils {
    private TestUtils() {
    }

    public static String requiredPropOrEnv(String key, String defaultValue) {
        return Objects.requireNonNull(propOrEnv(key, defaultValue), key);
    }
}
