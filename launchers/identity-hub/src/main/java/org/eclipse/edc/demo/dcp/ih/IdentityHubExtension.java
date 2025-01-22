/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.demo.dcp.ih;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;


@Extension("DCP Demo: Core Extension for IdentityHub")
public class IdentityHubExtension implements ServiceExtension {

    @Inject
    private CredentialStore store;

    @Inject
    private TypeManager typeManager;
    private String credentialsDir;
    private Monitor monitor;


    @Override
    public void initialize(ServiceExtensionContext context) {
        credentialsDir = context.getConfig().getString("edc.mvd.credentials.path");
        monitor = context.getMonitor().withPrefix("DEMO");
    }

    @Override
    public void start() {
        try {
            seedCredentials(credentialsDir, monitor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void seedCredentials(String credentialsSourceDirectory, Monitor monitor) throws IOException {

        var absPath = new File(credentialsSourceDirectory).getAbsoluteFile();

        if (!absPath.exists()) {
            monitor.warning("Path '%s' does not exist. It must be a resolvable path with read access. Will not add any VCs.".formatted(credentialsSourceDirectory));
            return;
        }
        var files = absPath.listFiles();
        if (files == null) {
            monitor.warning("No files found in directory '%s'. Will not add any VCs.".formatted(credentialsSourceDirectory));
            return;
        }

        var objectMapper = typeManager.getMapper(JSON_LD);
        // filtering for *.json files is advised, because on K8s there can be softlinks, if a directory is mapped via ConfigMap
        Stream.of(files).filter(f -> f.getName().endsWith(".json")).forEach(p -> {
            try {
                store.create(objectMapper.readValue(p, VerifiableCredentialResource.class));
                monitor.debug("Stored VC from file '%s'".formatted(p.getAbsolutePath()));
            } catch (IOException e) {
                monitor.severe("Error storing VC", e);
            }
        });
    }
}
