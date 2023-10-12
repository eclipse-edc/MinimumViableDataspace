

data "azurerm_subscription" "current_subscription" {
}

data "azurerm_client_config" "current_client" {
}

locals {
  api_key              = var.api_key //random_password.apikey.result
  edc_resources_folder = "/resources"
}

locals {
  registry_files_prefix = "${var.prefix}-"

  connector_id     = "urn:connector:${var.prefix}-${var.participant_name}"
  connector_name   = "connector-${var.participant_name}"
  connector_region = var.participant_region

  did_url = "did:web:${azurerm_storage_account.did.primary_web_host}"

  edc_dns_label       = "${var.prefix}-${var.participant_name}-edc-mvd"
  edc_default_port    = 8181
  edc_dsp_port        = 8282
  edc_identity_port   = 7171
  edc_management_port = 9191
}

resource "azurerm_resource_group" "participant" {
  name     = var.resource_group
  location = var.location
}

resource "azurerm_key_vault" "participant" {
  // added `kv` prefix because the keyvault name needs to begin with a letter
  name                        = "kv${var.prefix}${var.participant_name}"
  location                    = azurerm_resource_group.participant.location
  resource_group_name         = azurerm_resource_group.participant.name
  enabled_for_disk_encryption = false
  tenant_id                   = data.azurerm_client_config.current_client.tenant_id
  soft_delete_retention_days  = 7
  purge_protection_enabled    = false
  sku_name                    = "standard"
  enable_rbac_authorization   = true
}

# Role assignment so that the application may access the vault
resource "azurerm_role_assignment" "participant_secretsofficer" {
  scope                = azurerm_key_vault.participant.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = var.application_sp_object_id
}

# Role assignment so that the currently logged in user may add secrets to the vault
resource "azurerm_role_assignment" "current-user-secretsofficer" {
  scope                = azurerm_key_vault.participant.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = data.azurerm_client_config.current_client.object_id
}

# Role assignment so that the currently logged in user may add keys to the vault
resource "azurerm_role_assignment" "current-user-cryptoofficer" {
  scope                = azurerm_key_vault.participant.id
  role_definition_name = "Key Vault Crypto Officer"
  principal_id         = data.azurerm_client_config.current_client.object_id
}

resource "azurerm_storage_account" "assets" {
  name                     = "${var.prefix}${var.participant_name}assets"
  resource_group_name      = azurerm_resource_group.participant.name
  location                 = azurerm_resource_group.participant.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
}

resource "azurerm_storage_account" "inbox" {
  name                     = "${var.prefix}${var.participant_name}inbox"
  resource_group_name      = azurerm_resource_group.participant.name
  location                 = azurerm_resource_group.participant.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
}

resource "azurerm_storage_account" "shared" {
  name                     = "${var.prefix}${var.participant_name}shared"
  resource_group_name      = azurerm_resource_group.participant.name
  location                 = azurerm_resource_group.participant.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
}

resource "azurerm_storage_share" "share" {
  name                 = "share"
  storage_account_name = azurerm_storage_account.shared.name
  quota                = 1
}

resource "azurerm_storage_container" "assets_container" {
  name                 = "src-container"
  storage_account_name = azurerm_storage_account.assets.name
}

resource "azurerm_storage_blob" "testfile" {
  name                   = "text-document.txt"
  storage_account_name   = azurerm_storage_account.assets.name
  storage_container_name = azurerm_storage_container.assets_container.name
  type                   = "Block"
  source                 = "${path.module}/sample-data/text-document.txt"
}

resource "azurerm_storage_blob" "testfile2" {
  name                   = "text-document-2.txt"
  storage_account_name   = azurerm_storage_account.assets.name
  storage_container_name = azurerm_storage_container.assets_container.name
  type                   = "Block"
  source                 = "${path.module}/sample-data/text-document.txt"
}