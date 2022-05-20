output "edc_host" {
  value = azurerm_container_group.edc.fqdn
}

output "assets_storage_account" {
  value = azurerm_storage_account.assets.name
}

output "key_vault" {
  value = azurerm_key_vault.participant.name
}

output "connector_name" {
  value = local.connector_name
}

output "did_host" {
  value = length(azurerm_storage_blob.did) > 0 ? azurerm_storage_account.did.primary_web_host : null
}

output "edc_aci_name" {
  value = azurerm_container_group.edc.name
}

output "resource_group" {
  value = azurerm_container_group.edc.resource_group_name
}

output "webapp_url" {
  value = "http://${azurerm_container_group.webapp.fqdn}"
}

output "api_key" {
  value     = local.api_key
  sensitive = true
}
