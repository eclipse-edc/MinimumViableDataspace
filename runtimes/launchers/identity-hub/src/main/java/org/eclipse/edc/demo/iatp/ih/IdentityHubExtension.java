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
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.eclipse.edc.spi.CoreConstants.JSON_LD;


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
            seedCredentials(directory, context.getMonitor().withPrefix("DEMO"), getClass().getClassLoader());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // register scope mapper
    }

    @Provider
    public ScopeToCriterionTransformer createScopeTransformer() {
        return new CatenaScopeTransformer(List.of("MembershipCredential", "DismantlerCredential", "BpnCredential"));
    }

    private void seedCredentials(String directory, Monitor monitor, ClassLoader classLoader) throws IOException {

        URL url = classLoader.getResource(directory);
        if (url == null) {
            monitor.warning("Path '%s' does not exist. It must be a relative path within the 'resources' folder! Will not add any VCs.".formatted(directory));
            return;
        }
        String path = url.getPath();
        var files = new File(path).listFiles();
        if (files == null) {
            monitor.warning("No files found in directory '%s'. Will not add any VCs.".formatted(directory));
            return;
        }

        var objectMapper = typeManager.getMapper(JSON_LD);
        Stream.of(files).forEach(p -> {
            try {
                store.create(objectMapper.readValue(p, VerifiableCredentialResource.class));
                monitor.debug("Stored VC from file '%s'".formatted(p.getAbsolutePath()));
            } catch (IOException e) {
                monitor.severe("Error storing VC", e);
            }
        });
    }
}
