output "app_insights_connection_string" {
  value     = azurerm_application_insights.dataspace.connection_string
  sensitive = true
}

output "registry_host" {
  value = azurerm_container_group.registry-service.fqdn
}