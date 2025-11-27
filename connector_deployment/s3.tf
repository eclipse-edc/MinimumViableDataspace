module "remote_state_s3" {
  source      = "./modules/s3_bucket"
  project     = "kordat"
  environment = var.environment
  application = "assets"
  bucket_name = "${var.participant}-assets-bucket"
  versioning  = "Enabled"
}