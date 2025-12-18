module "kms" {
  source      = "./modules/kms"
  environment = var.environment
  project     = "kordat"
  alias       = "${var.participant}-key"
  role        = "kms"
}