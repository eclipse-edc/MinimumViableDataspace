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

terraform {
  required_providers {
    // for generating passwords, clientsecrets etc.
    random = {
      source = "hashicorp/random"
    }

    kubernetes = {
      source = "hashicorp/kubernetes"
    }
  }
}

provider "kubernetes" {
  config_path = "~/.kube/config"
}


# First connector
module "alice-connector" {
  source            = "./modules/connector"
  humanReadableName = "alice"
  participantId     = var.alice-bpn
  participant-did   = var.alice-did
  publickey-pem     = file("./assets/ec-p256-public.pem")
  database-name     = "alice"
  registry-json     = local.registry
  namespace         = kubernetes_namespace.ns.metadata.0.name
}

# Second connector
module "bob-connector" {
  source            = "./modules/connector"
  humanReadableName = "bob"
  participantId     = var.bob-bpn
  participant-did   = var.bob-did
  publickey-pem     = file("./assets/ec-p256-public.pem")
  database-name     = "bob"
  registry-json     = local.registry
  namespace         = kubernetes_namespace.ns.metadata.0.name
}

resource "kubernetes_namespace" "ns" {
  metadata {
    name = "iatp"
  }
}

locals {
  registry = jsonencode(
    [
      {
        dspUrl : "http://bob-controlplane:8082/api/dsp",
        participantId : var.bob-bpn,
        did : var.bob-did
      },
      {
        dspUrl : "http://alice-controlplane:8092/api/dsp",
        participantId : var.alice-bpn,
        did : var.alice-did
      }
    ]
  )
}