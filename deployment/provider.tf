# This file deploys all the components needed for the provider side of the scenario,
# i.e. a catalog server ("bob"), two connectors ("ted" and "carol") as well as one identityhub and one vault

# first provider connector "Ted"
module "provider-ted-connector" {
  source            = "./modules/connector"
  humanReadableName = "ted"
  participantId     = var.bob-did
  participant-did   = var.bob-did
  database-name     = "ted"
  namespace         = kubernetes_namespace.ns.metadata.0.name
  vault-url         = "http://provider-vault:8200"
}

# Second provider connector "Carol"
module "provider-carol-connector" {
  source            = "./modules/connector"
  humanReadableName = "carol"
  participantId     = var.bob-did
  participant-did   = var.bob-did
  database-name     = "carol"
  namespace         = kubernetes_namespace.ns.metadata.0.name
  vault-url         = "http://provider-vault:8200"
}

module "provider-identityhub" {
  depends_on = [module.provider-vault]
  source        = "./modules/identity-hub"
  credentials-dir = dirname("./assets/credentials/k8s/bob/")
  humanReadableName = "bob-identityhub" # must be named "bob-identityhub" until we regenerate DIDs and credentials
  participantId = var.bob-did
  vault-url     = "http://provider-vault:8200"
  service-name  = "bob"
}

# Catalog server runtime "Bob"
module "provider-catalog-server" {
  source            = "./modules/catalog-server"
  humanReadableName = "provider-catalog-server"
  participantId     = var.bob-did
  participant-did   = var.bob-did
  database-name     = "provider-catalog-server"
  namespace         = kubernetes_namespace.ns.metadata.0.name
  vault-url         = "http://provider-vault:8200"

}

module "provider-vault" {
  source            = "./modules/vault"
  humanReadableName = "provider-vault"
}

# Postgres database for the consumer
module "bob-postgres" {
  depends_on = [kubernetes_config_map.postgres-initdb-config-cs]
  source        = "./modules/postgres"
  instance-name = "bob"
  init-sql-configs = [
    kubernetes_config_map.postgres-initdb-config-cs.metadata[0].name,
    kubernetes_config_map.postgres-initdb-config-ted.metadata[0].name,
    kubernetes_config_map.postgres-initdb-config-carol.metadata[0].name
  ]
  namespace = kubernetes_namespace.ns.metadata.0.name
}

resource "kubernetes_config_map" "postgres-initdb-config-cs" {
  metadata {
    name      = "cs-initdb-config"
    namespace = kubernetes_namespace.ns.metadata.0.name
  }
  data = {
    "cs-initdb-config.sql" = file("./assets/postgres/catalog-server.sql")
  }
}

resource "kubernetes_config_map" "postgres-initdb-config-ted" {
  metadata {
    name      = "ted-initdb-config"
    namespace = kubernetes_namespace.ns.metadata.0.name
  }
  data = {
    "ted-initdb-config.sql" = file("./assets/postgres/ted.sql")
  }
}

resource "kubernetes_config_map" "postgres-initdb-config-carol" {
  metadata {
    name      = "carol-initdb-config"
    namespace = kubernetes_namespace.ns.metadata.0.name
  }
  data = {
    "carol-initdb-config.sql" = file("./assets/postgres/carol.sql")
  }
}