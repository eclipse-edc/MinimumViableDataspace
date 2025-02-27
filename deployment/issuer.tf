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
  depends_on = [kubernetes_config_map.issuer-initdb-config]
  source        = "./modules/postgres"
  instance-name = "issuer"
  init-sql-configs = ["issuer-initdb-config"]
  namespace     = kubernetes_namespace.ns.metadata.0.name
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
      EOT
  }
}