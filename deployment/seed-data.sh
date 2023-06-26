#!/bin/bash

set -euxo pipefail

# Split env vars into an array using delimiter ':'

ParticipantIdArray=(${PARTICIPANT_ID//:/ })
AssetsStorageAccountArray=(${ASSETS_STORAGE_ACCOUNT//:/ })
EdcHostArray=(${EDC_HOST//:/ })

if [ ${#ParticipantIdArray[@]} -ne ${#AssetsStorageAccountArray[@]} ] || [ ${#AssetsStorageAccountArray[@]} -ne ${#EdcHostArray[@]} ]; then
  echo "PARTICIPANT_ID,ASSETS_STORAGE_ACCOUNT and EDC_HOST must be of equal length"
  exit 1
fi

for i in "${!EdcHostArray[@]}"; do
  echo "Seeding data for Participant ID: ${ParticipantIdArray[$i]}, Assets Storage Account: ${AssetsStorageAccountArray[$i]}, EDC Host: ${EdcHostArray[$i]}"

  newman run \
    --folder "Publish Master Data" \
    --env-var data_management_url="http://${EdcHostArray[$i]}:9191/api/management" \
    --env-var storage_account="${AssetsStorageAccountArray[$i]}" \
    --env-var participant_id="${ParticipantIdArray[$i]}" \
    --env-var api_key="$API_KEY" \
    deployment/data/MVD.postman_collection.json
done
