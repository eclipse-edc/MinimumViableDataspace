variable "prefix" {
  description = "Prefix of resource names. Guarantee uniqueness of resource names to be able to deploy several MVD without conflicts."
  default     = "test"
}

variable "location" {
  default = "northeurope"
}

variable "resource_group" {
  default = "test-dataspace"
}

variable "registry_runtime_image" {
  description = "Image name of the Registry Service to deploy."
}

variable "acr_name" {
  default = "ageramvd"
}

variable "acr_resource_group" {
  default = "agera-mvd-common"
}

variable "container_cpu" {
  default = "0.5"
}

variable "container_memory" {
  default = "8"
}

variable "registry_resource_group" {
  description = "resource group of the registration service"
}

variable "application_sp_object_id" {
  description = "object id of application's service principal object"
}

variable "key_file" {
  description = "name of a file containing the private key in PEM format"
  default     = null
}

variable "public_key_jwk_file" {
  description = "name of a file containing the public key in JWK format"
  default     = null
}
