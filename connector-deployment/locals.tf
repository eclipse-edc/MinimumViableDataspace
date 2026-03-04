locals {
  participant-did = "did:web:${var.participant}-identityhub.${var.participant}%3A7083:${var.participant}"
  database_url    = "jdbc:postgresql://${var.postgres_endpoint}:${var.postgres_port}/${var.participant}"
  vault_url       = "http://${var.participant}-vault:8200"

  # S3 replication: only enabled for participants that replicate to another account
  replication_enabled = var.replicate_to_participant && var.participant_account_id != "" && var.participant_bucket_name != ""

  eks_oidc = trimprefix(
    data.aws_eks_cluster.eks.identity[0].oidc[0].issuer,
    "https://"
  )
}
