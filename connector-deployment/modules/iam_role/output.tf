output "role_arn" {
  value = aws_iam_role.role.arn
}

output "role_name" {
  value = "${var.tenant}-${var.environment}-${var.role_name}"
}
