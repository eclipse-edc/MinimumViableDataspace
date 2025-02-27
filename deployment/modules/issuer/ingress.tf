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

resource "kubernetes_ingress_v1" "api-ingress" {
  metadata {
    name      = "${var.humanReadableName}-ingress"
    namespace = var.namespace
    annotations = {
      "nginx.ingress.kubernetes.io/rewrite-target" = "/$2"
      "nginx.ingress.kubernetes.io/use-regex"      = "true"
    }
  }
  spec {
    ingress_class_name = "nginx"
    rule {
      http {

        path {
          path = "/issuer/cs(/|$)(.*)"
          backend {
            service {
              name = kubernetes_service.issuerservice-service.metadata.0.name
              port {
                number = var.ports.identity
              }
            }
          }
        }

        path {
          path = "/issuer/ad(/|$)(.*)"
          backend {
            service {
              name = kubernetes_service.issuerservice-service.metadata.0.name
              port {
                number = var.ports.issueradmin
              }
            }
          }
        }
      }
    }
  }
}

// the DID endpoint can not actually modify the URL, otherwise it'll mess up the DID resolution
resource "kubernetes_ingress_v1" "did-ingress" {
  metadata {
    name      = "${var.humanReadableName}-did-ingress"
    namespace = var.namespace
    annotations = {
      "nginx.ingress.kubernetes.io/rewrite-target" = "/issuer/$2"
    }
  }

  spec {
    ingress_class_name = "nginx"
    rule {
      http {


        # ingress routes for the DID endpoint
        path {
          path = "/issuer(/|&)(.*)"
          backend {
            service {
              name = kubernetes_service.issuerservice-service.metadata.0.name
              port {
                number = var.ports.did
              }
            }
          }
        }
      }
    }
  }
}