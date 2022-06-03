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
