# Create and name kms key
resource "aws_kms_key" "master_key" {
  deletion_window_in_days = 15

  tags = merge(var.tags, {
    Name     = var.alias
    Entorno  = title(var.environment)
    Rol      = var.role
    Proyecto = length(var.project) == 3 ? upper(var.project) : title(var.project)
  })
}

resource "aws_kms_alias" "master_key_alias" {
  name          = "alias/${var.alias}"
  target_key_id = aws_kms_key.master_key.key_id
}

resource "aws_kms_key_policy" "master_key_policy" {
  count  = length(var.policy) != 0 ? 1 : 0
  key_id = aws_kms_key.master_key.id
  policy = var.policy
}