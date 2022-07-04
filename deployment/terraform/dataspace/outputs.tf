output "app_insights_connection_string" {
  value     = azurerm_application_insights.dataspace.connection_string
  sensitive = true
}

output "registration_service_url" {
  value = "http://${azurerm_container_group.registration-service.fqdn}:${local.edc_default_port}"
}

output "authority_did_host" {
  value = length(azurerm_storage_blob.authority_did) > 0 ? azurerm_storage_account.authority_did.primary_web_host : null
}

output "gaiax_did_host" {
  value = length(azurerm_storage_blob.gaiax_did) > 0 ? azurerm_storage_account.gaiax_did.primary_web_host : null
}
