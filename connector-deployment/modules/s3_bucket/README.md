# S3 Bucket Module

Creates a S3 bucket and configure the acl, versioning, encryption and objects ownership.

## How to use
Include this code in your `main.tf`:

```
module "example_bucket" {
  source            = "./modules/s3_bucket"
  project           = var.project
  environment       = var.environment
  role              = "Descriptive functionality"
  bucket_name       = "The name of the bucket"
  object_ownership  = "ObjectWriter"
  object_expiration = 90 # Days
  acl               = "private"
  versioning        = "Enabled"
  encryption        = true
}
```

## Outputs
The name and arn of the new s3 bucket created
```
module.example_bucket.bucket_name
module.example_bucket.bucket_arn
```

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
| [aws_s3_bucket.bucket](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket) | resource |
| [aws_s3_bucket_acl.bucket_acl](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_acl) | resource |
| [aws_s3_bucket_cors_configuration.cors_configuration](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_cors_configuration) | resource |
| [aws_s3_bucket_lifecycle_configuration.lifecycle_configuration](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_lifecycle_configuration) | resource |
| [aws_s3_bucket_ownership_controls.bucket_ownership](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_ownership_controls) | resource |
| [aws_s3_bucket_server_side_encryption_configuration.bucket_encryption](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_server_side_encryption_configuration) | resource |
| [aws_s3_bucket_versioning.bucket_versioning](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_versioning) | resource |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_acl"></a> [acl](#input\_acl) | Bucket Acl | `string` | `"private"` | no |
| <a name="input_application"></a> [application](#input\_application) | Role into the product | `string` | n/a | yes |
| <a name="input_bucket_name"></a> [bucket\_name](#input\_bucket\_name) | Bucket Name | `string` | n/a | yes |
| <a name="input_cors"></a> [cors](#input\_cors) | CORS configuration | <pre>object({<br/>    apply           = bool<br/>    allowed_headers = list(string)<br/>    allowed_methods = list(string)<br/>    allowed_origins = list(string)<br/>    expose_headers  = list(string)<br/>  })</pre> | <pre>{<br/>  "allowed_headers": [],<br/>  "allowed_methods": [],<br/>  "allowed_origins": [],<br/>  "apply": false,<br/>  "expose_headers": []<br/>}</pre> | no |
| <a name="input_encryption"></a> [encryption](#input\_encryption) | Bucket Encryption | `string` | `true` | no |
| <a name="input_environment"></a> [environment](#input\_environment) | Environment (dev\|pre\|pro) | `string` | n/a | yes |
| <a name="input_lifecycle_rules"></a> [lifecycle\_rules](#input\_lifecycle\_rules) | JSON containing rules for lifecycle | <pre>list(object({<br/>    id     = string<br/>    status = string<br/>    prefix = string<br/>    transitions = list(object({<br/>      days          = number<br/>      storage_class = string<br/>    }))<br/>    expiration = number<br/>  }))</pre> | `[]` | no |
| <a name="input_object_ownership"></a> [object\_ownership](#input\_object\_ownership) | Bucket Objects ownership | `string` | `"ObjectWriter"` | no |
| <a name="input_project"></a> [project](#input\_project) | project name | `string` | n/a | yes |
| <a name="input_versioning"></a> [versioning](#input\_versioning) | Bucket Versioning | `string` | `"Disabled"` | no |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_bucket_arn"></a> [bucket\_arn](#output\_bucket\_arn) | n/a |
| <a name="output_bucket_id"></a> [bucket\_id](#output\_bucket\_id) | n/a |
| <a name="output_bucket_name"></a> [bucket\_name](#output\_bucket\_name) | n/a |
<!-- END_TF_DOCS -->
