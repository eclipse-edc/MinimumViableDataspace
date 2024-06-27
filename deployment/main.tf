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
    helm = {
      // used for Hashicorp Vault
      source = "hashicorp/helm"
    }
  }
}

provider "kubernetes" {
  config_path = "~/.kube/config"
}

provider "helm" {
  kubernetes {
    config_path = "~/.kube/config"
  }
}

# consumer connector
module "alice-connector" {
  source            = "./modules/connector"
  humanReadableName = "alice"
  participantId     = var.alice-did
  participant-did   = var.alice-did
  database-name     = "alice"
  namespace         = kubernetes_namespace.ns.metadata.0.name
}

module "consumer-alice-identityhub" {
  source            = "./modules/identity-hub"
  credentials-dir = dirname("./assets/credentials/k8s/alice")
  humanReadableName = "alice-identityhub"
  participantId     = var.alice-did
  vault-url         = "http://alice-vault:8200"
}

# first provider connector "Ted"
module "provider-ted-connector" {
  source            = "./modules/connector"
  humanReadableName = "provider-ted"
  participantId     = var.bob-did
  participant-did   = var.bob-did
  database-name = "ted"
  #   credentials-dir   = dirname("./assets/credentials/k8s/bob")
  namespace         = kubernetes_namespace.ns.metadata.0.name
}

# Second provider connector "Carol"
module "provider-carol-connector" {
  source            = "./modules/connector"
  humanReadableName = "provider-carol"
  participantId     = var.bob-did
  participant-did   = var.bob-did
  database-name     = "carol"
  namespace         = kubernetes_namespace.ns.metadata.0.name
}

module "provider-identityhub" {
  source            = "./modules/identity-hub"
  credentials-dir = dirname("./assets/credentials/k8s/bob")
  humanReadableName = "provider-identityhub"
  participantId     = var.bob-did
  vault-url         = "http://provider-catalog-server-vault:8200"
}

# Catalog server runtime "Bob"
module "provider-catalog-server" {
  source            = "./modules/catalog-server"
  humanReadableName = "provider-catalog-server"
  participantId     = var.bob-did
  participant-did   = var.bob-did
  database-name     = "provider-catalog-server"
  credentials-dir = dirname("./assets/credentials/k8s/bob")
  namespace         = kubernetes_namespace.ns.metadata.0.name
}

resource "kubernetes_namespace" "ns" {
  metadata {
    name = "mvd"
  }
}
