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

import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class ParticipantContextSeedExtensionTest {

    public static final String SUPER_USER = "super-user";
    public static final String API_KEY = "apiKey";
    private final ParticipantContextService participantContextService = mock();
    private final Vault vault = mock();
    private final Monitor monitor = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(ParticipantContextService.class, participantContextService);
        context.registerService(Vault.class, vault);
        context.registerService(Monitor.class, monitor);
        when(participantContextService.getParticipantContext(eq(SUPER_USER))).thenReturn(ServiceResult.notFound("foobar"));
    }

    @Test
    void start_verifySuperUser(ParticipantContextSeedExtension ext,
                               ServiceExtensionContext context) {

        when(participantContextService.createParticipantContext(any())).thenReturn(ServiceResult.success(Map.of(API_KEY, "some-key")));
        ext.initialize(context);

        ext.start();
        verify(participantContextService).getParticipantContext(eq(SUPER_USER));
        verify(participantContextService).createParticipantContext(any());
        verifyNoMoreInteractions(participantContextService);
    }

    @Test
    void start_failsToCreate(ParticipantContextSeedExtension ext, ServiceExtensionContext context) {

        when(participantContextService.createParticipantContext(any()))
                .thenReturn(ServiceResult.badRequest("test-message"));
        ext.initialize(context);
        assertThatThrownBy(ext::start).isInstanceOf(EdcException.class);

        verify(participantContextService).getParticipantContext(eq(SUPER_USER));
        verify(participantContextService).createParticipantContext(any());
        verifyNoMoreInteractions(participantContextService);
    }

    @Test
    void start_withApiKeyOverride(ParticipantContextSeedExtension ext,
                                  ServiceExtensionContext context) {


        when(vault.storeSecret(any(), any())).thenReturn(Result.success());

        var apiKeyOverride = "c3VwZXItdXNlcgo=.asdfl;jkasdfl;kasdf";
        when(context.getSetting(eq(ParticipantContextSeedExtension.SUPERUSER_APIKEY_PROPERTY), eq(null)))
                .thenReturn(apiKeyOverride);

        when(participantContextService.createParticipantContext(any()))
                .thenReturn(ServiceResult.success(Map.of(API_KEY, "generated-api-key")));
        when(participantContextService.getParticipantContext(eq(SUPER_USER)))
                .thenReturn(ServiceResult.notFound("foobar"))
                .thenReturn(ServiceResult.success(superUserContext().build()));

        ext.initialize(context);
        ext.start();
        verify(participantContextService, times(2)).getParticipantContext(eq(SUPER_USER));
        verify(participantContextService).createParticipantContext(any());
        verify(vault).storeSecret(eq("super-user-apikey"), eq(apiKeyOverride));
        verifyNoMoreInteractions(participantContextService, vault);
    }

    @Test
    void start_withInvalidKeyOverride(ParticipantContextSeedExtension ext,
                                      ServiceExtensionContext context) {
        when(vault.storeSecret(any(), any())).thenReturn(Result.success());

        var apiKeyOverride = "some-invalid-key";
        when(context.getSetting(eq(ParticipantContextSeedExtension.SUPERUSER_APIKEY_PROPERTY), eq(null)))
                .thenReturn(apiKeyOverride);

        when(participantContextService.createParticipantContext(any()))
                .thenReturn(ServiceResult.success(Map.of(API_KEY, "generated-api-key")));
        when(participantContextService.getParticipantContext(eq(SUPER_USER)))
                .thenReturn(ServiceResult.notFound("foobar"))
                .thenReturn(ServiceResult.success(superUserContext().build()));

        ext.initialize(context);
        ext.start();
        verify(participantContextService).createParticipantContext(any());
        verify(participantContextService, times(2)).getParticipantContext(eq(SUPER_USER));
        verify(vault).storeSecret(eq("super-user-apikey"), eq(apiKeyOverride));
        verify(monitor).warning(contains("this key appears to have an invalid format"));
        verifyNoMoreInteractions(participantContextService, vault);
    }

    @Test
    void start_whenVaultReturnsFailure(ParticipantContextSeedExtension ext,
                                       ServiceExtensionContext context) {
        when(vault.storeSecret(any(), any())).thenReturn(Result.failure("test-failure"));

        var apiKeyOverride = "c3VwZXItdXNlcgo=.asdfl;jkasdfl;kasdf";
        when(context.getSetting(eq(ParticipantContextSeedExtension.SUPERUSER_APIKEY_PROPERTY), eq(null)))
                .thenReturn(apiKeyOverride);

        when(participantContextService.createParticipantContext(any()))
                .thenReturn(ServiceResult.success(Map.of(API_KEY, "generated-api-key")));
        when(participantContextService.getParticipantContext(eq(SUPER_USER)))
                .thenReturn(ServiceResult.notFound("foobar"))
                .thenReturn(ServiceResult.success(superUserContext().build()));

        ext.initialize(context);
        ext.start();
        verify(participantContextService, times(2)).getParticipantContext(eq(SUPER_USER));
        verify(participantContextService).createParticipantContext(any());
        verify(vault).storeSecret(eq("super-user-apikey"), eq(apiKeyOverride));
        verify(monitor).warning(eq("Error storing API key in vault: test-failure"));
        verifyNoMoreInteractions(participantContextService, vault);
    }

    private ParticipantContext.Builder superUserContext() {
        return ParticipantContext.Builder.newInstance()
                .participantId(SUPER_USER)
                .apiTokenAlias("super-user-apikey");

    }
}