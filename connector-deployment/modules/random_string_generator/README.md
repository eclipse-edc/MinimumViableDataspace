# Random Strin Generator Module

This module generates a random string.

## How to use
Include this code in your `main.tf`:

```
module "string" {
  source           = "./modules/random_string_generator"
  length           = "16"
  spacial          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}
```

## Requirements

## Outputs
<!-- BEGIN_TF_DOCS -->
## Requirements

No requirements.

## Providers

| Name | Version |
|------|---------|
| <a name="provider_random"></a> [random](#provider\_random) | n/a |

## Modules

No modules.

## Resources

| Name | Type |
|------|------|
| [random_password.random_string_generator](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/password) | resource |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_length"></a> [length](#input\_length) | Length of the random string | `string` | `"16"` | no |
| <a name="input_override_special"></a> [override\_special](#input\_override\_special) | n/a | `string` | `"!#$%&*()-_=+[]{}<>:?"` | no |
| <a name="input_special"></a> [special](#input\_special) | n/a | `bool` | `true` | no |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_random_value"></a> [random\_value](#output\_random\_value) | n/a |
<!-- END_TF_DOCS -->
