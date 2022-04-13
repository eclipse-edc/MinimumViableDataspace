variable "prefix" {
  description = "Prefix of resource names. Guarantee uniqueness of resource names to be able to deploy several MVD without conflicts."
  default     = "test"
}

variable "participant_name" {
  default = "test-participant"
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