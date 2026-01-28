# iam_role

<!-- BEGIN_TF_DOCS -->
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
| [aws_iam_role.role](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role_policy.policy](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy) | resource |
| [aws_iam_role_policy_attachment.test-attach](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_environment"></a> [environment](#input\_environment) | Environment (dev\|pre\|pro) | `string` | n/a | yes |
| <a name="input_extra_policies"></a> [extra\_policies](#input\_extra\_policies) | Extra policies to attach | `list(string)` | `[]` | no |
| <a name="input_full_assume_role_policy"></a> [full\_assume\_role\_policy](#input\_full\_assume\_role\_policy) | n/a | `string` | `""` | no |
| <a name="input_policy_content"></a> [policy\_content](#input\_policy\_content) | Name of the content | `string` | `""` | no |
| <a name="input_policy_name"></a> [policy\_name](#input\_policy\_name) | Name of the policy | `string` | `""` | no |
| <a name="input_role"></a> [role](#input\_role) | Role into the product | `string` | n/a | yes |
| <a name="input_role_name"></a> [role\_name](#input\_role\_name) | Name of the role | `string` | n/a | yes |
| <a name="input_role_principal_service"></a> [role\_principal\_service](#input\_role\_principal\_service) | Principal service of the role | `string` | `""` | no |
| <a name="input_tenant"></a> [tenant](#input\_tenant) | Tenant name | `string` | n/a | yes |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_role_arn"></a> [role\_arn](#output\_role\_arn) | n/a |
| <a name="output_role_name"></a> [role\_name](#output\_role\_name) | n/a |
<!-- END_TF_DOCS -->
