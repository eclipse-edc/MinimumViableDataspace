# Internal Dataspace Authority resources (Dataspace DID)
resource "azurerm_storage_account" "dataspace_did" {
  name                     = "${var.prefix}dataspacedid"
  resource_group_name      = azurerm_resource_group.dataspace.name
  location                 = var.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
  static_website {}
}

resource "azurerm_storage_blob" "dataspace_did" {
  name                 = ".well-known/did.json" # `.well-known` path is defined by did:web specification
  storage_account_name = azurerm_storage_account.dataspace_did.name
  # Create did blob only if public_key_jwk_file is provided. Default public_key_jwk_file value is null.
  count                  = var.public_key_jwk_file_authority == null ? 0 : 1
  storage_container_name = "$web" # container used to serve static files (see static_website property on storage account)
  type                   = "Block"
  source_content = jsonencode({
    id = local.dataspace_did_uri
    "@context" = [
      "https://www.w3.org/ns/did/v1",
      {
        "@base" = local.dataspace_did_uri
      }
    ],
    "service" : [
      {
        "id" : "#registration-url",
        "type" : "RegistrationUrl",
        "serviceEndpoint" : local.registration_service_url
      },
      {
        "id" : "#self-description-url",
        "type" : "SelfDescription",
        "serviceEndpoint" : "http://${local.registration_service_host}:${local.edc_default_port}/api/identity-hub/self-description"
      }
    ],
    "verificationMethod" = [
      {
        "id"           = "#identity-key-authority"
        "controller"   = ""
        "type"         = "JsonWebKey2020"
        "publicKeyJwk" = jsondecode(file(var.public_key_jwk_file_authority))
      }
    ],
    "authentication" : [
      "#identity-key-authority"
  ] })
  content_type = "application/json"
}

