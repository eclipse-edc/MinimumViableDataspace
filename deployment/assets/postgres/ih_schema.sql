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



CREATE TABLE IF NOT EXISTS credential_resource
(
    id                    VARCHAR PRIMARY KEY NOT NULL, -- ID of the VC, duplicated here for indexing purposes
    create_timestamp      BIGINT              NOT NULL, -- POSIX timestamp of the creation of the VC
    issuer_id             VARCHAR             NOT NULL,
    holder_id             VARCHAR             NOT NULL,
    vc_state              INTEGER             NOT NULL,
    issuance_policy       JSON,
    reissuance_policy     JSON,
    raw_vc                VARCHAR             NOT NULL, -- Representation of the VC exactly as it was received by the issuer. Can be JWT or JSON(-LD)
    vc_format             INTEGER             NOT NULL, -- 0 = JSON-LD, 1 = JWT
    verifiable_credential JSON                NOT NULL, -- JSON-representation of the verifiable credential
    participant_id        VARCHAR                       -- ID of the ParticipantContext that owns this credentisl
);
CREATE UNIQUE INDEX IF NOT EXISTS credential_resource_credential_id_uindex ON credential_resource USING btree (id);
COMMENT ON COLUMN credential_resource.id IS 'ID of the VC, duplicated here for indexing purposes';
COMMENT ON COLUMN credential_resource.raw_vc IS 'Representation of the VC exactly as it was received by the issuer. Can be JWT or JSON(-LD) ';
COMMENT ON COLUMN credential_resource.vc_format IS '0 = JSON-LD, 1 = JWT';
COMMENT ON COLUMN credential_resource.verifiable_credential IS 'JSON-representation of the VerifiableCredential';

CREATE TABLE IF NOT EXISTS did_resources
(
    did              VARCHAR NOT NULL,
    create_timestamp BIGINT  NOT NULL,
    state_timestamp  BIGINT  NOT NULL,
    state            INT     NOT NULL,
    did_document     JSON    NOT NULL,
    participant_id   VARCHAR,
    PRIMARY KEY (did)
);

CREATE TABLE IF NOT EXISTS keypair_resource
(
    id                    VARCHAR PRIMARY KEY NOT NULL,               -- primary key
    participant_id        VARCHAR,                                    -- ID of the owning ParticipantContext. this is a loose business key, not a FK!
    timestamp             BIGINT              NOT NULL,               -- creation timestamp
    key_id                VARCHAR             NOT NULL,               -- name/key-id of this key pair. for use in JWTs etc.
    group_name            VARCHAR,
    is_default_pair       BOOLEAN                      DEFAULT FALSE, -- whether this keypair is the default one for a participant context
    use_duration          BIGINT,                                     -- maximum time this keypair can be active before it gets rotated
    rotation_duration     BIGINT,                                     -- duration during which this keypair is in a transitional state (rotated, not yet deactivated)
    serialized_public_key VARCHAR             NOT NULL,               -- serialized public key (PEM, JWK,...)
    private_key_alias     VARCHAR             NOT NULL,               -- alias under which the private key is stored in the HSM/Vault
    state                 INT                 NOT NULL DEFAULT 100    -- KeyPairState
);

CREATE TABLE IF NOT EXISTS participant_context
(
    participant_id     VARCHAR PRIMARY KEY NOT NULL, -- ID of the ParticipantContext
    created_date       BIGINT              NOT NULL, -- POSIX timestamp of the creation of the PC
    last_modified_date BIGINT,                       -- POSIX timestamp of the last modified date
    state              INTEGER             NOT NULL, -- 0 = CREATED, 1 = ACTIVE, 2 = DEACTIVATED
    api_token_alias    VARCHAR             NOT NULL, -- alias under which this PC's api token is stored in the vault
    did                VARCHAR,                      -- the DID with which this participant is identified
    roles              JSON                          -- JSON array containing all the roles a user has. may be empty
);
CREATE UNIQUE INDEX IF NOT EXISTS participant_context_participant_id_uindex ON participant_context USING btree (participant_id);

