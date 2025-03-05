#
#  Copyright (c) 2025 Cofinity-X
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0
#
#  SPDX-License-Identifier: Apache-2.0
#
#  Contributors:
#       Cofinity-X - initial API and implementation
#

resource "kubernetes_deployment" "issuerservice" {
  metadata {
    name      = lower(var.humanReadableName)
    namespace = var.namespace
    labels = {
      App = lower(var.humanReadableName)
    }
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        App = lower(var.humanReadableName)
      }
    }

    template {
      metadata {
        labels = {
          App = lower(var.humanReadableName)
        }
      }

      spec {
        container {
          image_pull_policy = "Never"
          image             = "issuerservice:latest"
          name              = "issuerservice"

          env_from {
            config_map_ref {
              name = kubernetes_config_map.issuerservice-config.metadata[0].name
            }
          }
          port {
            container_port = var.ports.web
            name           = "web"
          }

          port {
            container_port = var.ports.sts
            name           = "sts"
          }
          port {
            container_port = var.ports.debug
            name           = "debug"
          }
          port {
            container_port = var.ports.issuance
            name           = "issuance"
          }
          port {
            container_port = var.ports.issueradmin
            name           = "issueradmin"
          }
          port {
            container_port = var.ports.identity
            name           = "identity-port"
          }

          port {
            container_port = var.ports.did
            name           = "did"
          }

          liveness_probe {
            http_get {
              path = "/api/check/liveness"
              port = var.ports.web
            }
            failure_threshold = 10
            period_seconds    = 5
            timeout_seconds   = 30
          }

          readiness_probe {
            http_get {
              path = "/api/check/readiness"
              port = var.ports.web
            }
            failure_threshold = 10
            period_seconds    = 5
            timeout_seconds   = 30
          }

          startup_probe {
            http_get {
              path = "/api/check/startup"
              port = var.ports.web
            }
            failure_threshold = 10
            period_seconds    = 5
            timeout_seconds   = 30
          }
        }
      }
    }
  }
}

resource "kubernetes_config_map" "issuerservice-config" {
  metadata {
    name      = "${lower(var.humanReadableName)}-config"
    namespace = var.namespace
  }

  data = {
    EDC_ISSUER_STATUSLIST_SIGNING_KEY_ALIAS = "statuslist-signing-key"
    EDC_IH_API_SUPERUSER_KEY                = var.superuser_apikey
    WEB_HTTP_PORT                           = var.ports.web
    WEB_HTTP_PATH                           = "/api"
    WEB_HTTP_STS_PORT                       = var.ports.sts
    WEB_HTTP_STS_PATH                       = "/api/sts"
    WEB_HTTP_ISSUANCE_PORT                  = var.ports.issuance
    WEB_HTTP_ISSUANCE_PATH                  = "/api/issuance"
    WEB_HTTP_ISSUERADMIN_PORT               = var.ports.issueradmin
    WEB_HTTP_ISSUERADMIN_PATH               = "/api/admin"
    WEB_HTTP_VERSION_PORT                   = var.ports.version
    WEB_HTTP_VERSION_PATH                   = "/.well-known/api"
    WEB_HTTP_IDENTITY_PORT                  = var.ports.identity
    WEB_HTTP_IDENTITY_PATH                  = "/api/identity"
    WEB_HTTP_IDENTITY_AUTH_KEY              = "password"
    WEB_HTTP_DID_PORT                       = var.ports.did
    WEB_HTTP_DID_PATH                       = "/"

    JAVA_TOOL_OPTIONS               = "${var.useSVE ? "-XX:UseSVE=0 " : ""}-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${var.ports.debug}"
    EDC_VAULT_HASHICORP_URL         = var.vault-url
    EDC_VAULT_HASHICORP_TOKEN       = var.vault-token
    EDC_DATASOURCE_DEFAULT_URL      = var.database.url
    EDC_DATASOURCE_DEFAULT_USER     = var.database.user
    EDC_DATASOURCE_DEFAULT_PASSWORD = var.database.password

    # even though we have a default data source, we need a named datasource for the DatabaseAttestationSource, because
    # that is configured in the AttestationDefinition
    EDC_DATASOURCE_MEMBERSHIP_URL      = var.database.url
    EDC_DATASOURCE_MEMBERSHIP_USER     = var.database.user
    EDC_DATASOURCE_MEMBERSHIP_PASSWORD = var.database.password

    EDC_SQL_SCHEMA_AUTOCREATE          = true
    EDC_IAM_ACCESSTOKEN_JTI_VALIDATION = true
    EDC_IAM_DID_WEB_USE_HTTPS          = false

  }
}

