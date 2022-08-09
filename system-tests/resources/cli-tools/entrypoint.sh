#!/bin/bash

# stop on error
set -euo pipefail

PARTICIPANTS=(provider:eu consumer-eu:eu consumer-us:us)

# Register dataspace participants
for participant in "${PARTICIPANTS[@]}"; do
    participantArray=(${participant//:/ })

    participantName=${participantArray[0]}
    region=${participantArray[1]}
    participantDid="did:web:did-server:$participantName"

    echo "Registering $participantName"
    java -jar registration-service-cli.jar \
                -d="did:web:did-server:registration-service" \
                --http-scheme \
                -k=/resources/vault/$participantName/private-key.pem \
                -c="$participantDid" \
                 participants add \
                --ids-url "http://$participantName:8282"

   for subject in '"region": "'$region'"' '"gaiaXMember": "true"'
   do
     echo "Seeding VC for $participantName: $subject"
     vcId=$(uuidgen)
     java -jar identity-hub-cli.jar \
                 -s="http://$participantName:8181/api/identity-hub" \
                 vc add \
                 -c='{"id":"'$vcId'","credentialSubject":{'"$subject"}'}' \
                -b="$participantDid" \
                 -i="did:web:did-server:gaia-x" \
                 -k="/resources/vault/gaia-x/private-key.pem"
   done
done
