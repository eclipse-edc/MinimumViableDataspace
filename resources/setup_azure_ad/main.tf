# Configure Terraform
terraform {
  required_providers {
    azuread = {
      source  = "hashicorp/azuread"
      version = ">=2.26.1"
    }
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">=3.16.0"
    }
    github = {
      source  = "integrations/github"
      version = ">=4.28.0"
    }
  }
}
provider "azurerm" {
  features {
  }
}
provider "azuread" {
  tenant_id = var.tenant_id
}

provider "github" {

}

data "azuread_client_config" "current" {
}

data "azurerm_subscription" "primary" {
}
