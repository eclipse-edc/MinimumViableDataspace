output "ARM_CLIENT_ID" {
  value = azuread_application.gh-actions-mvd.application_id
}

output "ARM_SUBSCRIPTION_ID" {
  value = data.azurerm_subscription.primary.subscription_id
}

output "ARM_TENANT_ID" {
  value = var.tenant_id
}

output "APP_CLIENT_ID" {
  value = azuread_application.mvd-runtimes.application_id
}

output "APP_OBJECT_ID" {
  value = azuread_service_principal.mvd-runtimes-sp.object_id
}
output "APP_CLIENT_SECRET" {
  sensitive = true
  value     = azuread_application_password.mvd-runtimes-sp-password.value
}

output "COMMON_RESOURCE_GROUP" {
  value = var.common_resourcegroup
}

output "COMMON_RESOURCE_GROUP_LOCATION" {
  value = var.common_resourcegroup_location
}

output "TERRAFORM_STATE_CONTAINER" {
  value = var.tf_state_container
}

output "TERRAFORM_STATE_STORAGE_ACCOUNT" {
  value = var.tf_state_storageaccount
}

output "GH_REPO" {
  value = var.github_repo
}

output "ARM_CLIENT_SECRET" {
  sensitive = true
  value     = azuread_application_password.gh-actions-mvd-pwd.value
}