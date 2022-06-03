output "app_insights_connection_string" {
  value     = azurerm_application_insights.dataspace.connection_string
  sensitive = true
}
