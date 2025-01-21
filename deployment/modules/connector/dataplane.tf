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

resource "kubernetes_deployment" "dataplane" {
  # needs a hard dependency, otherwise the dataplane registration fails, and it is not retried
  depends_on = [kubernetes_deployment.controlplane]
  metadata {
    name      = "${lower(var.humanReadableName)}-dataplane"
    namespace = var.namespace
    labels = {
      App = "${lower(var.humanReadableName)}-dataplane"
    }
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        App = "${lower(var.humanReadableName)}-dataplane"
      }
    }

    template {
      metadata {
        labels = {
          App = "${lower(var.humanReadableName)}-dataplane"
        }
      }

      spec {
        container {
          name              = "dataplane-${lower(var.humanReadableName)}"
          image             = "dataplane:latest"
          image_pull_policy = "Never"

          env_from {
            config_map_ref {
              name = kubernetes_config_map.dataplane-config.metadata[0].name
            }
          }

          port {
            container_port = var.ports.public
            name           = "public-port"
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
        }
      }
    }
  }
}

resource "kubernetes_config_map" "dataplane-config" {
  metadata {
    name      = "${lower(var.humanReadableName)}-dataplane-config"
    namespace = var.namespace
  }

  ## Create databases for keycloak and MIW, create users and assign privileges
  data = {
    # hostname is "localhost" by default, but must be the service name at which the dataplane is reachable. URL scheme and port are appended by the application
    EDC_HOSTNAME                                      = local.dataplane-service-name
    EDC_RUNTIME_ID                                    = "${var.humanReadableName}-dataplane"
    EDC_PARTICIPANT_ID                                = var.participantId
    EDC_TRANSFER_PROXY_TOKEN_VERIFIER_PUBLICKEY_ALIAS = "${var.participantId}#${var.aliases.sts-public-key-id}"
    EDC_TRANSFER_PROXY_TOKEN_SIGNER_PRIVATEKEY_ALIAS  = "${var.participantId}#${var.aliases.sts-private-key}"
    EDC_DPF_SELECTOR_URL                              = "http://${local.controlplane-service-name}:${var.ports.control}/api/control/v1/dataplanes"
    WEB_HTTP_PORT                                     = var.ports.web
    WEB_HTTP_PATH                                     = "/api"
    WEB_HTTP_CONTROL_PORT                             = var.ports.control
    WEB_HTTP_CONTROL_PATH                             = "/api/control"
    WEB_HTTP_PUBLIC_PORT                              = var.ports.public
    WEB_HTTP_PUBLIC_PATH                              = "/api/public"
    EDC_VAULT_HASHICORP_URL                           = var.vault-url
    EDC_VAULT_HASHICORP_TOKEN                         = var.vault-token
    JAVA_TOOL_OPTIONS                                 = "${var.useSVE ? "-XX:UseSVE=0 " : ""}-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${var.ports.debug}"
    EDC_DATASOURCE_DEFAULT_URL                        = var.database.url
    EDC_DATASOURCE_DEFAULT_USER                       = var.database.user
    EDC_DATASOURCE_DEFAULT_PASSWORD                   = var.database.password
    EDC_SQL_SCHEMA_AUTOCREATE                         = true

    # remote STS configuration
    EDC_IAM_STS_OAUTH_TOKEN_URL           = var.sts-token-url
    EDC_IAM_STS_OAUTH_CLIENT_ID           = var.participantId
    EDC_IAM_STS_OAUTH_CLIENT_SECRET_ALIAS = "consumer-participant-sts-client-secret"
  }
}
