# GAIA-X Authority resources
resource "azurerm_storage_account" "gaiax_did" {
  name                     = "${var.prefix}gaiaxdid"
  resource_group_name      = azurerm_resource_group.dataspace.name
  location                 = var.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
  static_website {}
}

resource "azurerm_storage_blob" "gaiax_did" {
  name                 = ".well-known/did.json" # `.well-known` path is defined by did:web specification
  storage_account_name = azurerm_storage_account.gaiax_did.name
  # Create did blob only if public_key_jwk_file is provided. Default public_key_jwk_file value is null.
  count                  = var.public_key_jwk_file_gaiax == null ? 0 : 1
  storage_container_name = "$web" # container used to serve static files (see static_website property on storage account)
  type                   = "Block"
  source_content = jsonencode({
    id = local.gaiax_did_uri
    "@context" = [
      "https://www.w3.org/ns/did/v1",
      {
        "@base" = local.gaiax_did_uri
      }
    ],
    "service" : [],
    "verificationMethod" = [
      {
        "id"           = "#identity-key-gaiax"
        "controller"   = ""
        "type"         = "JsonWebKey2020"
        "publicKeyJwk" = jsondecode(file(var.public_key_jwk_file_gaiax))
      }
    ],
    "authentication" : [
      "#identity-key-gaiax"
  ] })
  content_type = "application/json"
}

