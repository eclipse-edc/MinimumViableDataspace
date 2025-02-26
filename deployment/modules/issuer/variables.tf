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


variable "humanReadableName" {
  type        = string
  description = "Human readable name of the issuer, NOT the ID!!. Required."
  default     = "issuerservice"
}

variable "participantId" {
  type        = string
  description = "Participant ID of the issuer. Usually a DID"
}

variable "namespace" {
  type = string
}

variable "ports" {
  type = object({
    web         = number
    sts         = number
    issuance    = number
    issueradmin = number
    version     = number
    identity    = number
    debug       = number
    did         = number
  })
  default = {
    web         = 10010
    sts         = 10011
    issuance    = 10012
    issueradmin = 10013
    version     = 10014
    identity    = 10015
    did         = 10016
    debug       = 1044
  }
}

variable "database" {
  type = object({
    url      = string
    user     = string
    password = string
  })
}

variable "useSVE" {
  type        = bool
  description = "If true, the -XX:UseSVE=0 switch (Scalable Vector Extensions) will be appended to the JAVA_TOOL_OPTIONS. Can help on macOs on Apple Silicon processors"
  default     = false
}

variable "vault-url" {
  description = "URL of the Hashicorp Vault"
  type        = string
}

variable "vault-token" {
  default     = "root"
  description = "This is the authentication token for the vault. DO NOT USE THIS IN PRODUCTION!"
  type        = string
}

variable "superuser_apikey" {
  default     = "c3VwZXItdXNlcg==.c3VwZXItc2VjcmV0LWtleQo="
  description = "Management API Key for the Super-User. Defaults to 'base64(super-user).base64(super-secret-key)"
  type        = string
}
