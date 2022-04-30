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
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RefreshCatalogServiceTest {

    @Test
    void saveNodeEntries() {
        var directory = mock(FederatedCacheNodeDirectory.class);
        var sampleFile = getClass().getClassLoader().getResource("24-node1.json");
        assertThat(sampleFile).isNotNull();
        var nodeJsonDir = Path.of(sampleFile.getPath()).getParent();
        var nodeJsonPrefix = "24-";
        var monitor = new ConsoleMonitor();
        var typeManager = new TypeManager();
        var service = new RefreshCatalogService(directory, nodeJsonDir, nodeJsonPrefix, monitor, typeManager);

        service.saveNodeEntries();

        verify(directory, times(1)).insert(argThat(n -> "node24-1".equals(n.getName())));
        verify(directory, times(1)).insert(argThat(n -> "node24-2".equals(n.getName())));
    }
}