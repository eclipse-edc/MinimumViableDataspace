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