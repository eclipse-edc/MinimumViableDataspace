resource "kubernetes_job" "seed_connectors_via_mgmt_api" {
  // wait until the connectors are running, otherwise terraform may report an error
  depends_on = [kubernetes_deployment.connector]
  metadata {
    name      = "seed-${var.humanReadableName}"
    namespace = var.namespace
  }
  spec {
    // run only once
    completions                = 1
    completion_mode            = "NonIndexed"
    // clean up any job pods after 90 seconds, failed or succeeded
    ttl_seconds_after_finished = "90"
    template {
      metadata {
        name = "seed-${var.humanReadableName}"
      }
      spec {
        /* SEED APPLICATION DATA */
        container {
          name    = "seed-application-data"
          image   = "postman/newman:ubuntu"
          command = [
            "newman", "run",
            "--folder", "Seed",
            "--env-var",
            "HOST=http://${kubernetes_service.controlplane-service.metadata.0.name}:${var.ports.management}",
            "/opt/collection/${local.newman_collection_name}"
          ]
          volume_mount {
            mount_path = "/opt/collection"
            name       = "seed-collection"
          }
        }

        /* SEED IDENTITY HUB DATA */
        container {
          name    = "create-participant"
          image   = "postman/newman:ubuntu"
          command = [
            "curl", "-o - -I", "--location",
            "http://${kubernetes_service.ih-service.metadata.0.name}:${var.ports.ih-management}/api/management/v1/participants/",
            "--header", "Content-Type: application/json",
            "--header", "x-api-key: ${var.ih_superuser_apikey}",
            "--data",
            "{\n    \"roles\":[],\n    \"serviceEndpoints\":[\n      {\n         \"type\": \"CredentialService\",\n         \"serviceEndpoint\": \"http://${kubernetes_service.ih-service.metadata.0.name}:${var.ports.resolution-api}/api/resolution/v1/participants/${base64encode(var.participant-did)}\",\n         \"id\": \"credentialservice-1\"\n      }\n    ],\n    \"active\": true,\n    \"participantId\": \"${var.participant-did}\",\n    \"did\": \"${var.participant-did}\",\n    \"key\":{\n        \"keyId\": \"key-1\",\n        \"privateKeyAlias\": \"key-1\",\n        \"publicKeyPem\":\"-----BEGIN PUBLIC KEY-----\\nMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE1l0Lof0a1yBc8KXhesAnoBvxZw5r\\noYnkAXuqCYfNK3ex+hMWFuiXGUxHlzShAehR6wvwzV23bbC0tcFcVgW//A==\\n-----END PUBLIC KEY-----\"\n    }\n}\n"
          ]
          volume_mount {
            mount_path = "/opt/collection"
            name       = "seed-collection"
          }
        }

        volume {
          name = "seed-collection"
          config_map {
            name = kubernetes_config_map.seed-collection.metadata.0.name
          }
        }
        // only restart when failed
        restart_policy = "OnFailure"
      }
    }
  }
}

resource "kubernetes_config_map" "seed-collection" {
  metadata {
    name      = "seed-collection-${var.humanReadableName}"
    namespace = var.namespace
  }
  data = {
    (local.newman_collection_name) = file("${path.module}/../../postman/MVD_.postman_collection.json")
  }
}

locals {
  newman_collection_name = "MVD.postman_collection.json"
}