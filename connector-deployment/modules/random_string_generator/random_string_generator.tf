resource "random_password" "random_string_generator" {
  length           = var.length
  special          = var.special
  override_special = var.override_special
}
