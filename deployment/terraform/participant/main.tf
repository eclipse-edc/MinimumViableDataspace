terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">= 3.1.0"
    }
  }

  backend "azurerm" {}
}

provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy = true
    }
  }
}

data "azurerm_subscription" "current_subscription" {
}

data "azurerm_client_config" "current_client" {
}

resource "random_password" "apikey" {
  length = 16
}

locals {
  api_key              = random_password.apikey.result
  edc_resources_folder = "/resources"
}

data "azurerm_container_registry" "registry" {
  name                = var.acr_name
  resource_group_name = var.acr_resource_group
}

locals {
  registry_files_prefix = "${var.prefix}-"

  connector_id     = "urn:connector:${var.prefix}-${var.participant_name}"
  connector_name   = "connector-${var.participant_name}"
  connector_region = var.participant_region

  did_url = "did:web:${azurerm_storage_account.did.primary_web_host}"

  edc_dns_label       = "${var.prefix}-${var.participant_name}-edc-mvd"
  edc_default_port    = 8181
  edc_ids_port        = 8282
  edc_management_port = 9191
}

resource "azurerm_resource_group" "participant" {
  name     = var.resource_group
  location = var.location
}

resource "azurerm_container_group" "edc" {
  name                = "${var.prefix}-${var.participant_name}-edc"
  location            = var.location
  resource_group_name = azurerm_resource_group.participant.name
  ip_address_type     = "Public"
  dns_name_label      = local.edc_dns_label
  os_type             = "Linux"

  image_registry_credential {
    username = data.azurerm_container_registry.registry.admin_username
    password = data.azurerm_container_registry.registry.admin_password
    server   = data.azurerm_container_registry.registry.login_server
  }

  container {
    name   = "edc"
    image  = "${data.azurerm_container_registry.registry.login_server}/${var.runtime_image}"
    cpu    = var.container_cpu
    memory = var.container_memory

    ports {
      port     = local.edc_default_port
      protocol = "TCP"
    }

    ports {
      port     = local.edc_ids_port
      protocol = "TCP"
    }

    ports {
      port     = local.edc_management_port
      protocol = "TCP"
    }

    volume {
      name       = "shared"
      mount_path = local.edc_resources_folder
      read_only  = true
      share_name = azurerm_storage_share.share.name

      storage_account_name = azurerm_storage_account.shared.name
      storage_account_key  = azurerm_storage_account.shared.primary_access_key
    }

    environment_variables = {
      EDC_IDS_ID         = local.connector_id
      EDC_CONNECTOR_NAME = local.connector_name

      EDC_VAULT_NAME     = azurerm_key_vault.participant.name
      EDC_VAULT_TENANTID = data.azurerm_client_config.current_client.tenant_id
      EDC_VAULT_CLIENTID = var.application_sp_client_id

      EDC_IDENTITY_DID_URL = local.did_url

      EDC_WEB_REST_CORS_ENABLED = "true"
      EDC_WEB_REST_CORS_HEADERS = "origin,content-type,accept,authorization,x-api-key"

      IDS_WEBHOOK_ADDRESS = "http://${local.edc_dns_label}.${var.location}.azurecontainer.io:${local.edc_ids_port}"

      REGISTRATION_SERVICE_API_URL = var.registration_service_api_url

      # Refresh catalog frequently to accelerate scenarios
      EDC_CATALOG_CACHE_EXECUTION_DELAY_SECONDS  = 10
      EDC_CATALOG_CACHE_EXECUTION_PERIOD_SECONDS = 10

      APPLICATIONINSIGHTS_ROLE_NAME = local.connector_name

      EDC_SELF_DESCRIPTION_DOCUMENT_PATH = "${local.edc_resources_folder}/${azurerm_storage_share_file.sdd.name}"
    }

    secure_environment_variables = {
      EDC_VAULT_CLIENTSECRET = var.application_sp_client_secret

      EDC_API_AUTH_KEY = local.api_key

      APPLICATIONINSIGHTS_CONNECTION_STRING = var.app_insights_connection_string
    }

    liveness_probe {
      http_get {
        port = 8181
        path = "/api/check/health"
      }
      initial_delay_seconds = 10
      failure_threshold     = 6
      timeout_seconds       = 3
    }
  }
}

