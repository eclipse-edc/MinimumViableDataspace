# Secret config
variable "length" {
  type        = string
  description = "Length of the random string"
  default     = "16"
}
variable "special" {
  type        = bool
  description = ""
  default     = true
}
variable "override_special" {
  type        = string
  description = ""
  default     = "!#$%&*()-_=+[]{}<>:?"
}