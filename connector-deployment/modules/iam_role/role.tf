resource "aws_iam_role" "role" {
  name = "${var.tenant}-${var.environment}-${var.role_name}"
  assume_role_policy = coalesce(
    var.full_assume_role_policy,
    jsonencode({
      Version = "2012-10-17"
      Statement = [
        {
          Action = "sts:AssumeRole"
          Effect = "Allow"
          Principal = {
            Service = var.role_principal_service
          }
        },
      ]
    })
  )

  tags = {
    Name        = "${var.tenant}-${var.environment}-${var.role_name}"
    tenant      = var.tenant
    environment = var.environment
    role        = var.role
  }
}
