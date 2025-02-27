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

resource "kubernetes_service" "issuerservice-service" {
  metadata {
    name      = var.humanReadableName
    namespace = var.namespace
  }
  spec {
    type = "NodePort"
    selector = {
      App = kubernetes_deployment.issuerservice.spec.0.template.0.metadata[0].labels.App
    }
    port {
      name = "web"
      port = var.ports.web
    }
    port {
      name = "sts"
      port = var.ports.sts
    }
    port {
      name = "debug"
      port = var.ports.debug
    }
    port {
      name = "issuance"
      port = var.ports.issuance
    }
    port {
      name = "issueradmin"
      port = var.ports.issueradmin
    }
    port {
      name = "identity"
      port = var.ports.identity
    }
    port {
      name = "did"
      port = var.ports.did
    }
  }
}