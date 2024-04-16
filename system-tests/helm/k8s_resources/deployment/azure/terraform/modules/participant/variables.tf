variable "prefix" {
  description = "Prefix of resource names. Guarantee uniqueness of resource names to be able to deploy several MVD without conflicts."
  default     = "test"
}

variable "participant_name" {
  default = "participant"
}

variable "participant_region" {
  default = "eu"
}
#
variable "participant_country" {
  default = "FR"
}

variable "location" {
  default = "eastus"
}

variable "resource_group" {
  default = "test-resource-group"
}

variable "application_sp_object_id" {
  description = "object id of application's service principal object"
}

variable "application_sp_client_id" {
  description = "client id of application's service principal object"
}

variable "public_key_jwk_file" {
  description = "name of a file containing the public key in JWK format"
  default     = null
}

variable "private_key_pem_file" {
  description = "path to the participant's PEM encoded private key"
}

variable "api_key" {
  default = "ApiKeyDefaultValue"
}