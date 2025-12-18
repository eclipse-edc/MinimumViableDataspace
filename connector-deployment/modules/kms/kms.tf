# Current AWS account
data "aws_caller_identity" "current" {}

# Create and name kms key
resource "aws_kms_key" "master_key" {
  deletion_window_in_days = 15
  enable_key_rotation     = true
}

resource "aws_kms_alias" "master_key_alias" {
  name          = "alias/${var.alias}"
  target_key_id = aws_kms_key.master_key.key_id
}

resource "aws_kms_key_policy" "master_key_policy" {
  key_id = aws_kms_key.master_key.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      # Admin de la key (tu cuenta)
      {
        Sid      = "AllowAccountAdmin"
        Effect   = "Allow"
        Principal = { AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root" }
        Action   = "kms:*"
        Resource = "*"
      },
      # Permitir a S3 usar la key para este bucket (vía servicio s3 y en tu cuenta)
      {
        Sid    = "AllowS3UseOfKey"
        Effect = "Allow"
        Principal = { Service = "s3.amazonaws.com" }
        Action = [
          "kms:Encrypt",
          "kms:Decrypt",
          "kms:ReEncrypt*",
          "kms:GenerateDataKey*",
          "kms:DescribeKey"
        ]
        Resource = "*"
        Condition = {
          StringEquals = {
            "kms:CallerAccount" = data.aws_caller_identity.current.account_id
          }
          StringLike = {
            "kms:ViaService" = "s3.eu-west-1.amazonaws.com"
          }
        }
      }
    ]
  })
}