# Configure Terraform
terraform {
  required_providers {
    azuread = {
      source  = "hashicorp/azuread"
      version = ">=2.41.0"
    }
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">=3.71.0"
    }
    github = {
      source  = "integrations/github"
      version = ">=5.34.0"
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
