#
#  Copyright (c) 2023 Contributors to the Eclipse Foundation
#
#  See the NOTICE file(s) distributed with this work for additional
#  information regarding copyright ownership.
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#  License for the specific language governing permissions and limitations
#  under the License.
#
#  SPDX-License-Identifier: Apache-2.0
#

resource "kubernetes_deployment" "connector" {
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
          name              = lower(var.humanReadableName)
          image             = "catalog-server:latest"
          image_pull_policy = "Never"

          env_from {
            config_map_ref {
              name = kubernetes_config_map.catalog-server-config.metadata[0].name
            }
          }

          port {
            container_port = var.ports.management
            name           = "management-port"
          }
          port {
            container_port = var.ports.web
            name           = "default-port"
          }
          port {
            container_port = var.ports.debug
            name           = "debug-port"
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

          volume_mount {
            mount_path = "/etc/registry"
            name       = "registry-volume"
          }
        }

        volume {
          name = "registry-volume"
          config_map {
            name = kubernetes_config_map.catalog-server-config.metadata[0].name
          }
        }
      }
    }
  }
}

resource "kubernetes_config_map" "catalog-server-config" {
  metadata {
    name      = "${lower(var.humanReadableName)}-connector-config"
    namespace = var.namespace
  }

  ## Create databases for keycloak and MIW, create users and assign privileges
  data = {
    EDC_IAM_ISSUER_ID               = var.participantId
    EDC_IAM_DID_WEB_USE_HTTPS       = false
    WEB_HTTP_PORT                   = var.ports.web
    WEB_HTTP_PATH                   = "/api"
    WEB_HTTP_MANAGEMENT_PORT        = var.ports.management
    WEB_HTTP_MANAGEMENT_PATH        = "/api/management"
    WEB_HTTP_MANAGEMENT_AUTH_TYPE   = "tokenbased"
    WEB_HTTP_MANAGEMENT_AUTH_KEY    = "password"
    WEB_HTTP_CONTROL_PORT           = var.ports.control
    WEB_HTTP_CONTROL_PATH           = "/api/control"
    WEB_HTTP_PROTOCOL_PORT          = var.ports.protocol
    WEB_HTTP_PROTOCOL_PATH          = "/api/dsp"
    EDC_DSP_CALLBACK_ADDRESS        = "http://${local.controlplane-service-name}:${var.ports.protocol}/api/dsp"
    EDC_IAM_STS_PRIVATEKEY_ALIAS    = "${var.participantId}#${var.aliases.sts-private-key}"
    EDC_IAM_STS_PUBLICKEY_ID        = "${var.participantId}#${var.aliases.sts-public-key-id}"
    JAVA_TOOL_OPTIONS               = "${var.useSVE ? "-XX:UseSVE=0 " : ""}-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${var.ports.debug}"
    EDC_IH_AUDIENCE_REGISTRY_PATH   = "/etc/registry/registry.json"
    EDC_PARTICIPANT_ID              = var.participantId
    EDC_VAULT_HASHICORP_URL         = var.vault-url
    EDC_VAULT_HASHICORP_TOKEN       = var.vault-token
    EDC_MVD_PARTICIPANTS_LIST_FILE  = "/etc/participants/participants.json"
    EDC_DATASOURCE_DEFAULT_URL      = var.database.url
    EDC_DATASOURCE_DEFAULT_USER     = var.database.user
    EDC_DATASOURCE_DEFAULT_PASSWORD = var.database.password
    EDC_SQL_SCHEMA_AUTOCREATE       = true

    # remote STS configuration
    EDC_IAM_STS_OAUTH_TOKEN_URL           = var.sts-token-url
    EDC_IAM_STS_OAUTH_CLIENT_ID           = var.participantId
    EDC_IAM_STS_OAUTH_CLIENT_SECRET_ALIAS = "${var.participantId}-sts-client-secret"
  }
}
