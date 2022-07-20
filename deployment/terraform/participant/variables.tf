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

variable "runtime_image" {
  description = "Image name of the EDC Connector to deploy"
}

variable "dashboard_image" {
  description = "Image name of the Data Dashboard web app to deploy"
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
  default = "8"
}

variable "app_insights_connection_string" {
  description = "optional connection string to Application Insights"
  default     = null
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

variable "public_key_jwk_file" {
  description = "name of a file containing the public key in JWK format"
  default     = null
}

variable "registration_service_api_url" {
  description = "registration api url"
}

variable "data_dashboard_theme" {
  description = "theme for the data dashboard ui"
  default     = "" # Use default theme. Possible theme values are defined in `theme.scss` in the DataDashboard repository.
}
