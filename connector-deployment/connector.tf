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

# This file deploys all the components needed for the consumer side of the scenario,
# i.e. the connector, an identityhub and a vault.

#
resource "kubernetes_namespace_v1" "ns_participant" {
  metadata {
    name = var.participant
  }
}

# connector
module "participant-connector" {
  source            = "./modules/connector"
  humanReadableName = var.participant
  participantId     = local.participant-did
  database = {
    user     = var.participant
    password = random_password.participant_password.result
    url      = local.database_url
  }
  vault-url                = local.vault_url
  namespace                = kubernetes_namespace_v1.ns_participant.metadata.0.name
  sts-token-url            = "${module.participant-identityhub.sts-token-url}/token"
  useSVE                   = var.useSVE
  s3_endpoint              = "https://${module.remote_state_s3.bucket_name}.s3.eu-west-1.amazonaws.com"
  aws_access_key           = aws_iam_access_key.deployer.id
  aws_secret_key           = aws_iam_access_key.deployer.secret
  service_account_role_arn = module.participant-s3-role.role_arn
}

# consumer identity hub
module "participant-identityhub" {
  depends_on        = [module.participant-vault]
  source            = "./modules/identity-hub"
  credentials-dir   = dirname("./assets/credentials/k8s/consumer/") # To~Do
  humanReadableName = "${var.participant}-identityhub"
  participantId     = local.participant-did
  vault-url         = local.vault_url
  service-name      = var.participant
  database = {
    user     = var.participant
    password = random_password.participant_password.result
    url      = local.database_url
  }
  namespace = kubernetes_namespace_v1.ns_participant.metadata.0.name
  useSVE    = var.useSVE
}

# participant vault
module "participant-vault" {
  source            = "./modules/vault"
  humanReadableName = "${var.participant}-vault"
  namespace         = kubernetes_namespace_v1.ns_participant.metadata.0.name
}
