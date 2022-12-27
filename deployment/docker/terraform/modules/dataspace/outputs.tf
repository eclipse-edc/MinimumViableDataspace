output "connector_name" {
  value = local.connector_name
}

output "key_vault" {
  value = azurerm_key_vault.registrationservice.name
}

output "registration_service_host" {
  value = local.registration_service_host
}

output "registration_service_url" {
  value = local.registration_service_url
}

output "dataspace_did_host" {
  value = length(azurerm_storage_blob.dataspace_did) > 0 ? azurerm_storage_account.dataspace_did.primary_web_host : null
}

output "gaiax_did_host" {
  value = length(azurerm_storage_blob.gaiax_did) > 0 ? azurerm_storage_account.gaiax_did.primary_web_host : null
}
output "authority-sdd-file" {
  value = "${path.module}/build/${local.dataspace_authority_name}-sdd.json"
}