resource "azurerm_key_vault_secret" "asset_storage_accesskey" {
  key_vault_id = azurerm_key_vault.participant.id
  name         = "${azurerm_storage_account.assets.name}-key1"
  value        = azurerm_storage_account.assets.primary_access_key
  depends_on   = [azurerm_role_assignment.current-user-secretsofficer]
}

resource "azurerm_key_vault_secret" "inbox_storage_accesskey" {
  key_vault_id = azurerm_key_vault.participant.id
  name         = "${azurerm_storage_account.inbox.name}-key1"
  value        = azurerm_storage_account.inbox.primary_access_key
  depends_on   = [azurerm_role_assignment.current-user-secretsofficer]
}

resource "azurerm_key_vault_secret" "private-key" {
  key_vault_id = azurerm_key_vault.participant.id
  name         = local.connector_name
  value        = file(var.private_key_pem_file)
  depends_on   = [azurerm_role_assignment.current-user-secretsofficer]
}