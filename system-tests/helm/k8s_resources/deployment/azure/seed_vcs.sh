#!/bin/bash

if [ "$#" -lt 7 ]; then
  echo "Usage: sh $0 <PARTICIPANT_NAME> <REGION> <MGMT_API_PORT> <IDENTITY_PORT> <PARTICIPANT_DID_HOST> <GAIAX_DID_HOST> <ASSET_STORAGE_ACCOUNT>"
  exit 1
fi

participant="$1"
region="$2"
managementPort="$3"
identityPort="$4"
participant_did_host="$5"
gaiax_did_host="$6"
asset_account="$7"


## Function declarations to be used later
pushCredential() {
  local participant="$1"
  local claims="$2"
  echo "Push claims to $participant at port $identityPort"
  echo "    claims: ${claims}"
  echo
  local participant_did="did:web:$participant_did_host"
  local gaiax_did="$3"

  java -jar identity-hub-cli.jar -s="$ihUrl" vc add \
    -c="$claims" \
    -b="$participant_did" \
    -i="$gaiax_did" \
    -k="terraform/generated/dataspace/gaiaxkey.pem"
}

checkCredentials() {
  len=$(java -jar identity-hub-cli.jar -s="$ihUrl" vc list | jq -r '. | length')
  if [ "$len" -lt 1 ]; then
    echo "Wrong number of VCs, expected > 1, got ${len}"
    exit 2
  fi
}

# variables
gaiax_did="did:web:$gaiax_did_host"


ihUrl="http://localhost:${identityPort}/api/identity/identity-hub"

echo "### Handling participant \"$participant\" in region \"$region\""
echo "### Push seed data    "

# read the API KEY from the .env file that was generated during the resource generation phase
# cut into tokens at the "=" with cut and remove all double-quotes with tr
api_key=$(grep "EDC_API_AUTH_KEY" "docker/$participant.env" | cut -d "=" -f2 | tr -d '"')
newman run \
  --folder "Publish Master Data" \
  --env-var data_management_url="http://localhost:$managementPort/api/management" \
  --env-var storage_account="${asset_account}" \
  --env-var participant_id="${participant}" \
  --env-var api_key="$api_key" \
  ../data/MVD.postman_collection.json
echo

# hack - assume all containers have sequential management api managementPort configurations, check docker/docker-compose.yml for details!!!

pushCredential "$participant" '{"region": "'"$region"'"}' "$gaiax_did"

checkCredentials