variable "prefix" {
  description = "Prefix of resource names. Guarantee uniqueness of resource names to be able to deploy several MVD without conflicts."
  default     = "test"
}

variable "location" {
  default = "eastus"
}

variable "resource_group" {
  default = "test-dataspace"
}

variable "dataspace_authority_country" {
  default = "DE"
}

variable "application_sp_object_id" {
  description = "object id of application's service principal object"
}

variable "application_sp_client_id" {
  description = "client id of application's service principal object"
}

variable "application_sp_client_secret" {
  description = "client secret of application's service principal object"
  sensitive   = true
}

variable "public_key_jwk_file_authority" {
  description = "name of a file containing the Registration Service public key in JWK format"
  default     = null
}

variable "public_key_jwk_file_gaiax" {
  description = "name of a file containing the GAIA-X public key in JWK format"
  default     = null
}

variable "private_key_pem_file" {
  description = "path to the registration service's PEM encoded private key"
}