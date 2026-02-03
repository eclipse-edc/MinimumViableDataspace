# resource "aws_iam_user" "main" {
#   name = "${var.project}-${var.participant}-s3-user"

# #   tags = merge(var.tags, {
# #     Name     = "${var.tenant}-${var.project}-${var.environment}-${var.role}-user"
# #     tenant   = var.tenant
# #     Proyecto = length(var.project) == 3 ? upper(var.project) : title(var.project)
# #     Entorno  = title(var.environment)
# #     Rol      = var.role
# #   })
# }

# resource "aws_iam_access_key" "deployer" {
#   user = aws_iam_user.main.name
# }