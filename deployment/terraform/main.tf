terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">= 3.1.0"
    }
  }
}

provider "azurerm" {
  features {}
}

resource "azurerm_resource_group" "participant" {
  name     = var.resource_group
  location = var.location
}

data "azurerm_container_registry" "registry" {
  name                = var.acr_name
  resource_group_name = var.acr_resource_group
}

resource "azurerm_container_group" "edc" {
  name                = "${var.prefix}-${var.participant_name}-edc"
  location            = var.location
  resource_group_name = azurerm_resource_group.participant.name
  ip_address_type     = "Public"
  dns_name_label      = "${var.prefix}-${var.participant_name}-edc-mvd"
  os_type             = "Linux"

  image_registry_credential {
    username = data.azurerm_container_registry.registry.admin_username
    password = data.azurerm_container_registry.registry.admin_password
    server   = data.azurerm_container_registry.registry.login_server
  }

  container {
    name   = "${var.prefix}-${var.participant_name}-edc"
    image  = "${data.azurerm_container_registry.registry.login_server}/${var.runtime_image}"
    cpu    = var.container_cpu
    memory = var.container_memory

    ports {
      port     = 8181
      protocol = "TCP"
    }
    ports {
      port     = 9191
      protocol = "TCP"
    }
  }
}
