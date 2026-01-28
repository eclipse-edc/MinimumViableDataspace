module "participant-s3-role" {
  source                  = "./modules/iam_role"
  environment             = var.environment
  tenant                  = var.project
  role                    = "kordat-participant"
  role_name               = "${var.participant}-s3-sa-role"
  full_assume_role_policy = <<EOT
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Federated": "${data.aws_iam_openid_connect_provider.eks_oidc.arn}"
            },
            "Action": "sts:AssumeRoleWithWebIdentity",
            "Condition": {
                "StringEquals": {
                    "${local.eks_oidc}:sub": "system:serviceaccount:${var.participant}:${var.participant}-s3-sa"
                }
            }
        }
    ]
}
EOT

  policy_name    = "${var.participant}-s3-sa-policy"
  policy_content = <<EOT
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:ListAccessPointsForObjectLambda",
                "s3:GetAccessPoint",
                "s3:PutAccountPublicAccessBlock",
                "s3:ListAccessPoints",
                "s3:CreateStorageLensGroup",
                "s3:ListJobs",
                "s3:PutStorageLensConfiguration",
                "s3:ListMultiRegionAccessPoints",
                "s3:ListStorageLensGroups",
                "s3:ListStorageLensConfigurations",
                "s3:GetAccountPublicAccessBlock",
                "s3:ListAllMyBuckets",
                "s3:ListAccessGrantsInstances",
                "s3:PutAccessPointPublicAccessBlock",
                "s3:CreateJob"
            ],
            "Resource": "*"
        },
        {
            "Action": [
              "s3:*"
            ],
            "Effect": "Allow",
            "Resource": [
            "${module.assets_s3_bucket.bucket_arn}",
            "${module.assets_s3_bucket.bucket_arn}/*"
            ]
        }
    ]
}
EOT
}
