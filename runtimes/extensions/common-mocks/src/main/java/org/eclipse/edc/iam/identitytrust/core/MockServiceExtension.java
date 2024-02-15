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

package org.eclipse.edc.iam.identitytrust.core;

import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.identithub.did.spi.DidWebParser;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.net.URI;
import java.nio.charset.Charset;

public class MockServiceExtension implements ServiceExtension {

    @Inject
    private DidResolverRegistry resolverRegistry;

    @Inject
    private TypeManager typeManager;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var didResolver = new DidExampleResolver(typeManager.getMapper(), context.getMonitor());

        didResolver.addCached("did:example:dataspace-issuer", "did_example_dataspace-issuer.json");
        resolverRegistry.register(didResolver);
    }

    @Provider
    public DidWebParser createCustomDidWebParser(ServiceExtensionContext context) {
        return new DidWebParserLogInterceptor(context.getMonitor());
    }

    private static class DidWebParserLogInterceptor extends DidWebParser {
        private final Monitor monitor;

        private DidWebParserLogInterceptor(Monitor monitor) {
            this.monitor = monitor;
        }

        @Override
        public String parse(URI url, Charset charset) {
            var did = super.parse(url, charset);
            monitor.debug("DEMO: inbound lookup URL: %s".formatted(url));
            monitor.debug("DEMO: resulting DID: '%s'".formatted(did));

            return did;
        }
    }
}
