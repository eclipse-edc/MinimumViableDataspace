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

variable "image-pull-policy" {
  default     = "Always"
  type        = string
  description = "Kubernetes ImagePullPolicy for all images"
}

variable "humanReadableName" {
  type        = string
  description = "Human readable name of the connector, NOT the BPN!!. Required."
}

variable "participantId" {
  type        = string
  description = "Participant ID of the connector. In Catena-X, this MUST be the BPN"
}

variable "participant-did" {
  type        = string
  description = "DID:WEB identifier of the participant"
}

variable "namespace" {
  type    = string
  default = "mvd"
}

variable "ports" {
  type = object({
    resolution-api = number
    web            = number
    management     = number
    protocol       = number
    debug          = number
    ih-default     = number
    ih-debug       = number
    ih-did         = number
    ih-management  = number
  })
  default = {
    web            = 8080
    management     = 8081
    protocol       = 8082
    debug          = 1044
    ih-default     = 7080
    ih-debug       = 1045
    ih-management  = 7081
    resolution-api = 7082
    ih-did         = 7083
  }
}

variable "publickey-pem" {
  type        = string
  description = "Public key in PEM format"
}

variable "database-name" {
  type = string
}

variable "registry-json" {
  type        = string
  description = "JSON file containing all DSP-URL-to-audience mappings"
}

variable "credentials-dir" {
  type    = string
  default = "JSON object containing the credentials to seed, sorted by human-readable participant name"
}

variable "ih_superuser_apikey" {
  default     = "c3VwZXItdXNlcg==.c3VwZXItc2VjcmV0LWtleQo="
  description = "Management API Key for the Super-User. Defaults to 'base64(super-user).base64(super-secret-key)"
  type        = string
}

variable "vault-token" {
  default     = "root"
  description = "This is the authentication token for the vault. DO NOT USE THIS IN PRODUCTION!"
  type        = string
}

variable "aliases" {
  type = object({
    sts-private-key = string
    sts-public-key-id = string
  })
  default = {
    sts-private-key = "key-1"
    sts-public-key-id = "key-1"
  }
}

locals {
  name                      = lower(var.humanReadableName)
  controlplane-service-name = "${var.humanReadableName}-controlplane"
  ih-service-name           = "${var.humanReadableName}-identityhub"
}
