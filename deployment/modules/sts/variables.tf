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

variable "humanReadableName" {
  description = "Name for STS instance"
}

variable "namespace" {
  description = "kubernetes namespace where the PG instance is deployed"
}

variable "ports" {
  type = object({
    web      = number
    accounts = number
    sts      = number
    debug    = number
  })
  default = {
    web      = 8080
    accounts = 8081
    sts      = 8082
    debug    = 1046
  }
}

variable "database" {
  type = object({
    url      = string
    user     = string
    password = string
  })
}

variable "accounts-path" {
  default = "/api"
}

variable "sts-path" {
  default = "/api/sts"
}

variable "vault-url" {
  type = string
}

variable "vault-token" {
  type    = string
  default = "root"
}

variable "useSVE" {
  type        = bool
  description = "If true, the -XX:UseSVE=0 switch (Scalable Vector Extensions) will be appended to the JAVA_TOOL_OPTIONS. Can help on macOs on Apple Silicon processors"
  default     = false
}