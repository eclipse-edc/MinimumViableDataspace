# This file deploys all the components needed for the provider side of the scenario,
# i.e. a catalog server ("bob"), two connectors ("ted" and "carol") as well as one identityhub and one vault

# first provider connector "provider-qna"
module "provider-qna-connector" {
  source            = "./modules/connector"
  humanReadableName = "provider-qna"
  participantId     = var.provider-did
  participant-did   = var.provider-did
  database = {
    user     = "qna"
    password = "provider-qna"
    url      = "jdbc:postgresql://${module.provider-postgres.database-url}/provider_qna"
  }
  namespace = kubernetes_namespace.ns.metadata.0.name
  vault-url = "http://provider-vault:8200"
}

# Second provider connector "provider-manufacturing"
module "provider-manufacturing-connector" {
  source            = "./modules/connector"
  humanReadableName = "provider-manufacturing"
  participantId     = var.provider-did
  participant-did   = var.provider-did
  database = {
    user     = "manufacturing"
    password = "provider-manufacturing"
    url      = "jdbc:postgresql://${module.provider-postgres.database-url}/provider_manufacturing"
  }
  namespace = kubernetes_namespace.ns.metadata.0.name
  vault-url = "http://provider-vault:8200"
}

module "provider-identityhub" {
  depends_on        = [module.provider-vault]
  source            = "./modules/identity-hub"
  credentials-dir   = dirname("./assets/credentials/k8s/provider/")
  humanReadableName = "provider-identityhub" # must be named "bob-identityhub" until we regenerate DIDs and credentials
  participantId     = var.provider-did
  vault-url         = "http://provider-vault:8200"
  service-name      = "provider"
  database = {
    user     = "identityhub"
    password = "identityhub"
    url      = "jdbc:postgresql://${module.provider-postgres.database-url}/identityhub"
  }
}

# Catalog server runtime
module "provider-catalog-server" {
  source            = "./modules/catalog-server"
  humanReadableName = "provider-catalog-server"
  participantId     = var.provider-did
  participant-did   = var.provider-did
  namespace         = kubernetes_namespace.ns.metadata.0.name
  vault-url         = "http://provider-vault:8200"
  database = {
    user     = "catalog_server"
    password = "catalog_server"
    url      = "jdbc:postgresql://${module.provider-postgres.database-url}/catalog_server"
  }
}

module "provider-vault" {
  source            = "./modules/vault"
  humanReadableName = "provider-vault"
}

# Postgres database for the consumer
module "provider-postgres" {
  depends_on    = [kubernetes_config_map.postgres-initdb-config-cs]
  source        = "./modules/postgres"
  instance-name = "provider"
  init-sql-configs = [
    kubernetes_config_map.postgres-initdb-config-cs.metadata[0].name,
    kubernetes_config_map.postgres-initdb-config-pqna.metadata[0].name,
    kubernetes_config_map.postgres-initdb-config-pm.metadata[0].name,
    kubernetes_config_map.postgres-initdb-config-ih.metadata[0].name
  ]
  namespace = kubernetes_namespace.ns.metadata.0.name
}

resource "kubernetes_config_map" "postgres-initdb-config-cs" {
  metadata {
    name      = "cs-initdb-config"
    namespace = kubernetes_namespace.ns.metadata.0.name
  }
  data = {
    "cs-initdb-config.sql" = <<-EOT
        CREATE USER catalog_server WITH ENCRYPTED PASSWORD 'catalog_server';
        CREATE DATABASE catalog_server;
        \c catalog_server

        ${file("./assets/postgres/edc_schema.sql")}

        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO catalog_server;
      EOT
  }
}

resource "kubernetes_config_map" "postgres-initdb-config-pqna" {
  metadata {
    name      = "provider-qna-initdb-config"
    namespace = kubernetes_namespace.ns.metadata.0.name
  }
  data = {
    "provider-qna-initdb-config.sql" = <<-EOT
        CREATE USER qna WITH ENCRYPTED PASSWORD 'provider-qna';
        CREATE DATABASE provider_qna;
        \c provider_qna

        ${file("./assets/postgres/edc_schema.sql")}

        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO qna;
      EOT
  }
}

resource "kubernetes_config_map" "postgres-initdb-config-pm" {
  metadata {
    name      = "provider-manufacturing-initdb-config"
    namespace = kubernetes_namespace.ns.metadata.0.name
  }
  data = {
    "provider-manufacturing-initdb-config.sql" = <<-EOT
        CREATE USER manufacturing WITH ENCRYPTED PASSWORD 'provider-manufacturing';
        CREATE DATABASE provider_manufacturing;
        \c provider_manufacturing

        ${file("./assets/postgres/edc_schema.sql")}

        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO manufacturing;
      EOT
  }
}

resource "kubernetes_config_map" "postgres-initdb-config-ih" {
  metadata {
    name      = "ih-initdb-config"
    namespace = kubernetes_namespace.ns.metadata.0.name
  }
  data = {
    "ih-initdb-config.sql" = <<-EOT
        CREATE USER identityhub WITH ENCRYPTED PASSWORD 'identityhub';
        CREATE DATABASE identityhub;
        \c identityhub

        ${file("./assets/postgres/ih_schema.sql")}

        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO identityhub;
      EOT
  }
}