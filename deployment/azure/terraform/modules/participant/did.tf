resource "azurerm_storage_account" "did" {
  name                     = "${var.prefix}${var.participant_name}did"
  resource_group_name      = azurerm_resource_group.participant.name
  location                 = azurerm_resource_group.participant.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
  static_website {}
}

resource "azurerm_storage_blob" "did" {
  name                 = ".well-known/did.json" # `.well-known` path is defined by did:web specification
  storage_account_name = azurerm_storage_account.did.name
  # Create did blob only if public_key_jwk_file is provided. Default public_key_jwk_file value is null.
  count                  = var.public_key_jwk_file == null ? 0 : 1
  storage_container_name = "$web"
  # container used to serve static files (see static_website property on storage account)
  type = "Block"
  source_content = jsonencode({
    id = local.did_url
    "@context" = [
      "https://www.w3.org/ns/did/v1",
      {
        "@base" = local.did_url
      }
    ],
    "service" : [
      {
        "id" : "#identity-hub-url",
        "type" : "IdentityHub",
        "serviceEndpoint" : "http://${var.participant_name}:${local.edc_identity_port}/api/identity/identity-hub"
      },
      {
        "id" : "#dsp-url",
        "type" : "DSPMessaging",
        "serviceEndpoint" : "http://${urlencode(var.participant_name)}:${local.edc_dsp_port}/api/dsp"
      },
      {
        "id" : "#self-description-url",
        "type" : "SelfDescription",
        "serviceEndpoint" : "http://${var.participant_name}:${local.edc_default_port}/api/identity/identity-hub/self-description"
      }
    ],
    "verificationMethod" = [
      {
        "id"           = "#identity-key-1"
        "controller"   = ""
        "type"         = "JsonWebKey2020"
        "publicKeyJwk" = jsondecode(file(var.public_key_jwk_file))
      }
    ],
    "authentication" : [
      "#identity-key-1"
    ]
  })
  content_type = "application/json"
}