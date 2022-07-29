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
    // When deleting App Insights, resources related to microsoft.alertsmanagement remain
    resource_group {
      prevent_deletion_if_contains_resources = false
    }
  }
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
  registration_service_url         = "http://${local.registration_service_dns_label}.${var.location}.azurecontainer.io:${local.registration_service_port}${local.registration_service_path_prefix}"

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
      port     = local.registration_service_port
      protocol = "TCP"
    }

    environment_variables = {
      EDC_CONNECTOR_NAME      = local.connector_name
      JWT_AUDIENCE            = local.registration_service_url
      WEB_HTTP_AUTHORITY_PORT = local.registration_service_port
      WEB_HTTP_AUTHORITY_PATH = local.registration_service_path_prefix
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

# Internal Dataspace Authority resources (Dataspace DID)
resource "azurerm_storage_account" "dataspace_did" {
  name                     = "${var.prefix}dataspacedid"
  resource_group_name      = azurerm_resource_group.dataspace.name
  location                 = var.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
  static_website {}
}

resource "azurerm_storage_blob" "dataspace_did" {
  name                 = ".well-known/did.json" # `.well-known` path is defined by did:web specification
  storage_account_name = azurerm_storage_account.dataspace_did.name
  # Create did blob only if public_key_jwk_file is provided. Default public_key_jwk_file value is null.
  count                  = var.public_key_jwk_file_authority == null ? 0 : 1
  storage_container_name = "$web" # container used to serve static files (see static_website property on storage account)
  type                   = "Block"
  source_content = jsonencode({
    id = local.dataspace_did_uri
    "@context" = [
      "https://www.w3.org/ns/did/v1",
      {
        "@base" = local.dataspace_did_uri
      }
    ],
    "service" : [
      {
        "id" : "#registration-url",
        "type" : "RegistrationUrl",
        "serviceEndpoint" : local.registration_service_url
      }
    ],
    "verificationMethod" = [
      {
        "id"           = "#identity-key-authority"
        "controller"   = ""
        "type"         = "JsonWebKey2020"
        "publicKeyJwk" = jsondecode(file(var.public_key_jwk_file_authority))
      }
    ],
    "authentication" : [
      "#identity-key-authority"
  ] })
  content_type = "application/json"
}

# GAIA-X Authority resources
resource "azurerm_storage_account" "gaiax_did" {
  name                     = "${var.prefix}gaiaxdid"
  resource_group_name      = azurerm_resource_group.dataspace.name
  location                 = var.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
  static_website {}
}

resource "azurerm_storage_blob" "gaiax_did" {
  name                 = ".well-known/did.json" # `.well-known` path is defined by did:web specification
  storage_account_name = azurerm_storage_account.gaiax_did.name
  # Create did blob only if public_key_jwk_file is provided. Default public_key_jwk_file value is null.
  count                  = var.public_key_jwk_file_gaiax == null ? 0 : 1
  storage_container_name = "$web" # container used to serve static files (see static_website property on storage account)
  type                   = "Block"
  source_content = jsonencode({
    id = local.gaiax_did_uri
    "@context" = [
      "https://www.w3.org/ns/did/v1",
      {
        "@base" = local.gaiax_did_uri
      }
    ],
    "verificationMethod" = [
      {
        "id"           = "#identity-key-gaiax"
        "controller"   = ""
        "type"         = "JsonWebKey2020"
        "publicKeyJwk" = jsondecode(file(var.public_key_jwk_file_gaiax))
      }
    ],
    "authentication" : [
      "#identity-key-gaiax"
  ] })
  content_type = "application/json"
}

