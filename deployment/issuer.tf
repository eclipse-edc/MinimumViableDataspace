#
#  Copyright (c) 2025 Cofinity-X
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0
#
#  SPDX-License-Identifier: Apache-2.0
#
#  Contributors:
#       Cofinity-X - initial API and implementation
#

module "dataspace-issuer" {
  source            = "./modules/issuer"
  humanReadableName = "dataspace-issuer-service"
  participantId     = var.consumer-did
  database = {
    user     = "issuer"
    password = "issuer"
    url      = "jdbc:postgresql://${module.dataspace-issuer-postgres.database-url}/issuer"
  }
  vault-url = "http://consumer-vault:8200"
  namespace = kubernetes_namespace.ns.metadata.0.name
  useSVE    = var.useSVE
}

# Postgres database for the consumer
module "dataspace-issuer-postgres" {
  depends_on       = [kubernetes_config_map.issuer-initdb-config]
  source           = "./modules/postgres"
  instance-name    = "issuer"
  init-sql-configs = ["issuer-initdb-config"]
  namespace        = kubernetes_namespace.ns.metadata.0.name
}

# DB initialization for the EDC database
resource "kubernetes_config_map" "issuer-initdb-config" {
  metadata {
    name      = "issuer-initdb-config"
    namespace = kubernetes_namespace.ns.metadata.0.name
  }
  data = {
    "issuer-initdb-config.sql" = <<-EOT
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
      EOT
  }
}