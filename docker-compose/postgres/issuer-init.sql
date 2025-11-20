CREATE USER issuer WITH ENCRYPTED PASSWORD 'issuer' SUPERUSER;
CREATE DATABASE issuer;
\c issuer issuer

create table if not exists membership_attestations
(
    membership_type       integer   default 0,
    holder_id             varchar                             not null,
    membership_start_date timestamp default now()             not null,
    id                    varchar   default gen_random_uuid() not null
        constraint attestations_pk
            primary key
);

create unique index if not exists membership_attestation_holder_id_uindex
  on membership_attestations (holder_id);

-- seed the consumer and provider into the attestations DB, so that they can request FoobarCredentials sourcing
-- information from the database
INSERT INTO membership_attestations (membership_type, holder_id) VALUES (1, 'did:web:consumer-identityhub%3A7083:consumer');
INSERT INTO membership_attestations (membership_type, holder_id) VALUES (2, 'did:web:provider-identityhub%3A7083:provider');
