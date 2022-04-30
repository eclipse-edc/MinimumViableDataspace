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

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Service to refresh the federated catalog on start-up, using a set of JSON files as input.
 */
class RefreshCatalogService {
    private final FederatedCacheNodeDirectory nodeDirectory;
    private final Path nodeJsonDir;
    private final String nodeJsonPrefix;
    private final Monitor monitor;
    private final TypeManager typeManager;

    /**
     * Constructs a new instance of {@link RefreshCatalogService}.
     *
     * @param nodeDirectory  directory service to populate
     * @param nodeJsonDir    directory containing source JSON files
     * @param nodeJsonPrefix prefix to filter source JSON files on
     * @param monitor        monitor service
     * @param typeManager    type manager service
     */
    RefreshCatalogService(FederatedCacheNodeDirectory nodeDirectory, Path nodeJsonDir, String nodeJsonPrefix, Monitor monitor, TypeManager typeManager) {
        if (!nodeJsonDir.toFile().isDirectory()) {
            throw new EdcException(nodeJsonDir + " should be a directory");
        }
        this.nodeDirectory = nodeDirectory;
        this.nodeJsonDir = nodeJsonDir;
        this.nodeJsonPrefix = nodeJsonPrefix;
        this.monitor = monitor;
        this.typeManager = typeManager;
    }

    /**
     * Populates federated catalog based on JSON files.
     */
    void saveNodeEntries() {
        try {
            monitor.info("Refreshing catalog");
            var files = Files.find(nodeJsonDir, 1,
                    (path, attrs) -> path.toFile().getName().startsWith(nodeJsonPrefix));
            files
                    .map(this::parseFederatedCacheNode)
                    .forEach(this::insertNode);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    private FederatedCacheNode parseFederatedCacheNode(Path path) {
        try {
            return typeManager.readValue(Files.readString(path), FederatedCacheNode.class);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    private void insertNode(FederatedCacheNode node) {
        monitor.info("Adding catalog node " + node.getName());
        nodeDirectory.insert(node);
    }
}


