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

resource "kubernetes_service" "ih-service" {
  metadata {
    name      = var.humanReadableName
    namespace = var.namespace
  }
  spec {
    type = "NodePort"
    selector = {
      App = kubernetes_deployment.identityhub.spec.0.template.0.metadata[0].labels.App
    }
    # we need a stable IP, otherwise there will be a cycle with the issuer
    port {
      name = "credentials"
      port = var.ports.credentials-api
    }
    port {
      name = "debug"
      port = var.ports.ih-debug
    }
    port {
      name = "management"
      port = var.ports.ih-identity-api
    }
    port {
      name = "did"
      port = var.ports.ih-did
    }
    port {
      name = "sts"
      port = var.ports.sts-api
    }
  }
}