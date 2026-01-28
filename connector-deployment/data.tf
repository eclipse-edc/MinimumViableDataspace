# Current AWS account
data "aws_caller_identity" "current" {}

# EKS data
data "aws_eks_cluster" "eks" {
  name = "${var.project}-${var.environment}-eks"
}

data "aws_iam_openid_connect_provider" "eks_oidc" {
  url = data.aws_eks_cluster.eks.identity[0].oidc[0].issuer
}