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
        // this container seeds application data to the connector
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

        // seed DID documents via the IH's management API
        container {
          name    = "seed-did"
          image   = "curlimages/curl"
          command = [
            "curl", "-v",
            "-X", "POST",
            "--location",
            "http://${kubernetes_service.ih-service.metadata.0.name}:${var.ports.ih-did-management}/api/management/v1/dids?publish=true",
            "--header", "Content-Type: application/json",
            "--data-binary", file("${path.module}/../../assets/dids/${replace(var.participant-did, ":", "_")}.json")
          ]
        }

        // this container seeds data to the miw service
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
    (local.newman_collection_name) = file("${path.module}/../../postman/IATP_Demo.postman_collection.json")
  }
}

locals {
  newman_collection_name = "IATP_Demo.postman_collection.json"
}