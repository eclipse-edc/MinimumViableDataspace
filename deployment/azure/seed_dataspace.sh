#!/bin/bash

set -euo pipefail

participant_json=$(terraform -chdir=terraform output -json participant_data)
dataspace_json=$(terraform -chdir=terraform output -json dataspace_data)

echo "##########################################"
echo "### Seed Data and register Participants   "
echo "##########################################"
gxdid=$(echo "$dataspace_json" | jq -r '.gaiax_did_host')
dsdid=$(echo "$dataspace_json" | jq -r '.dataspace_did_host')
p1=9191
p2=7171

# iterate over the participants json data obtained earlier
for row in $(echo "${participant_json}" | jq -r '.[] | @base64'); do
  _jq() {
    echo "${row}" | base64 --decode | jq -r "${1}"
  }
  p1did=$(_jq '.didhost')
  name=$(_jq '.participant.name')
  region=$(_jq '.participant.region')
  asset_account=$(_jq '.assets_account')

  echo "Seed data and VC"
  ./seed_vcs.sh $name $region $p1 $p2 "$p1did" "$gxdid" "$asset_account"

  echo "Register participant with dataspace"
  java -jar registration-service-cli.jar \
    -u "http://localhost:8184/api/authority" \
    -d "did:web:${dsdid}" \
    -c "did:web:${p1did}" \
    -k "terraform/generated/$name/participant.pem" \
    participants add

  ((p1 = p1 + 1))
  ((p2 = p2 + 1))
done