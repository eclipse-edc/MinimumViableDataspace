#!/bin/bash

set -euo pipefail

sed="sed -i"
if [[ $OSTYPE == 'darwin'* ]]; then
  sed="sed -i ''"
fi

data_json="$1"
for row in $(echo "${data_json}" | jq -r '.[] | @base64'); do
  _jq() {
    echo "${row}" | base64 --decode | jq -r "${1}"
  }
  api_key=$(_jq '.api_key')
  vault_name=$(_jq '.vault')
  did_host=""$(_jq '.didhost')
  conn_name=$(_jq '.connector_name')
  participant_name=$(_jq '.participant.name')
  assets_account=$(_jq '.assets_account')

  echo "Update Docker-compose environment variables for Participant"
  env_file="./docker/${participant_name}.env"
  echo "processing file $env_file"
  $sed "s/EDC_VAULT_NAME=\".*\"/EDC_VAULT_NAME=\"$vault_name\"/g" "$env_file"
  $sed "s/EDC_VAULT_CLIENTSECRET=\".*\"/EDC_VAULT_CLIENTSECRET=\"${APP_CLIENT_SECRET}\"/g" "$env_file"
  $sed "s/EDC_VAULT_CLIENTID=\".*\"/EDC_VAULT_CLIENTID=\"${APP_CLIENT_ID}\"/g" "$env_file"
  $sed "s/EDC_VAULT_TENANTID=\".*\"/EDC_VAULT_TENANTID=\"${ARM_TENANT_ID}\"/g" "$env_file"
  $sed "s/EDC_IDENTITY_DID_URL=\".*\"/EDC_IDENTITY_DID_URL=\"did:web:$did_host\"/g" "$env_file"
  $sed "s/EDC_PARTICIPANT_ID=\".*\"/EDC_PARTICIPANT_ID=\"did:web:$did_host\"/g" "$env_file"
  $sed "s/EDC_CONNECTOR_NAME=\".*\"/EDC_CONNECTOR_NAME=\"$conn_name\"/g" "$env_file"
  $sed "s/EDC_API_AUTH_KEY=\".*\"/EDC_API_AUTH_KEY=\"$api_key\"/g" "$env_file"
  echo "Verify that the DID Endpoint is ready"
  curl -sSl --fail https://$did_host/.well-known/did.json | jq '.id'
  echo

  echo "Update UI App Config file"
  appCfgFile="./resources/appconfig/${participant_name}/app.config.json"
  echo "processing file $appCfgFile"

  $sed "s/\"storageAccount\": *\".*\"/\"storageAccount\": \"${assets_account}\"/g" "$appCfgFile"
done