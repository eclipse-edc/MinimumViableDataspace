module "kms" {
  source      = "./modules/kms"
  environment = var.environment
  project     = var.project
  alias       = "${var.participant}-key"
  role        = "kms"
}