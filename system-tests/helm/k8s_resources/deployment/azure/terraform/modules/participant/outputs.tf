output "assets_storage_account" {
  value = azurerm_storage_account.assets.name
}

output "assets_storage_account_key" {
  value     = azurerm_storage_account.assets.primary_access_key
  sensitive = true
}

output "inbox_storage_account" {
  value = azurerm_storage_account.inbox.name
}

output "inbox_storage_account_key" {
  value     = azurerm_storage_account.inbox.primary_access_key
  sensitive = true
}

output "key_vault" {
  value = azurerm_key_vault.participant.name
}

output "connector_name" {
  value = local.connector_name
}

output "participant_did_host" {
  value = length(azurerm_storage_blob.did) > 0 ? azurerm_storage_account.did.primary_web_host : null
}

output "resource_group" {
  value = azurerm_resource_group.participant.name
}

output "api_key" {
  value     = local.api_key
  sensitive = true
}

output "sdd_file" {
  value = local_file.sdd
}