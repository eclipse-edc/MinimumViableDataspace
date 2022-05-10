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
  description = "object id of application's service principal object"
}

variable "application_sp_client_id" {
  description = "client id of application's service principal object"
}

variable "application_sp_client_secret" {
  description = "client secret of application's service principal object"
  sensitive   = true
}

variable "key_file" {
  description = "name of a file containing the private key in PEM format"
  default     = null
}

variable "public_key_jwk_file" {
  description = "name of a file containing the public key in JWK format"
  default     = null
}
variable "registry_resource_group" {
  description = "resource group of the registry JSON documents file share storage account"
}

variable "registry_storage_account" {
  description = "name of the registry JSON documents file share storage account"
}

variable "registry_share" {
  description = "name of the registry JSON documents file share"
}

variable "data_dashboard_image_tag" {
  description = "tag of the Data Dashboard web app image to deploy"
  default     = "9f1e9c48c39906b8005a961c52768a7488c988ab"
}


