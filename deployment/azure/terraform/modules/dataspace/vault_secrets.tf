resource "azurerm_key_vault_secret" "private-key" {
  key_vault_id = azurerm_key_vault.registrationservice.id
  name         = local.connector_name
  value        = file(var.private_key_pem_file)
  depends_on   = [azurerm_role_assignment.current-user-secretsofficer]
}