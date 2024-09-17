/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.seed;

import org.eclipse.edc.identityhub.spi.authentication.ServicePrincipal;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;

public class ParticipantContextSeedExtension implements ServiceExtension {
    public static final String NAME = "MVD ParticipantContext Seed Extension";
    public static final String DEFAULT_SUPER_USER_PARTICIPANT_ID = "super-user";

    @Setting(value = "Explicitly set the initial API key for the Super-User")
    public static final String SUPERUSER_APIKEY_PROPERTY = "edc.ih.api.superuser.key";

    @Setting(value = "Config value to set the super-user's participant ID.", defaultValue = DEFAULT_SUPER_USER_PARTICIPANT_ID)
    public static final String SUPERUSER_PARTICIPANT_ID_PROPERTY = "edc.ih.api.superuser.id";
    private String superUserParticipantId;
    private String superUserApiKey;
    private Monitor monitor;
    @Inject
    private ParticipantContextService participantContextService;
    @Inject
    private Vault vault;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        superUserParticipantId = context.getSetting(SUPERUSER_PARTICIPANT_ID_PROPERTY, DEFAULT_SUPER_USER_PARTICIPANT_ID);
        superUserApiKey = context.getSetting(SUPERUSER_APIKEY_PROPERTY, null);
        monitor = context.getMonitor();
    }

    @Override
    public void start() {
        // create super-user
        if (participantContextService.getParticipantContext(superUserParticipantId).succeeded()) { // already exists
            monitor.debug("super-user already exists with ID '%s', will not re-create".formatted(superUserParticipantId));
            return;
        }
        participantContextService.createParticipantContext(ParticipantManifest.Builder.newInstance()
                        .participantId(superUserParticipantId)
                        .did("did:web:%s".formatted(superUserParticipantId)) // doesn't matter, not intended for resolution
                        .active(true)
                        .key(KeyDescriptor.Builder.newInstance()
                                .keyGeneratorParams(Map.of("algorithm", "EdDSA", "curve", "Ed25519"))
                                .keyId("%s-key".formatted(superUserParticipantId))
                                .privateKeyAlias("%s-alias".formatted(superUserParticipantId))
                                .build())
                        .roles(List.of(ServicePrincipal.ROLE_ADMIN))
                        .build())
                .onSuccess(generatedKey -> {
                    var apiKey = ofNullable(superUserApiKey)
                            .map(key -> {
                                if (!key.contains(".")) {
                                    monitor.warning("Super-user key override: this key appears to have an invalid format, you may be unable to access some APIs. It must follow the structure: 'base64(<participantId>).<random-string>'");
                                }
                                participantContextService.getParticipantContext(superUserParticipantId)
                                        .onSuccess(pc -> vault.storeSecret(pc.getApiTokenAlias(), key)
                                                .onSuccess(u -> monitor.debug("Super-user key override successful"))
                                                .onFailure(f -> monitor.warning("Error storing API key in vault: %s".formatted(f.getFailureDetail()))))
                                        .onFailure(f -> monitor.warning("Error overriding API key for '%s': %s".formatted(superUserParticipantId, f.getFailureDetail())));
                                return key;
                            })
                            .orElse(generatedKey.get("apiKey").toString());
                    monitor.info("Created user 'super-user'. Please take note of the API Key: %s".formatted(apiKey));
                })
                .orElseThrow(f -> new EdcException("Error creating Super-User: " + f.getFailureDetail()));
    }
}
