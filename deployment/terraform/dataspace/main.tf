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
    // When deleting App Insights, resources related to microsoft.alertsmanagement remain
    resource_group {
      prevent_deletion_if_contains_resources = false
    }
  }
}

locals {
  edc_resources_folder     = "/resources"
  dataspace_authority_name = "authority"
}

data "azurerm_subscription" "current_subscription" {
}

data "azurerm_client_config" "current_client" {
}

data "azurerm_container_registry" "registrationservice" {
  name                = var.acr_name
  resource_group_name = var.acr_resource_group
}

locals {
  registry_files_prefix = "${var.prefix}-"

  connector_name = "connector-registration"

  registration_service_dns_label   = "${var.prefix}-registration-mvd"
  edc_default_port                 = 8181
  registration_service_port        = 8182
  registration_service_path_prefix = "/authority"
  registration_service_host        = "${local.registration_service_dns_label}.${var.location}.azurecontainer.io"
  registration_service_url         = "http://${local.registration_service_host}:${local.registration_service_port}${local.registration_service_path_prefix}"

  dataspace_did_uri = "did:web:${azurerm_storage_account.dataspace_did.primary_web_host}"
  gaiax_did_uri     = "did:web:${azurerm_storage_account.gaiax_did.primary_web_host}"
}

resource "azurerm_resource_group" "dataspace" {
  name     = var.resource_group
  location = var.location
}

resource "azurerm_application_insights" "dataspace" {
  name                = "${var.prefix}-appinsights"
  location            = var.location
  resource_group_name = azurerm_resource_group.dataspace.name
  application_type    = "java"
}

resource "azurerm_container_group" "registration-service" {
  name                = "${var.prefix}-registration-mvd"
  location            = var.location
  resource_group_name = azurerm_resource_group.dataspace.name
  ip_address_type     = "Public"
  dns_name_label      = local.registration_service_dns_label
  os_type             = "Linux"

  image_registry_credential {
    username = data.azurerm_container_registry.registrationservice.admin_username
    password = data.azurerm_container_registry.registrationservice.admin_password
    server   = data.azurerm_container_registry.registrationservice.login_server
  }

  container {
    name   = "registration-service"
    image  = "${data.azurerm_container_registry.registrationservice.login_server}/${var.registrationservice_runtime_image}"
    cpu    = var.container_cpu
    memory = var.container_memory

    ports {
      port     = local.edc_default_port
      protocol = "TCP"
    }

    ports {
      port     = local.registration_service_port
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
      EDC_CONNECTOR_NAME = local.connector_name

      EDC_VAULT_NAME     = azurerm_key_vault.registrationservice.name
      EDC_VAULT_TENANTID = data.azurerm_client_config.current_client.tenant_id
      EDC_VAULT_CLIENTID = var.application_sp_client_id

      EDC_IDENTITY_DID_URL = local.dataspace_did_uri

      JWT_AUDIENCE                  = local.registration_service_url
      WEB_HTTP_AUTHORITY_PORT       = local.registration_service_port
      WEB_HTTP_AUTHORITY_PATH       = local.registration_service_path_prefix
      APPLICATIONINSIGHTS_ROLE_NAME = local.connector_name

      EDC_SELF_DESCRIPTION_DOCUMENT_PATH = "${local.edc_resources_folder}/${azurerm_storage_share_file.sdd.name}"
    }

    secure_environment_variables = {
      APPLICATIONINSIGHTS_CONNECTION_STRING = azurerm_application_insights.dataspace.connection_string
      EDC_VAULT_CLIENTSECRET                = var.application_sp_client_secret
    }

    liveness_probe {
      http_get {
        port = local.edc_default_port
        path = "/api/check/health"
      }
      initial_delay_seconds = 10
      failure_threshold     = 6
      timeout_seconds       = 3
    }
  }
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