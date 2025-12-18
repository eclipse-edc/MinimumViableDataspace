# Common variables
variable "project" {
  type        = string
  description = "project name"
}
variable "environment" {
  type        = string
  description = "Environment (dev|pre|pro)"
}
variable "application" {
  type        = string
  description = "Role into the product"
}

# Bucket variables
variable "bucket_name" {
  type        = string
  description = "Bucket Name"
}
variable "object_ownership" {
  type        = string
  description = "Bucket Objects ownership"
  default     = "ObjectWriter"
}
variable "lifecycle_rules" {
  type = list(object({
    id     = string
    status = string
    prefix = string
    transitions = list(object({
      days          = number
      storage_class = string
    }))
    expiration = number
  }))
  description = "JSON containing rules for lifecycle"
  default     = []
}

variable "acl" {
  type        = string
  description = "Bucket Acl"
  default     = "private"
}
variable "versioning" {
  type        = string
  description = "Bucket Versioning"
  default     = "Disabled"
}
variable "kms" {
  type        = string
  description = "KMS for Encryption"
}
variable "cors" {
  type = object({
    apply           = bool
    allowed_headers = list(string)
    allowed_methods = list(string)
    allowed_origins = list(string)
    expose_headers  = list(string)
  })
  description = "CORS configuration"
  default = {
    apply           = false,
    allowed_headers = [],
    allowed_methods = [],
    allowed_origins = [],
  expose_headers = [], }
}
