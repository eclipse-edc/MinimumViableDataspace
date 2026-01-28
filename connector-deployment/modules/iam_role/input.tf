# Common variables
variable "tenant" {
  type        = string
  description = "Tenant name"
}
variable "environment" {
  type        = string
  description = "Environment (dev|pre|pro)"
}
variable "role" {
  type        = string
  description = "Role into the product"
}

# Role vars
variable "role_name" {
  type        = string
  description = "Name of the role"
}
variable "role_principal_service" {
  type        = string
  description = "Principal service of the role"
  default     = ""
}
variable "full_assume_role_policy" {
  type    = string
  default = ""
}
variable "policy_name" {
  type        = string
  description = "Name of the policy"
  default     = ""
}
variable "policy_content" {
  type        = string
  description = "Name of the content"
  default     = ""
}

# Additional attachements
variable "extra_policies" {
  type        = list(string)
  description = "Extra policies to attach"
  default     = []
}
