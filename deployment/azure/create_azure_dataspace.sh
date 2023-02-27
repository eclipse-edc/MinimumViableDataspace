#!/bin/bash

set -euo pipefail

echo "#########################"
echo "### CREATE dataspace"
echo "#########################"

echo "Create tfvars file"
cat >terraform/terraform.tfvars <<EOF
application_sp_object_id = "$APP_OBJECT_ID"
application_sp_client_id = "$APP_CLIENT_ID"
application_sp_client_secret="$APP_CLIENT_SECRET"
res_prefix="$RESOURCES_PREFIX"
EOF
echo

echo "Create backend conf"
echo "
  resource_group_name   = \""${COMMON_RESOURCE_GROUP}"\"
  storage_account_name  = \""${TERRAFORM_STATE_STORAGE_ACCOUNT}"\"
  container_name        = \""${TERRAFORM_STATE_CONTAINER}"\"
  key                   = \""${RESOURCES_PREFIX}"\"
" > terraform/backend.conf
echo

echo "###############################"
echo "### Create Dataspace Resources "
echo "###############################"
./generate_keys.sh

echo "Az CLI login"
az login --service-principal -u $ARM_CLIENT_ID -p $ARM_CLIENT_SECRET --tenant $ARM_TENANT_ID -o none
echo

echo "Run Terraform"
terraform -chdir="./terraform" init -backend-config=backend.conf
terraform -chdir="./terraform" apply -auto-approve
echo

echo "###################################"
echo "### Configure Dataspace Runtimes "
echo "###################################"

echo "Update runtime configuration"
participant_json=$(terraform -chdir=terraform output -json participant_data)
./create_participant_resources.sh "$participant_json"
dataspace_json=$(terraform -chdir=terraform output -json dataspace_data)
./create_dataspace_resources.sh "$dataspace_json"

echo "Copy self descriptions"
mkdir -p resources
cp -r terraform/modules/participant/resources/* resources/
cp -r terraform/modules/dataspace/resources/* resources/
echo


