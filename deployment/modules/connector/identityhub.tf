resource "kubernetes_deployment" "identityhub" {
  depends_on = [helm_release.vault]
  metadata {
    name      = "${lower(var.humanReadableName)}-identityhub"
    namespace = var.namespace
    labels = {
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
            container_port = var.ports.ih-identity-api
            name           = "did-mgmt"
          }
          port {
            container_port = var.ports.ih-did
            name           = "did"
          }

          volume_mount {
            mount_path = "/etc/credentials"
            name       = "credentials-volume"
          }
        }

        volume {
          name = "credentials-volume"
          config_map {
            name = kubernetes_config_map.identityhub-credentials-map.metadata[0].name
          }
        }
      }

    }
  }
}

locals {
  dir = "${var.credentials-dir}/${var.humanReadableName}"
}

resource "kubernetes_config_map" "identityhub-credentials-map" {
  metadata {
    name      = "${lower(var.humanReadableName)}-credentials"
    namespace = var.namespace
  }

  data = {
    for f in fileset(local.dir, "*.json") : f => file(join("/", [local.dir, f]))
  }
}

resource "kubernetes_config_map" "identityhub-config" {
  metadata {
    name      = "${lower(var.humanReadableName)}-ih-config"
    namespace = var.namespace
  }

  data = {
    # IdentityHub variables
    EDC_API_AUTH_KEY             = "password"
    EDC_IH_IAM_ID                = var.participantId
    EDC_IAM_DID_WEB_USE_HTTPS    = false
    EDC_IH_IAM_PUBLICKEY_ALIAS   = local.public-key-alias
    EDC_IH_API_SUPERUSER_KEY     = var.ih_superuser_apikey
    WEB_HTTP_PORT                = var.ports.ih-default
    WEB_HTTP_PATH                = "/api"
    WEB_HTTP_IDENTITY_PORT       = var.ports.ih-identity-api
    WEB_HTTP_IDENTITY_PATH       = "/api/management"
    WEB_HTTP_RESOLUTION_PORT     = var.ports.resolution-api
    WEB_HTTP_RESOLUTION_PATH     = "/api/resolution"
    WEB_HTTP_DID_PORT            = var.ports.ih-did
    WEB_HTTP_DID_PATH            = "/"
    JAVA_TOOL_OPTIONS            = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${var.ports.ih-debug}"
    EDC_IAM_STS_PRIVATEKEY_ALIAS = var.aliases.sts-private-key
    EDC_IAM_STS_PUBLICKEY_ID     = var.aliases.sts-public-key-id
    EDC_MVD_CREDENTIALS_PATH     = "/etc/credentials/"
    EDC_VAULT_HASHICORP_URL      = "http://${var.humanReadableName}-vault:8200"
    EDC_VAULT_HASHICORP_TOKEN    = var.vault-token
  }
}

locals {
  public-key-alias = "${var.humanReadableName}-publickey"
}