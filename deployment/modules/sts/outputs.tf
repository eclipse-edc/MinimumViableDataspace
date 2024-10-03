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

output "sts-accounts-url" {
  value = "http://${kubernetes_service.sts-service.metadata.0.name}:${var.ports.accounts}${var.accounts-path}"
}

output "sts-token-url" {
  value = "http://${kubernetes_service.sts-service.metadata.0.name}:${var.ports.sts}${var.sts-path}"
}
