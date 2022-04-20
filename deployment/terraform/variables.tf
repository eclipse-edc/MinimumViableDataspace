variable "prefix" {
  description = "Prefix of resource names. Guarantee uniqueness of resource names to be able to deploy several MVD without conflicts."
  default     = "test"
}

variable "participant_name" {
  default = "participant"
}

variable "runtime_image" {
}

variable "location" {
  default = "northeurope"
}

variable "acr_name" {
  default = "ageramvd"
}

variable "acr_resource_group" {
  default = "agera-mvd-common"
}

variable "resource_group" {
  default = "test-resource-group"
}

variable "container_cpu" {
  default = "0.5"
}

variable "container_memory" {
  default = "1.5"
}

variable "application_sp_object_id" {
  description = "id of application's service principal object"
}

variable "key_file" {
  description = "name of a file containing the private key in PEM format"
  default     = null
}

variable "public_key_jwk_file" {
  description = "name of a file containing the public key in JWK format"
  default = null
}
