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

data "azurerm_container_registry" "registry" {
  name                = var.acr_name
  resource_group_name = var.acr_resource_group
}

data "azurerm_storage_account" "registry" {
  name                = var.registry_storage_account
  resource_group_name = var.registry_resource_group
}

data "azurerm_storage_share" "registry" {
  name                 = var.registry_share
  storage_account_name = data.azurerm_storage_account.registry.name
}

locals {
  registry_files_prefix = "${var.prefix}-"

  connector_name = "connector-registry"

  registry_service_dns_label = "${var.prefix}-registry-mvd"
  edc_default_port           = 8181
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

resource "azurerm_container_group" "registry-service" {
  name                = "${var.prefix}-registry"
  location            = var.location
  resource_group_name = azurerm_resource_group.dataspace.name
  ip_address_type     = "Public"
  dns_name_label      = local.registry_service_dns_label
  os_type             = "Linux"

  image_registry_credential {
    username = data.azurerm_container_registry.registry.admin_username
    password = data.azurerm_container_registry.registry.admin_password
    server   = data.azurerm_container_registry.registry.login_server
  }

  container {
    name   = "registry-service"
    image  = "${data.azurerm_container_registry.registry.login_server}/${var.registry_runtime_image}"
    cpu    = var.container_cpu
    memory = var.container_memory

    ports {
      port     = local.edc_default_port
      protocol = "TCP"
    }

    environment_variables = {
      EDC_CONNECTOR_NAME = local.connector_name

      NODES_JSON_DIR          = "/registry"
      NODES_JSON_FILES_PREFIX = local.registry_files_prefix
    }

    volume {
      storage_account_name = data.azurerm_storage_account.registry.name
      storage_account_key  = data.azurerm_storage_account.registry.primary_access_key
      share_name           = data.azurerm_storage_share.registry.name
      mount_path           = "/registry"
      name                 = "registry"
    }

    liveness_probe {
      http_get {
        port = 8181
        path = "/api/check/health"
      }
    }
  }
}

