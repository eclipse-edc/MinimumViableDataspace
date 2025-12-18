output "random_value" {
  value     = random_password.random_string_generator.result
  sensitive = true
}