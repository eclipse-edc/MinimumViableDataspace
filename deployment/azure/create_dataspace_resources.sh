#!/bin/bash

set -euo pipefail

sed="sed -i"
if [[ $OSTYPE == 'darwin'* ]]; then
  sed="sed -i ''"
fi
dataspace_data="$1"

vault_name=$(echo "$dataspace_data" | jq -r '.vault_name')
dataspace_did_host=$(echo "$dataspace_data" | jq -r '.dataspace_did_host')
gaiax_did_host=$(echo "$dataspace_data" | jq -r '.gaiax_did_host')

echo "- Verify DID endpoints (GAIA-X Authority and Dataspace) are available:"
curl -sSl --fail "https://$gaiax_did_host/.well-known/did.json" | jq '.id'
curl -sSl --fail "https://$dataspace_did_host/.well-known/did.json" | jq '.id'
echo

echo "- Update Docker-compose environment variables for RegistrationService"
env_file="docker/reg.env"
$sed "s/EDC_VAULT_NAME=\".*\"/EDC_VAULT_NAME=\"$vault_name\"/g" $env_file
$sed "s/EDC_VAULT_CLIENTSECRET=\".*\"/EDC_VAULT_CLIENTSECRET=\"${APP_CLIENT_SECRET}\"/g" $env_file
$sed "s/EDC_VAULT_CLIENTID=\".*\"/EDC_VAULT_CLIENTID=\"${APP_CLIENT_ID}\"/g" $env_file
$sed "s/EDC_VAULT_TENANTID=\".*\"/EDC_VAULT_TENANTID=\"${ARM_TENANT_ID}\"/g" "$env_file"
$sed "s/EDC_IDENTITY_DID_URL=\".*\"/EDC_IDENTITY_DID_URL=\"did:web:$dataspace_did_host\"/g" $env_file

echo
