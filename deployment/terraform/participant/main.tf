terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">= 3.7.0"
    }
  }

  backend "azurerm" {
    use_oidc = true
  }
}

provider "azurerm" {
  use_oidc = true
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

resource "random_password" "apikey" {
  length = 16
}

locals {
  api_key              = random_password.apikey.result
  edc_resources_folder = "/resources"
}

data "azurerm_container_registry" "registry" {
  name                = var.acr_name
  resource_group_name = var.acr_resource_group
}

locals {
  registry_files_prefix = "${var.prefix}-"

  connector_id     = "urn:connector:${var.prefix}-${var.participant_name}"
  connector_name   = "connector-${var.participant_name}"
  connector_region = var.participant_region

  did_url = "did:web:${azurerm_storage_account.did.primary_web_host}"

  edc_dns_label       = "${var.prefix}-${var.participant_name}-edc-mvd"
  edc_default_port    = 8181
  edc_ids_port        = 8282
  edc_management_port = 9191
}

resource "azurerm_resource_group" "participant" {
  name     = var.resource_group
  location = var.location
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
      port     = local.edc_default_port
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

    volume {
      name       = "shared"
      mount_path = local.edc_resources_folder
      read_only  = true
      share_name = azurerm_storage_share.share.name

      storage_account_name = azurerm_storage_account.shared.name
      storage_account_key  = azurerm_storage_account.shared.primary_access_key
    }

    environment_variables = {
      EDC_IDS_ID         = local.connector_id
      EDC_CONNECTOR_NAME = local.connector_name

      EDC_VAULT_NAME     = azurerm_key_vault.participant.name
      EDC_VAULT_TENANTID = data.azurerm_client_config.current_client.tenant_id
      EDC_VAULT_CLIENTID = var.application_sp_client_id

      EDC_IDENTITY_DID_URL = local.did_url

      EDC_WEB_REST_CORS_ENABLED = "true"
      EDC_WEB_REST_CORS_HEADERS = "origin,content-type,accept,authorization,x-api-key"

      IDS_WEBHOOK_ADDRESS = "http://${local.edc_dns_label}.${var.location}.azurecontainer.io:${local.edc_ids_port}"

      REGISTRATION_SERVICE_API_URL = var.registration_service_api_url

      # Refresh catalog frequently to accelerate scenarios
      EDC_CATALOG_CACHE_EXECUTION_DELAY_SECONDS  = 10
      EDC_CATALOG_CACHE_EXECUTION_PERIOD_SECONDS = 10
      EDC_CATALOG_CACHE_PARTITION_NUM_CRAWLERS   = 10 // actual number will be limited by the number of work items


      APPLICATIONINSIGHTS_ROLE_NAME = local.connector_name

      EDC_SELF_DESCRIPTION_DOCUMENT_PATH = "${local.edc_resources_folder}/${azurerm_storage_share_file.sdd.name}"
    }

    secure_environment_variables = {
      EDC_VAULT_CLIENTSECRET = var.application_sp_client_secret

      EDC_API_AUTH_KEY = local.api_key

      APPLICATIONINSIGHTS_CONNECTION_STRING = var.app_insights_connection_string
    }

    liveness_probe {
      http_get {
        port = 8181
        path = "/api/check/health"
      }
      initial_delay_seconds = 10
      failure_threshold     = 6
      timeout_seconds       = 3
    }
  }
}

resource "azurerm_container_group" "webapp" {
  name                = "${var.prefix}-${var.participant_name}-webapp"
  location            = var.location
  resource_group_name = azurerm_resource_group.participant.name
  ip_address_type     = "Public"
  dns_name_label      = "${var.prefix}-${var.participant_name}-mvd"
  os_type             = "Linux"

  image_registry_credential {
    username = data.azurerm_container_registry.registry.admin_username
    password = data.azurerm_container_registry.registry.admin_password
    server   = data.azurerm_container_registry.registry.login_server
  }

  container {
    name   = "webapp"
    image  = "${data.azurerm_container_registry.registry.login_server}/${var.dashboard_image}"
    cpu    = 1
    memory = 1

    ports {
      port     = 80
      protocol = "TCP"
    }

    volume {
      name       = "appconfig"
      mount_path = "/usr/share/nginx/html/assets/config"
      secret = {
        "app.config.json" = base64encode(jsonencode({
          "theme"                       = var.data_dashboard_theme
          "dataManagementApiUrl"        = "http://${azurerm_container_group.edc.fqdn}:${local.edc_management_port}/api/v1/data"
          "catalogUrl"                  = "http://${azurerm_container_group.edc.fqdn}:${local.edc_default_port}/api/federatedcatalog"
          "apiKey"                      = local.api_key
          "storageAccount"              = azurerm_storage_account.inbox.name
          "storageExplorerLinkTemplate" = "storageexplorer://v=1&accountid=${azurerm_resource_group.participant.id}/providers/Microsoft.Storage/storageAccounts/{{account}}&subscriptionid=${data.azurerm_subscription.current_subscription.subscription_id}&resourcetype=Azure.BlobContainer&resourcename={{container}}",
        }))
      }
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
  source                 = "sample-data/text-document.txt"
}

resource "azurerm_storage_blob" "testfile2" {
  name                   = "text-document-2.txt"
  storage_account_name   = azurerm_storage_account.assets.name
  storage_container_name = azurerm_storage_container.assets_container.name
  type                   = "Block"
  source                 = "sample-data/text-document.txt"
}