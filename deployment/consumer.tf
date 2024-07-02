# This file deploys all the components needed for the consumer side of the scenario,
# i.e. the connector, an identityhub and a vault.

# consumer connector
module "alice-connector" {
  source            = "./modules/connector"
  humanReadableName = "alice"
  participantId     = var.alice-did
  participant-did   = var.alice-did
  database = {
    user = "alice"
    password = "alice"
    url  = "jdbc:postgresql://${module.consumer-postgres.database-url}/consumer"
  }
  namespace         = kubernetes_namespace.ns.metadata.0.name
  vault-url         = "http://consumer-vault:8200"
}

# consumer identity hub
module "consumer-alice-identityhub" {
  depends_on = [module.consumer-vault]
  source            = "./modules/identity-hub"
  credentials-dir = dirname("./assets/credentials/k8s/alice/")
  humanReadableName = "alice-identityhub"
  participantId     = var.alice-did
  vault-url         = "http://consumer-vault:8200"
  service-name      = "alice"
  database = {
    user = "alice"
    password = "alice"
    url  = "jdbc:postgresql://${module.consumer-postgres.database-url}/consumer"
  }
}

# consumer vault
module "consumer-vault" {
  source            = "./modules/vault"
  humanReadableName = "consumer-vault"
}

# Postgres database for the consumer
module "consumer-postgres" {
  depends_on = [kubernetes_config_map.postgres-initdb-config-consumer]
  source        = "./modules/postgres"
  instance-name = "alice"
  init-sql-configs = ["alice-initdb-config"]
  namespace     = kubernetes_namespace.ns.metadata.0.name
}

# DB initialization for the EDC database
resource "kubernetes_config_map" "postgres-initdb-config-consumer" {
  metadata {
    name      = "alice-initdb-config"
    namespace = kubernetes_namespace.ns.metadata.0.name
  }
  data = {
    "alice-initdb-config.sql" = <<-EOT
        CREATE USER alice WITH ENCRYPTED PASSWORD 'alice';
        CREATE DATABASE consumer;
        \c consumer

        ${file("./assets/postgres/edc_schema.sql")}

        ${file("./assets/postgres/ih_schema.sql")}

        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO alice;
      EOT
  }
}