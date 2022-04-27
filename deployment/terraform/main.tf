terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">= 3.1.0"
    }
  }

  backend "azurerm" {}
}

provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy = true
    }
  }
}

data "azurerm_subscription" "current_subscription" {
}

data "azurerm_client_config" "current_client" {
}

resource "azurerm_resource_group" "participant" {
  name     = var.resource_group
  location = var.location
}

data "azurerm_container_registry" "registry" {
  name                = var.acr_name
  resource_group_name = var.acr_resource_group
}

locals {
  edc_dns_label       = "${var.prefix}-${var.participant_name}-edc-mvd"
  edc_control_port    = 8181
  edc_ids_port        = 8282
  edc_management_port = 9191
}

resource "azurerm_container_group" "edc" {
  name                = "${var.prefix}-${var.participant_name}-edc"
  location            = var.location
  resource_group_name = azurerm_resource_group.participant.name
  ip_address_type     = "Public"
  dns_name_label      = local.edc_dns_label
  os_type             = "Linux"

  image_registry_credential {
    username = data.azurerm_container_registry.registry.admin_username
    password = data.azurerm_container_registry.registry.admin_password
    server   = data.azurerm_container_registry.registry.login_server
  }

  container {
    name   = "edc"
    image  = "${data.azurerm_container_registry.registry.login_server}/${var.runtime_image}"
    cpu    = var.container_cpu
    memory = var.container_memory

    ports {
      port     = local.edc_control_port
      protocol = "TCP"
    }
    ports {
      port     = local.edc_ids_port
      protocol = "TCP"
    }
    ports {
      port     = local.edc_management_port
      protocol = "TCP"
    }
    environment_variables = {
      EDC_IDS_ID             = "urn:connector:${var.prefix}-${var.participant_name}"
      EDC_VAULT_NAME         = azurerm_key_vault.participant.name
      EDC_VAULT_TENANTID     = data.azurerm_client_config.current_client.tenant_id
      EDC_VAULT_CLIENTID     = var.application_sp_client_id
      EDC_VAULT_CLIENTSECRET = var.application_sp_client_secret
      IDS_WEBHOOK_ADDRESS    = "http://${local.edc_dns_label}.${var.location}.azurecontainer.io:${local.edc_ids_port}"
    }
  }
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
resource "azurerm_role_assignment" "edc_keyvault" {
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

resource "azurerm_storage_account" "assets" {
  name                     = "${var.prefix}${var.participant_name}assets"
  resource_group_name      = azurerm_resource_group.participant.name
  location                 = azurerm_resource_group.participant.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
}

resource "azurerm_storage_account" "did" {
  name                     = "${var.prefix}${var.participant_name}did"
  resource_group_name      = azurerm_resource_group.participant.name
  location                 = azurerm_resource_group.participant.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
  static_website {}
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
  source                 = "sample-data/text-document.txt"
}

resource "azurerm_key_vault_secret" "asset_storage_key" {
  name         = "${azurerm_storage_account.assets.name}-key1"
  value        = azurerm_storage_account.assets.primary_access_key
  key_vault_id = azurerm_key_vault.participant.id
  depends_on = [
    azurerm_role_assignment.current-user-secretsofficer
  ]
}

resource "azurerm_key_vault_secret" "did_key" {
  name = var.participant_name
  # Create did_key secret only if key_file value is provided. Default key_file value is null.
  count        = var.key_file == null ? 0 : 1
  value        = file(var.key_file)
  key_vault_id = azurerm_key_vault.participant.id
  depends_on = [
    azurerm_role_assignment.current-user-secretsofficer
  ]
}

resource "azurerm_storage_blob" "did" {
  name                 = "did.json"
  storage_account_name = azurerm_storage_account.did.name
  # Create did blob only if public_key_jwk_file is provided. Default public_key_jwk_file value is null.
  count                  = var.public_key_jwk_file == null ? 0 : 1
  storage_container_name = "$web"
  type                   = "Block"
  source_content = jsonencode({
    id = "did:web:${azurerm_storage_account.did.primary_web_host}:identity",
    "@context" = ["https://www.w3.org/ns/did/v1",
      {
        "@base" = "did:web:${azurerm_storage_account.did.primary_web_host}:identity"
      }
    ],
    "verificationMethod" = [
      {
        "id"           = "#identity-key-1",
        "controller"   = "",
        "type"         = "JsonWebKey2020",
        "publicKeyJwk" = jsondecode(file(var.public_key_jwk_file))
      }
    ],
    "authentication" : [
      "#identity-key-1"
  ] })
  content_type = "application/json"
}
