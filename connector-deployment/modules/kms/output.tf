output "key_id" {
  value = aws_kms_key.master_key.key_id
}
output "key_arn" {
  value = aws_kms_key.master_key.arn
}