resource "azurerm_container_group" "webapp" {
  name                = "${var.prefix}-${var.participant_name}-webapp"
  location            = var.location
  resource_group_name = azurerm_resource_group.participant.name
  ip_address_type     = "Public"
  dns_name_label      = "${var.prefix}-${var.participant_name}-mvd"
  os_type             = "Linux"

  image_registry_credential {
    username = data.azurerm_container_registry.registry.admin_username
    password = data.azurerm_container_registry.registry.admin_password
    server   = data.azurerm_container_registry.registry.login_server
  }

  container {
    name   = "webapp"
    image  = "${data.azurerm_container_registry.registry.login_server}/${var.dashboard_image}"
    cpu    = 1
    memory = 1

    ports {
      port     = 80
      protocol = "TCP"
    }

    volume {
      name       = "appconfig"
      mount_path = "/usr/share/nginx/html/assets/config"
      secret = {
        "app.config.json" = base64encode(jsonencode({
          "theme"                       = var.data_dashboard_theme
          "dataManagementApiUrl"        = "http://${azurerm_container_group.edc.fqdn}:${local.edc_management_port}/api/v1/data"
          "catalogUrl"                  = "http://${azurerm_container_group.edc.fqdn}:${local.edc_default_port}/api/federatedcatalog"
          "apiKey"                      = local.api_key
          "storageAccount"              = azurerm_storage_account.inbox.name
          "storageExplorerLinkTemplate" = "storageexplorer://v=1&accountid=${azurerm_resource_group.participant.id}/providers/Microsoft.Storage/storageAccounts/{{account}}&subscriptionid=${data.azurerm_subscription.current_subscription.subscription_id}&resourcetype=Azure.BlobContainer&resourcename={{container}}",
        }))
      }
    }
  }
}

resource "azurerm_key_vault" "participant" {
  // added `kv` prefix because the keyvault name needs to begin with a letter
  name                        = "kv${var.prefix}${var.participant_name}"
  location                    = azurerm_resource_group.participant.location
  resource_group_name         = azurerm_resource_group.participant.name
  enabled_for_disk_encryption = false
  tenant_id                   = data.azurerm_client_config.current_client.tenant_id
  soft_delete_retention_days  = 7
  purge_protection_enabled    = false
  sku_name                    = "standard"
  enable_rbac_authorization   = true
}

# Role assignment so that the application may access the vault
resource "azurerm_role_assignment" "edc_keyvault" {
  scope                = azurerm_key_vault.participant.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = var.application_sp_object_id
}

# Role assignment so that the currently logged in user may add secrets to the vault
resource "azurerm_role_assignment" "current-user-secretsofficer" {
  scope                = azurerm_key_vault.participant.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = data.azurerm_client_config.current_client.object_id
}

resource "azurerm_storage_account" "assets" {
  name                     = "${var.prefix}${var.participant_name}assets"
  resource_group_name      = azurerm_resource_group.participant.name
  location                 = azurerm_resource_group.participant.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
}

resource "azurerm_storage_account" "did" {
  name                     = "${var.prefix}${var.participant_name}did"
  resource_group_name      = azurerm_resource_group.participant.name
  location                 = azurerm_resource_group.participant.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
  static_website {}
}

resource "azurerm_storage_account" "inbox" {
  name                     = "${var.prefix}${var.participant_name}inbox"
  resource_group_name      = azurerm_resource_group.participant.name
  location                 = azurerm_resource_group.participant.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
}

resource "azurerm_storage_account" "shared" {
  name                     = "${var.prefix}${var.participant_name}shared"
  resource_group_name      = azurerm_resource_group.participant.name
  location                 = azurerm_resource_group.participant.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
}

resource "azurerm_storage_share" "share" {
  name                 = "share"
  storage_account_name = azurerm_storage_account.shared.name
  quota                = 1
}

resource "azurerm_storage_container" "assets_container" {
  name                 = "src-container"
  storage_account_name = azurerm_storage_account.assets.name
}

resource "azurerm_storage_blob" "testfile" {
  name                   = "text-document.txt"
  storage_account_name   = azurerm_storage_account.assets.name
  storage_container_name = azurerm_storage_container.assets_container.name
  type                   = "Block"
  source                 = "sample-data/text-document.txt"
}

