# This file deploys all the components needed for the provider side of the scenario,
# i.e. a catalog server ("bob"), two connectors ("ted" and "carol") as well as one identityhub and one vault

# first provider connector "Ted"
module "provider-ted-connector" {
  source            = "./modules/connector"
  humanReadableName = "ted"
  participantId     = var.bob-did
  participant-did   = var.bob-did
  database = {
    user     = "ted"
    password = "ted"
    url      = "jdbc:postgresql://${module.bob-postgres.database-url}/ted"
  }
  namespace = kubernetes_namespace.ns.metadata.0.name
  vault-url = "http://provider-vault:8200"
}

# Second provider connector "Carol"
module "provider-carol-connector" {
  source            = "./modules/connector"
  humanReadableName = "carol"
  participantId     = var.bob-did
  participant-did   = var.bob-did
  database = {
    user     = "carol"
    password = "carol"
    url      = "jdbc:postgresql://${module.bob-postgres.database-url}/carol"
  }
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
  database = {
    user = "identityhub"
    password = "identityhub"
    url  = "jdbc:postgresql://${module.bob-postgres.database-url}/identityhub"
  }
}

# Catalog server runtime "Bob"
module "provider-catalog-server" {
  source            = "./modules/catalog-server"
  humanReadableName = "provider-catalog-server"
  participantId     = var.bob-did
  participant-did   = var.bob-did
  namespace         = kubernetes_namespace.ns.metadata.0.name
  vault-url         = "http://provider-vault:8200"
  database = {
    user     = "catalog_server"
    password = "catalog_server"
    url      = "jdbc:postgresql://${module.bob-postgres.database-url}/catalog_server"
  }
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
    kubernetes_config_map.postgres-initdb-config-carol.metadata[0].name,
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

resource "kubernetes_config_map" "postgres-initdb-config-ted" {
  metadata {
    name      = "ted-initdb-config"
    namespace = kubernetes_namespace.ns.metadata.0.name
  }
  data = {
    "ted-initdb-config.sql" = <<-EOT
        CREATE USER ted WITH ENCRYPTED PASSWORD 'ted';
        CREATE DATABASE ted;
        \c ted

        ${file("./assets/postgres/edc_schema.sql")}

        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ted;
      EOT
  }
}

resource "kubernetes_config_map" "postgres-initdb-config-carol" {
  metadata {
    name      = "carol-initdb-config"
    namespace = kubernetes_namespace.ns.metadata.0.name
  }
  data = {
    "carol-initdb-config.sql" = <<-EOT
        CREATE USER carol WITH ENCRYPTED PASSWORD 'carol';
        CREATE DATABASE carol;
        \c carol

        ${file("./assets/postgres/edc_schema.sql")}

        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO carol;
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