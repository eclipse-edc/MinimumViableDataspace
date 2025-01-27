#
#  Copyright (c) 2024 Metaform Systems, Inc.
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0
#
#  SPDX-License-Identifier: Apache-2.0
#
#  Contributors:
#       Metaform Systems, Inc. - initial API and implementation
#

resource "kubernetes_deployment" "sts" {
  metadata {
    name      = var.humanReadableName
    namespace = var.namespace
    labels = {
      App = var.humanReadableName
    }
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        App = var.humanReadableName
      }
    }
    template {
      metadata {
        labels = {
          App = var.humanReadableName
        }
      }
      spec {
        container {
          image             = "sts:latest"
          name              = "sts"
          image_pull_policy = "Never"

          env_from {
            config_map_ref {
              name = kubernetes_config_map.sts-config.metadata[0].name
            }
          }
          port {
            container_port = var.ports.accounts
            name           = "accounts-port"
          }

          port {
            container_port = var.ports.sts
            name           = "sts-port"
          }

          # Uncomment this to assign (more) resources
          #          resources {
          #            limits = {
          #              cpu    = "2"
          #              memory = "512Mi"
          #            }
          #            requests = {
          #              cpu    = "250m"
          #              memory = "50Mi"
          #            }
          #          }

          liveness_probe {
            http_get {
              path = "/internal/check/liveness"
              port = var.ports.web
            }
            failure_threshold = 10
            period_seconds    = 5
            timeout_seconds   = 30
          }

          readiness_probe {
            http_get {
              path = "/internal/check/readiness"
              port = var.ports.web
            }
            failure_threshold = 10
            period_seconds    = 5
            timeout_seconds   = 30
          }

          startup_probe {
            http_get {
              path = "/internal/check/startup"
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

resource "kubernetes_config_map" "sts-config" {
  metadata {
    name      = "${var.humanReadableName}-config"
    namespace = var.namespace
  }

  ## Create databases for keycloak and MIW, create users and assign privileges
  data = {
    JAVA_TOOL_OPTIONS               = "${var.useSVE ? "-XX:UseSVE=0 " : ""}-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${var.ports.debug}"
    WEB_HTTP_ACCOUNTS_PORT          = var.ports.accounts
    WEB_HTTP_ACCOUNTS_PATH          = var.accounts-path
    WEB_HTTP_ACCOUNTS_AUTH_TYPE     = "tokenbased"
    WEB_HTTP_ACCOUNTS_AUTH_KEY      = "password"
    WEB_HTTP_PORT                   = var.ports.web
    WEB_HTTP_PATH                   = "/internal"
    WEB_HTTP_STS_PORT               = var.ports.sts
    WEB_HTTP_STS_PATH               = var.sts-path
    EDC_DATASOURCE_DEFAULT_URL      = var.database.url
    EDC_DATASOURCE_DEFAULT_USER     = var.database.user
    EDC_DATASOURCE_DEFAULT_PASSWORD = var.database.password
    EDC_SQL_SCHEMA_AUTOCREATE       = true

    EDC_VAULT_HASHICORP_URL   = var.vault-url
    EDC_VAULT_HASHICORP_TOKEN = var.vault-token
  }
}

resource "kubernetes_service" "sts-service" {
  metadata {
    name      = "${var.humanReadableName}-service"
    namespace = var.namespace
  }
  spec {
    selector = {
      App = kubernetes_deployment.sts.spec.0.template.0.metadata[0].labels.App
    }
    port {
      name        = "accounts-port"
      port        = var.ports.accounts
      target_port = var.ports.accounts
    }
    port {
      name        = "sts-port"
      port        = var.ports.sts
      target_port = var.ports.sts
    }
  }
}
