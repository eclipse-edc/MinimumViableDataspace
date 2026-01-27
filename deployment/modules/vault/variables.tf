#
#  Copyright (c) 2024 Contributors to the Eclipse Foundation
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

variable "humanReadableName" {
  type        = string
  description = "Human readable name. Should not contain special characters"
}

variable "namespace" {
  type = string
}

variable "vault-token" {
  default     = "root"
  description = "This is the authentication token for the vault. DO NOT USE THIS IN PRODUCTION!"
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