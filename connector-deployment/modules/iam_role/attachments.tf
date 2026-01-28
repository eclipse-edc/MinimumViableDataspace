resource "aws_iam_role_policy_attachment" "test-attach" {
  for_each   = toset(var.extra_policies)
  role       = aws_iam_role.role.id
  policy_arn = each.key
}