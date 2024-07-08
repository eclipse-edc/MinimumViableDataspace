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

package org.eclipse.edc.demo.dcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.keys.keyparsers.PemParser;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.mockito.Mockito.mock;

/**
 * Use this test to read a verifiable credential from the file system, and sign it with a given private key. You will need:
 * <ul>
 *     <li>A JSON file containing the VC</li>
 *     <li>A public/private key pair in either JWK or PEM format</li>
 * </ul>
 */
public class JwtSigner {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void generateJwt() throws JOSEException, IOException {

        var header = new JWSHeader.Builder(JWSAlgorithm.EdDSA)
                .keyID("did:example:dataspace-issuer#key-1")
                .type(JOSEObjectType.JWT)
                .build();


        //todo: change this to whatever credential JSON you want to sign
        var credential = mapper.readValue(new File(System.getProperty("user.dir") + "/../../deployment/assets/credentials/k8s/consumer/membership_vc.json"), Map.class);

        //todo: change the claims to suit your needs
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience("did:web:bob-identityhub%3A7083:bob")
                .subject("did:web:bob-identityhub%3A7083:bob")
                .issuer("did:example:dataspace-issuer")
                .claim("vc", credential)
                .issueTime(Date.from(Instant.now()))
                .build();

        // this must be the path to the Credential issuer's private key
        var privateKey = (PrivateKey) new PemParser(mock()).parse(readFile(System.getProperty("user.dir") + "/../../deployment/assets/issuer_private.pem")).orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        var jwt = new SignedJWT(header, claims);
        jwt.sign(CryptoConverter.createSignerFor(privateKey));

        System.out.println(jwt.serialize());
    }

    private String readFile(String path) {
        try {
            return Files.readString(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
