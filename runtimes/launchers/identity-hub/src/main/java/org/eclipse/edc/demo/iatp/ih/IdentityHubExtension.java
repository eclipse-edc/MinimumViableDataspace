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
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.store.model.VerifiableCredentialResource;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.List;

import static org.eclipse.edc.spi.CoreConstants.JSON_LD;


@Extension("IATP Demo: Core Extension for IdentityHub")
public class IdentityHubExtension implements ServiceExtension {

    @Inject
    private CredentialStore store;

    @Inject
    private TypeManager typeManager;


    @Override
    public void initialize(ServiceExtensionContext context) {
        seedCredentials(context);

        // register scope mapper
    }

    @Provider
    public ScopeToCriterionTransformer createScopeTransformer() {
        return new CatenaScopeTransformer(List.of("MembershipCredential", "DismantlerCredential", "BpnCredential"));
    }

    private void seedCredentials(ServiceExtensionContext context) {
        var iamId = context.getConfig().getString("edc.ih.iam.id");
        var objectMapper = typeManager.getMapper(JSON_LD);

        try {
            store.create(objectMapper.readValue(getClass().getClassLoader().getResourceAsStream("credentials/" + iamId + "-membership-credential.json"), VerifiableCredentialResource.class));
            store.create(objectMapper.readValue(getClass().getClassLoader().getResourceAsStream("credentials/" + iamId + "-pcf-credential.json"), VerifiableCredentialResource.class));
        } catch (Exception e) {
            context.getMonitor().severe("Error storing VC", e);
        }
    }
}
