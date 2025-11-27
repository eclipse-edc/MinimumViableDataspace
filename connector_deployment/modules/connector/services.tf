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

resource "kubernetes_service" "controlplane-service" {
  metadata {
    name      = local.controlplane-service-name
    namespace = var.namespace
  }
  spec {
    type = "NodePort"
    selector = {
      App = kubernetes_deployment.controlplane.spec.0.template.0.metadata[0].labels.App
    }
    port {
      name = "health"
      port = var.ports.web
    }
    port {
      name = "management"
      port = var.ports.management
    }
    port {
      name = "catalog"
      port = var.ports.catalog
    }
    port {
      name = "protocol"
      port = var.ports.protocol
    }
    port {
      name = "debug"
      port = var.ports.debug
    }
    port {
      name = "control"
      port = var.ports.control
    }
  }
}

resource "kubernetes_service" "dataplane-service" {
  metadata {
    name      = local.dataplane-service-name
    namespace = var.namespace
  }
  spec {
    type = "NodePort"
    selector = {
      App = kubernetes_deployment.dataplane.spec.0.template.0.metadata[0].labels.App
    }
    port {
      name = "control"
      port = var.ports.control
    }
    port {
      name = "public"
      port = var.ports.public
    }
  }
}