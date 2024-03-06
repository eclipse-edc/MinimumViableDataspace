locals {
  edc_resources_folder     = "/resources"
  dataspace_authority_name = "authority"
}

data "azurerm_subscription" "current_subscription" {
}

data "azurerm_client_config" "current_client" {
}

locals {
  registry_files_prefix = "${var.prefix}-"

  connector_name = "registration-service"

  registration_service_dns_label   = "${var.prefix}-registration-mvd"
  edc_default_port                 = 8181
  registration_service_port        = 8184
  registration_service_path_prefix = "/api/authority"
  registration_service_host        = "registration-service"
  registration_service_url         = "http://${local.registration_service_host}:${local.registration_service_port}${local.registration_service_path_prefix}"

  dataspace_did_uri = "did:web:${azurerm_storage_account.dataspace_did.primary_web_host}"
  gaiax_did_uri     = "did:web:${azurerm_storage_account.gaiax_did.primary_web_host}"
}

resource "azurerm_resource_group" "dataspace" {
  name     = var.resource_group
  location = var.location
}

resource "azurerm_key_vault" "registrationservice" {
  // added `kv` prefix because the keyvault name needs to begin with a letter
  name                        = "kv${var.prefix}registration"
  location                    = var.location
  resource_group_name         = azurerm_resource_group.dataspace.name
  enabled_for_disk_encryption = false
  tenant_id                   = data.azurerm_client_config.current_client.tenant_id
  soft_delete_retention_days  = 7
  purge_protection_enabled    = false
  sku_name                    = "standard"
  enable_rbac_authorization   = true
}

# Role assignment so that the application may access the vault
resource "azurerm_role_assignment" "registrationservice_keyvault" {
  scope                = azurerm_key_vault.registrationservice.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = var.application_sp_object_id
}

# Role assignment so that the currently logged in user may add secrets to the vault
resource "azurerm_role_assignment" "current-user-secretsofficer" {
  scope                = azurerm_key_vault.registrationservice.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = data.azurerm_client_config.current_client.object_id
}

# Role assignment so that the currently logged in user may add keys to the vault
resource "azurerm_role_assignment" "current-user-cryptoofficer" {
  scope                = azurerm_key_vault.registrationservice.id
  role_definition_name = "Key Vault Crypto Officer"
  principal_id         = data.azurerm_client_config.current_client.object_id
}

resource "azurerm_storage_account" "shared" {
  name                     = "${var.prefix}${local.dataspace_authority_name}shared"
  resource_group_name      = azurerm_resource_group.dataspace.name
  location                 = azurerm_resource_group.dataspace.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
}

resource "azurerm_storage_share" "share" {
  name                 = "share"
  storage_account_name = azurerm_storage_account.shared.name
  quota                = 1
}