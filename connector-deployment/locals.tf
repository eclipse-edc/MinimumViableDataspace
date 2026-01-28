locals {
  participant-did = "did:web:${var.participant}-identityhub%3A7083:${var.participant}"
  database_url    = "jdbc:postgresql://${var.postgres_endpoint}:${var.postgres_port}/${var.participant}"
  vault_url       = "http://${var.participant}-vault:8200"

  eks_oidc = trimprefix(
    data.aws_eks_cluster.eks.identity[0].oidc[0].issuer,
    "https://"
  )
}
