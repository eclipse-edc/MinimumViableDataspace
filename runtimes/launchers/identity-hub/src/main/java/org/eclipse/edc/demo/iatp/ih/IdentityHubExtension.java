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

package org.eclipse.edc.demo.iatp.ih;

import org.eclipse.edc.identityhub.spi.ScopeToCriterionTransformer;
import org.eclipse.edc.identityhub.spi.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.constants.CoreConstants;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;


@Extension("IATP Demo: Core Extension for IdentityHub")
public class IdentityHubExtension implements ServiceExtension {

    @Inject
    private CredentialStore store;

    @Inject
    private TypeManager typeManager;


    @Override
    public void initialize(ServiceExtensionContext context) {
        try {
            var directory = context.getConfig().getString("edc.mvd.credentials.path");
            seedCredentials(directory, context.getMonitor().withPrefix("DEMO"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // register scope mapper
    }

    @Provider
    public ScopeToCriterionTransformer createScopeTransformer() {
        return new CatenaScopeTransformer(List.of("MembershipCredential", "DismantlerCredential", "BpnCredential"));
    }

    private void seedCredentials(String directory, Monitor monitor) throws IOException {

        var absPath = new File(directory).getAbsoluteFile();

        if (!absPath.exists()) {
            monitor.warning("Path '%s' does not exist. It must be a resolvable path with read access. Will not add any VCs.".formatted(directory));
            return;
        }
        var files = absPath.listFiles();
        if (files == null) {
            monitor.warning("No files found in directory '%s'. Will not add any VCs.".formatted(directory));
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
