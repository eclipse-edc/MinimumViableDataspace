# kms

<!-- BEGINNING OF PRE-COMMIT-TERRAFORM DOCS HOOK -->
## Requirements

No requirements.

## Providers

| Name | Version |
|------|---------|
| <a name="provider_aws"></a> [aws](#provider\_aws) | n/a |

## Modules

No modules.

## Resources

| Name | Type |
|------|------|
| [aws_kms_alias.master_key_alias](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/kms_alias) | resource |
| [aws_kms_key.master_key](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/kms_key) | resource |
| [aws_kms_key_policy.master_key_policy](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/kms_key_policy) | resource |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_alias"></a> [alias](#input\_alias) | KMS alias | `string` | `""` | no |
| <a name="input_environment"></a> [environment](#input\_environment) | Environment (dev\|pre\|pro) | `string` | n/a | yes |
| <a name="input_policy"></a> [policy](#input\_policy) | KMS policy | `string` | `""` | no |
| <a name="input_project"></a> [project](#input\_project) | Project | `string` | n/a | yes |
| <a name="input_role"></a> [role](#input\_role) | Role into the product | `string` | `"kms"` | no |
| <a name="input_tags"></a> [tags](#input\_tags) | Tags to use | `map(any)` | `{}` | no |
| <a name="input_tenant"></a> [tenant](#input\_tenant) | Tenant name | `string` | n/a | yes |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_key_arn"></a> [key\_arn](#output\_key\_arn) | n/a |
| <a name="output_key_id"></a> [key\_id](#output\_key\_id) | n/a |
<!-- END OF PRE-COMMIT-TERRAFORM DOCS HOOK -->
