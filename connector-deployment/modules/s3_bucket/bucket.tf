resource "aws_s3_bucket" "bucket" {
  bucket = "${var.project}-${var.environment}-${var.bucket_name}"

  tags = {
    Name        = "${var.project}-${var.environment}-${var.bucket_name}"
    project     = var.project
    environment = var.environment
    application = var.application
    module      = "s3_bucket"
  }
}

resource "aws_s3_bucket_ownership_controls" "bucket_ownership" {
  bucket = aws_s3_bucket.bucket.id
  rule {
    object_ownership = var.object_ownership
  }
}

resource "aws_s3_bucket_public_access_block" "this" {
  bucket = aws_s3_bucket.bucket.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_acl" "bucket_acl" {
  depends_on = [aws_s3_bucket_ownership_controls.bucket_ownership]

  bucket = aws_s3_bucket.bucket.id
  acl    = var.acl
}

resource "aws_s3_bucket_versioning" "bucket_versioning" {
  bucket = aws_s3_bucket.bucket.id
  versioning_configuration {
    status = var.versioning
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "bucket_encryption" {
  bucket = aws_s3_bucket.bucket.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = var.kms
    }

    # Reduce llamadas a KMS y coste en muchos casos
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "lifecycle_configuration" {
  count  = length(var.lifecycle_rules) > 0 ? 1 : 0
  bucket = aws_s3_bucket.bucket.id

  dynamic "rule" {
    for_each = { for each in var.lifecycle_rules : each.id => each }
    iterator = rule
    content {
      id     = rule.key
      status = rule.value.status

      filter {
        prefix = rule.value.prefix
      }
      dynamic "expiration" {
        for_each = rule.value.expiration != 0 ? [1] : []
        iterator = expiration
        content {
          days = rule.value.expiration
        }
      }

      dynamic "transition" {
        for_each = rule.value.transitions
        iterator = transition

        content {
          days          = transition.value.days
          storage_class = transition.value.storage_class
        }
      }
    }
  }
}

resource "aws_s3_bucket_cors_configuration" "cors_configuration" {
  count  = var.cors.apply ? 1 : 0
  bucket = aws_s3_bucket.bucket.id

  cors_rule {
    allowed_headers = var.cors.allowed_headers
    allowed_methods = var.cors.allowed_methods
    allowed_origins = var.cors.allowed_origins
    expose_headers  = var.cors.expose_headers
  }
}

# (Opcional) Forzar cifrado: deniega PUT si no viene SSE-KMS con TU key
# resource "aws_s3_bucket_policy" "enforce_kms" {
#   bucket = aws_s3_bucket.bucket.id
#   policy = jsonencode({
#     Version = "2012-10-17"
#     Statement = [
#       {
#         Sid    = "DenyUnEncryptedObjectUploads"
#         Effect = "Deny"
#         Principal = "*"
#         Action = "s3:PutObject"
#         Resource = "${aws_s3_bucket.bucket.arn}/*"
#         Condition = {
#           StringNotEquals = {
#             "s3:x-amz-server-side-encryption" = "aws:kms"
#           }
#         }
#       },
#       {
#         Sid    = "DenyWrongKMSKey"
#         Effect = "Deny"
#         Principal = "*"
#         Action = "s3:PutObject"
#         Resource = "${aws_s3_bucket.bucket.arn}/*"
#         Condition = {
#           StringNotEquals = {
#             "s3:x-amz-server-side-encryption-aws-kms-key-id" = var.kms
#           }
#         }
#       }
#     ]
#   })
# }
