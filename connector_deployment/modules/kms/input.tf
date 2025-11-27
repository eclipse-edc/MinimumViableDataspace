# Common variables
variable "environment" {
  type        = string
  description = "Environment (dev|pre|pro)"
}
variable "role" {
  type        = string
  description = "Role into the product"
  default     = "kms"
}
variable "project" {
  type        = string
  description = "Project"
}
variable "tags" {
  type        = map(any)
  description = "Tags to use"
  default     = {}
}

# Config vars
variable "policy" {
  type        = string
  description = "KMS policy"
  default     = ""
}
variable "alias" {
  type        = string
  description = "KMS alias"
  default     = ""
}