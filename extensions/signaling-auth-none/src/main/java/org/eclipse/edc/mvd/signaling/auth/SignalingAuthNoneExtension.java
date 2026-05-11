/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.mvd.signaling.auth;

import org.eclipse.edc.connector.dataplane.selector.spi.instance.AuthorizationProfile;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.signaling.spi.authorization.Header;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorization;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorizationRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static org.eclipse.edc.spi.result.Result.success;

@Provides(SignalingAuthorizationRegistry.class)
public class SignalingAuthNoneExtension implements ServiceExtension {

    private static final SignalingAuthorization NONE_AUTH = new SignalingAuthorization() {
        @Override
        public String getType() {
            return "none";
        }

        @Override
        public org.eclipse.edc.spi.result.Result<String> isAuthorized(Function<String, String> headerGetter, AuthorizationProfile authorizationProfile) {
            return success("dummy-token");
        }

        @Override
        public org.eclipse.edc.spi.result.Result<Header> evaluate(AuthorizationProfile authorizationProfile) {
            return success(new Header("Authorization", "Bearer dummy-token"));
        }
    };

    @Inject(required = false)
    private SignalingAuthorizationRegistry signalingAuthorizationRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (signalingAuthorizationRegistry == null) {
            var reg = new NoneAuthRegistry();
            context.registerService(SignalingAuthorizationRegistry.class, reg);
            signalingAuthorizationRegistry = reg;
        }

        signalingAuthorizationRegistry.register(NONE_AUTH);
    }


    private static class NoneAuthRegistry implements SignalingAuthorizationRegistry {
        @Override
        public void register(SignalingAuthorization signalingAuthorization) {

        }

        @Override
        public Collection<SignalingAuthorization> getAll() {
            return List.of(NONE_AUTH);
        }

        @Override
        public SignalingAuthorization findByType(String type) {
            return type.equals(NONE_AUTH.getType()) ? NONE_AUTH : null;
        }
    }
}
