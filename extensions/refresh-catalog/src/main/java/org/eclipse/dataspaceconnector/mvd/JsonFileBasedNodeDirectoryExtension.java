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

package org.eclipse.dataspaceconnector.mvd;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Extension to set up federated cache directory using a set of JSON files as backend.
 */
@Provides(FederatedCacheNodeDirectory.class)
public class JsonFileBasedNodeDirectoryExtension implements ServiceExtension {
    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        TypeManager typeManager = context.getTypeManager();
        var nodeJsonPath = Path.of(Objects.requireNonNull(System.getenv("NODES_JSON_DIR"), "Env var NODES_JSON_DIR is null"));
        var nodeJsonPrefix = Objects.requireNonNull(System.getenv("NODES_JSON_FILES_PREFIX"), "Env var NODES_JSON_FILES_PREFIX is null");
        var service = new JsonFileBasedNodeDirectory(nodeJsonPath, nodeJsonPrefix, monitor, typeManager);
        context.registerService(FederatedCacheNodeDirectory.class, service);
    }
}


