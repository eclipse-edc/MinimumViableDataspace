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
  depends_on        = [module.provider-vault]
  source            = "./modules/identity-hub"
  credentials-dir   = dirname("./assets/credentials/k8s/bob/")
  humanReadableName = "bob-identityhub" # must be named "bob-identityhub" until we regenerate DIDs and credentials
  participantId     = var.bob-did
  vault-url         = "http://provider-vault:8200"
  service-name      = "bob"
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