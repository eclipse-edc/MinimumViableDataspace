#!/bin/bash

echo "- Az CLI login"
# if this script is used on CI, please adapt the following line to however your CI authenticates to AZ AD
az login --service-principal -u $ARM_CLIENT_ID -p $ARM_CLIENT_SECRET --tenant $ARM_TENANT_ID -o none
echo

terraform -chdir="./terraform" init -backend-config=backend.conf
terraform -chdir=terraform destroy -auto-approve
