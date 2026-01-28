resource "aws_iam_role_policy" "policy" {
  count = length(var.policy_content) > 0 ? 1 : 0
  name  = "${var.tenant}-${var.environment}-${var.policy_name}"
  role  = aws_iam_role.role.id

  policy = var.policy_content
}
