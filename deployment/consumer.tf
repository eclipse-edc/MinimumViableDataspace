# consumer connector
module "alice-connector" {
  source            = "./modules/connector"
  humanReadableName = "alice"
  participantId     = var.alice-did
  participant-did   = var.alice-did
  database-name     = "alice"
  namespace         = kubernetes_namespace.ns.metadata.0.name
  vault-url         = "http://consumer-vault:8200"
}

# consumer identity hub
module "consumer-alice-identityhub" {
  source            = "./modules/identity-hub"
  credentials-dir = dirname("./assets/credentials/k8s/alice/")
  humanReadableName = "alice-identityhub"
  participantId     = var.alice-did
  vault-url         = "http://consumer-vault:8200"
  service-name      = "alice"
}

# consumer vault
module "consumer-vault" {
  source            = "./modules/vault"
  humanReadableName = "consumer-vault"
}