resource "kubernetes_deployment" "identityhub" {
  metadata {
    name      = "${lower(var.humanReadableName)}-identityhub"
    namespace = var.namespace
    labels    = {
      App = "${lower(var.humanReadableName)}-identityhub"
    }
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        App = "${lower(var.humanReadableName)}-identityhub"
      }
    }

    template {
      metadata {
        labels = {
          App = "${lower(var.humanReadableName)}-identityhub"
        }
      }

      spec {
        container {
          image_pull_policy = "Never"
          image             = "identity-hub:latest"
          name              = "identity-hub"

          env_from {
            config_map_ref {
              name = kubernetes_config_map.identityhub-config.metadata[0].name
            }
          }
          port {
            container_port = var.ports.resolution-api
            name           = "res-port"
          }

          port {
            container_port = var.ports.ih-debug
            name           = "debug"
          }
          port {
            container_port = var.ports.ih-did-management
            name           = "did-mgmt"
          }
          port {
            container_port = var.ports.ih-did
            name           = "did"
          }
        }
      }

    }
  }
}

resource "kubernetes_config_map" "identityhub-config" {
  metadata {
    name      = "${lower(var.humanReadableName)}-ih-config"
    namespace = var.namespace
  }

  data = {
    # IdentityHub variables
    EDC_API_AUTH_KEY          = "password"
    EDC_IH_IAM_ID             = var.participantId
    EDC_IAM_DID_WEB_USE_HTTPS = false
    EDC_IH_IAM_PUBLICKEY_PEM  = var.publickey-pem
    WEB_HTTP_PORT             = var.ports.ih-default
    WEB_HTTP_PATH             = "/api"
    WEB_HTTP_MANAGEMENT_PORT  = var.ports.ih-did-management
    WEB_HTTP_MANAGEMENT_PATH  = "/api/management"
    WEB_HTTP_RESOLUTION_PORT  = var.ports.resolution-api
    WEB_HTTP_RESOLUTION_PATH  = "/api/resolution"
    WEB_HTTP_DID_PORT         = var.ports.ih-did
    WEB_HTTP_DID_PATH         = "/"
    JAVA_TOOL_OPTIONS         = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${var.ports.ih-debug}"
  }
}