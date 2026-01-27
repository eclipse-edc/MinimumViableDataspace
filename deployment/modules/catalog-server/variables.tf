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

## Normally, you shouldn't need to change any values here. If you do, please be sure to also change them in the seed script (seed-k8s.sh).
## Neglecting to do that will render the connectors and identity hubs inoperable!


variable "image-pull-policy" {
  default     = "Always"
  type        = string
  description = "Kubernetes ImagePullPolicy for all images"
}

variable "humanReadableName" {
  type        = string
  description = "Human readable name of the connector, NOT the ID!!. Required."
}

variable "participantId" {
  type        = string
  description = "DID:WEB identifier of the participant"
}

variable "namespace" {
  type = string
}

variable "ports" {
  type = object({
    web        = number
    management = number
    protocol   = number
    control    = number
    debug      = number
  })
  default = {
    web        = 8080
    management = 8081
    protocol   = 8082
    control    = 8083
    debug      = 1044
  }
}

variable "database" {
  type = object({
    url      = string
    user     = string
    password = string
  })
}

variable "participant-list-file" {
  type    = string
  default = "./assets/participants/participants.k8s.json"
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

variable "vault-url" {
  description = "URL of the Hashicorp Vault"
  type        = string
}

variable "sts-token-url" {
  description = "Full URL of the STS token endpoint"
  type        = string
}

variable "aliases" {
  type = object({
    sts-private-key   = string
    sts-public-key-id = string
  })
  default = {
    sts-private-key   = "key-1"
    sts-public-key-id = "key-1"
  }
}

variable "useSVE" {
  type        = bool
  description = "If true, the -XX:UseSVE=0 switch (Scalable Vector Extensions) will be appended to the JAVA_TOOL_OPTIONS. Can help on macOs on Apple Silicon processors"
  default     = false
}

locals {
  name                      = lower(var.humanReadableName)
  controlplane-service-name = "${var.humanReadableName}-controlplane"
  ih-service-name           = "${var.humanReadableName}-identityhub"
}
