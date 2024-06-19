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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class ParticipantContextSeedExtensionTest {

    private final ParticipantContextService participantContextService = mock();
    private final Vault vault = mock();
    private final Monitor monitor = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(ParticipantContextService.class, participantContextService);
        context.registerService(Vault.class, vault);
        context.registerService(Monitor.class, monitor);
    }

    @Test
    void initialize_verifySuperUser(ParticipantContextSeedExtension ext,
                                    ServiceExtensionContext context) {

        when(participantContextService.createParticipantContext(any())).thenReturn(ServiceResult.success("some-key"));

        ext.initialize(context);
        verify(participantContextService).createParticipantContext(any());
        verifyNoMoreInteractions(participantContextService);
    }

    @Test
    void initialize_failsToCreate(ParticipantContextSeedExtension ext, ServiceExtensionContext context) {

        when(participantContextService.createParticipantContext(any()))
                .thenReturn(ServiceResult.badRequest("test-message"));
        assertThatThrownBy(() -> ext.initialize(context)).isInstanceOf(EdcException.class);
        verify(participantContextService).createParticipantContext(any());
        verifyNoMoreInteractions(participantContextService);
    }

    @Test
    void initialize_withApiKeyOverride(ParticipantContextSeedExtension ext,
                                       ServiceExtensionContext context) {


        when(vault.storeSecret(any(), any())).thenReturn(Result.success());

        var apiKeyOverride = "c3VwZXItdXNlcgo=.asdfl;jkasdfl;kasdf";
        when(context.getSetting(eq(ParticipantContextSeedExtension.SUPERUSER_APIKEY_PROPERTY), eq(null)))
                .thenReturn(apiKeyOverride);

        when(participantContextService.createParticipantContext(any()))
                .thenReturn(ServiceResult.success("generated-api-key"));
        when(participantContextService.getParticipantContext(eq("super-user")))
                .thenReturn(ServiceResult.success(superUserContext().build()));

        ext.initialize(context);
        verify(participantContextService).createParticipantContext(any());
        verify(participantContextService).getParticipantContext(eq("super-user"));
        verify(vault).storeSecret(eq("super-user-apikey"), eq(apiKeyOverride));
        verifyNoMoreInteractions(participantContextService, vault);
    }

    @Test
    void initialize_withInvalidKeyOverride(ParticipantContextSeedExtension ext,
                                           ServiceExtensionContext context) {
        when(vault.storeSecret(any(), any())).thenReturn(Result.success());

        var apiKeyOverride = "some-invalid-key";
        when(context.getSetting(eq(ParticipantContextSeedExtension.SUPERUSER_APIKEY_PROPERTY), eq(null)))
                .thenReturn(apiKeyOverride);

        when(participantContextService.createParticipantContext(any()))
                .thenReturn(ServiceResult.success("generated-api-key"));
        when(participantContextService.getParticipantContext(eq("super-user")))
                .thenReturn(ServiceResult.success(superUserContext().build()));

        ext.initialize(context);
        verify(participantContextService).createParticipantContext(any());
        verify(participantContextService).getParticipantContext(eq("super-user"));
        verify(vault).storeSecret(eq("super-user-apikey"), eq(apiKeyOverride));
        verify(monitor).warning(contains("this key appears to have an invalid format"));
        verifyNoMoreInteractions(participantContextService, vault);
    }

    @Test
    void initialize_whenVaultReturnsFailure(ParticipantContextSeedExtension ext,
                                            ServiceExtensionContext context) {
        when(vault.storeSecret(any(), any())).thenReturn(Result.failure("test-failure"));

        var apiKeyOverride = "c3VwZXItdXNlcgo=.asdfl;jkasdfl;kasdf";
        when(context.getSetting(eq(ParticipantContextSeedExtension.SUPERUSER_APIKEY_PROPERTY), eq(null)))
                .thenReturn(apiKeyOverride);

        when(participantContextService.createParticipantContext(any()))
                .thenReturn(ServiceResult.success("generated-api-key"));
        when(participantContextService.getParticipantContext(eq("super-user")))
                .thenReturn(ServiceResult.success(superUserContext().build()));

        ext.initialize(context);
        verify(participantContextService).createParticipantContext(any());
        verify(participantContextService).getParticipantContext(eq("super-user"));
        verify(vault).storeSecret(eq("super-user-apikey"), eq(apiKeyOverride));
        verify(monitor).warning(eq("Error storing API key in vault: test-failure"));
        verifyNoMoreInteractions(participantContextService, vault);
    }

    private ParticipantContext.Builder superUserContext() {
        return ParticipantContext.Builder.newInstance()
                .participantId("super-user")
                .apiTokenAlias("super-user-apikey");

    }
}