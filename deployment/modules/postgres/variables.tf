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

variable "instance-name" {
  description = "Name for the Postgres instance, must be unique for each postgres instances"
}

variable "database-port" {
  default = 5432
}

variable "init-sql-configs" {
  description = "Name of config maps with init sql scripts"
  default     = []
}

variable "namespace" {
  description = "kubernetes namespace where the PG instance is deployed"
}