resource "azurerm_storage_blob" "testfile2" {
  name                   = "text-document-2.txt"
  storage_account_name   = azurerm_storage_account.assets.name
  storage_container_name = azurerm_storage_container.assets_container.name
  type                   = "Block"
  source                 = "sample-data/text-document.txt"
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
        "serviceEndpoint" : "http://${azurerm_container_group.edc.fqdn}:${local.edc_default_port}/api/identity-hub"
      },
      {
        "id" : "#ids-url",
        "type" : "IDSMessaging",
        "serviceEndpoint" : "http://${urlencode(azurerm_container_group.edc.fqdn)}:${local.edc_ids_port}"
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

resource "local_file" "registry_entry" {
  content = jsonencode({
    # `name` must be identical to EDC connector EDC_CONNECTOR_NAME setting for catalog asset filtering to
    # exclude assets from own connector.
    name               = local.connector_name,
    url                = "http://${azurerm_container_group.edc.fqdn}:${local.edc_ids_port}",
    supportedProtocols = ["ids-multipart"]
  })
  filename = "${path.module}/build/${var.participant_name}.json"
}

resource "local_file" "sdd" {
  content  = <<EOT
    {
        "selfDescriptionCredential": {
            "@context": [
                "http://www.w3.org/ns/shacl#",
                "http://www.w3.org/2001/XMLSchema#",
                "http://w3id.org/gaia-x/participant#",
                "@nest"
            ],
            "@id": "https://compliance.gaia-x.eu/.well-known/participant.json",
            "@type": [
                "VerifiableCredential",
                "LegalPerson"
            ],
            "credentialSubject": {
                "id": "did:compliance.gaia-x.eu",
                "gx-participant:registrationNumber": {
                    "@type": "xsd:string",
                    "@value": "${var.participant_name}"
                },
                "gx-participant:headquarterAddress": {
                    "@type": "gx-participant:Address",
                    "gx-participant:country": {
                        "@value": "${var.participant_country}",
                        "@type": "xsd:string"
                    }
                },
                "gx-participant:legalAddress": {
                    "@type": "gx-participant:Address",
                    "gx-participant:country": {
                        "@value": "${var.participant_country}",
                        "@type": "xsd:string"
                    }
                }
            },
            "proof": {
                "type": "JsonWebKey2020",
                "created": "2022-07-05T14:43:06.543Z",
                "proofPurpose": "assertionMethod",
                "verificationMethod": "did:web:test.delta-dao.com",
                "jws": "eyJhbGciOiJQUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..WkJ7XuHlg2zQxoyFyAkt-QGzMdeCQRhylNbtu8CClGx11B49Z_zKm-HAEZv-NLupapvVYswL2JjoCcEQPhUEqhYruFIXcDwSTkBRpIxo084fytVMZtM2HDHV2snYpn7zUpfVzCOb-T2pkWkbmVvAOcSOg9OLPPWO1ypqUcimaEgdkyEHK-HFAuuqtll7K_5xP0-4_anXbF7Rr4aj0WQ5_glJMD8C2wjGir5DZB_vCOygVuprUL0OSPjdxB-4k6F1UPGr8MJ-IClfXpRaV0zdjkCZseCm4dIi9SOKGYTK609atCbhG3iQdukuZLhYJ8XhHyYv_5vGjkIVeayES78R1Q"
            }
        },
        "complianceCredential": {
            "@context": [
                "https://www.w3.org/2018/credentials/v1"
            ],
            "@type": [
                "VerifiableCredential",
                "ParticipantCredential"
            ],
            "id": "https://catalogue.gaia-x.eu/credentials/ParticipantCredential/1657032187885",
            "issuer": "did:web:compliance.gaia-x.eu",
            "issuanceDate": "2022-07-05T14:43:07.885Z",
            "credentialSubject": {
                "id": "did:compliance.gaia-x.eu",
                "hash": "bd3a7c2819c80b2a4ccf24151ea2212aeffd5aecafce2a4f9672b7f707ed76a3"
            },
            "proof": {
                "type": "JsonWebKey2020",
                "created": "2022-07-05T14:43:07.885Z",
                "proofPurpose": "assertionMethod",
                "jws": "eyJhbGciOiJQUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..Sfbi2OjSoS4MLJA_ZHbAxjeWp5rD9t652mo7tV-zEV2sJjOYGOEGS7of9P8BDyHb1QJ1tNScJQu83aIEEN-NiYZGpWHfHQ39n0TnZHRiUI0GkbX8W2XDaL2wDIa62Q30v_-PdcnOruApcOIyIBVVFfel9b8OZU3L0lb0z71AO17kgDYWVMauchn9DFQrPcbPycn39dzwwoh2ojnIn6HZ5JtIeBsjzeLq2EnzNgkSjXiubHZRPjjPwM9ZqMl_Bmo0Nta18Kk8r3j5X0974xvbV63f7dfbHglNBnvc4ncEnWiRqIaF1MoMsw_EhUrVETrfrxju4Bm9cFunOIeKf8FuUQ",
                "verificationMethod": "did:web:compliance.gaia-x.eu"
            }
        }
    }
  EOT
  filename = "${path.module}/build/${var.participant_name}-sdd.json"
}

resource "azurerm_storage_share_file" "sdd" {
  name             = "sdd.json"
  storage_share_id = azurerm_storage_share.share.id
  source           = local_file.sdd.filename
}
