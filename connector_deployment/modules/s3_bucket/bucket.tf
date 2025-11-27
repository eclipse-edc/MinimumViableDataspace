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
  count  = (var.encryption ? 1 : 0)
  bucket = aws_s3_bucket.bucket.id

  rule {
    bucket_key_enabled = true

    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
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
