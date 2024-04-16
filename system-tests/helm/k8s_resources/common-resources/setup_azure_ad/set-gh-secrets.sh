REPO=$(terraform output -raw GH_REPO)
gh="gh --repo $REPO"

$gh secret set ARM_CLIENT_ID --body "$(terraform output -raw ARM_CLIENT_ID)"
$gh secret set ARM_SUBSCRIPTION_ID --body "$(terraform output -raw ARM_SUBSCRIPTION_ID)"
$gh secret set ARM_TENANT_ID --body "$(terraform output -raw ARM_TENANT_ID)"
$gh secret set ARM_CLIENT_SECRET --body "$(terraform output -raw ARM_CLIENT_SECRET)"
$gh secret set APP_CLIENT_ID --body "$(terraform output -raw APP_CLIENT_ID)"
$gh secret set APP_CLIENT_SECRET --body "$(terraform output -raw APP_CLIENT_SECRET)"
$gh secret set APP_OBJECT_ID --body "$(terraform output -raw APP_OBJECT_ID)"
$gh secret set COMMON_RESOURCE_GROUP --body "$(terraform output -raw COMMON_RESOURCE_GROUP)"
$gh secret set COMMON_RESOURCE_GROUP_LOCATION --body "$(terraform output -raw COMMON_RESOURCE_GROUP_LOCATION)"
$gh secret set TERRAFORM_STATE_CONTAINER --body "$(terraform output -raw TERRAFORM_STATE_CONTAINER)"
$gh secret set TERRAFORM_STATE_STORAGE_ACCOUNT --body "$(terraform output -raw TERRAFORM_STATE_STORAGE_ACCOUNT)"