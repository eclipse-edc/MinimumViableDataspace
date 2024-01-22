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

package org.eclipse.edc.demo.iatp.core;

import org.eclipse.edc.identitytrust.TrustedIssuerRegistry;
import org.eclipse.edc.identitytrust.VcConstants;
import org.eclipse.edc.identitytrust.model.Issuer;
import org.eclipse.edc.identitytrust.scope.ScopeExtractorRegistry;
import org.eclipse.edc.identitytrust.verification.SignatureSuiteRegistry;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.security.signature.jws2020.JwsSignature2020Suite;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.iam.identitytrust.core.IatpScopeExtractorExtension.CATALOG_REQUEST_SCOPE;
import static org.eclipse.edc.iam.identitytrust.core.IatpScopeExtractorExtension.NEGOTIATION_REQUEST_SCOPE;
import static org.eclipse.edc.iam.identitytrust.core.IatpScopeExtractorExtension.TRANSFER_PROCESS_REQUEST_SCOPE;
import static org.eclipse.edc.spi.CoreConstants.JSON_LD;

public class IatpPatchExtension implements ServiceExtension {
    @Inject
    private TypeManager typeManager;

    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private SignatureSuiteRegistry signatureSuiteRegistry;

    @Inject
    private TrustedIssuerRegistry trustedIssuerRegistry;

    @Inject
    private ScopeExtractorRegistry scopeExtractorRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {

        // register signature suite
        var suite = new JwsSignature2020Suite(typeManager.getMapper(JSON_LD));
        signatureSuiteRegistry.register(VcConstants.JWS_2020_SIGNATURE_SUITE, suite);

        // register dataspace issuer
        trustedIssuerRegistry.addIssuer(new Issuer("did:example:dataspace-issuer", Map.of()));

        // register a default scope provider
        var contextMappingFunction = new DefaultScopeExtractor(Set.of(
                "org.eclipse.edc.vc.type:MembershipCredential:read"));
        policyEngine.registerPostValidator(CATALOG_REQUEST_SCOPE, contextMappingFunction);
        policyEngine.registerPostValidator(NEGOTIATION_REQUEST_SCOPE, contextMappingFunction);
        policyEngine.registerPostValidator(TRANSFER_PROCESS_REQUEST_SCOPE, contextMappingFunction);


        //register scope extractor
        scopeExtractorRegistry.registerScopeExtractor(new FrameworkCredentialScopeExtractor());

    }
}
