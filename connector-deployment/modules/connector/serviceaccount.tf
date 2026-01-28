resource "kubernetes_service_account" "s3_sa" {
  metadata {
    name      = "${lower(var.humanReadableName)}-s3-sa"
    namespace = var.namespace

    annotations = {
      "eks.amazonaws.com/role-arn" = var.service_account_role_arn
    }
  }
}