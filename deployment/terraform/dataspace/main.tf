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

