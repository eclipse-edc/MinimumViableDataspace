#!/bin/bash

# stop on error
set -euo pipefail

function registerParticipant() {
  local participantName="$1"
  local participantDid="$2"

  echo "Registering $participantName"
  java -jar registration-service-cli.jar \
              -d="did:web:did-server:registration-service" \
              --http-scheme \
              -k=/resources/vault/$participantName/private-key.pem \
              -c="$participantDid" \
               participants add
}

function seedVerifiedCredentials() {
  local participantName="$1"
  local participantDid="$2"
  local region="$3"

   for subject in '"region": "'$region'"' '"gaiaXMember": "true"'
   do
     echo "Seeding VC for $participantName: $subject"
     vcId=$(uuidgen)
     java -jar identity-hub-cli.jar \
                 -s="http://$participantName:8181/api/identity-hub" \
                 vc add \
                 -c='{"id":"'$vcId'","credentialSubject":{'"$subject"'}}' \
                -b="$participantDid" \
                 -i="did:web:did-server:gaia-x" \
                 -k="/resources/vault/gaia-x/private-key.pem"
   done
}

PARTICIPANTS=(company1:eu company2:eu company3:us)

# Seed VCs and register participants.
for participant in "${PARTICIPANTS[@]}"; do
  participantArray=(${participant//:/ })

  participantName=${participantArray[0]}
  region=${participantArray[1]}
  participantDid="did:web:did-server:$participantName"

  # seed vc for participant
  seedVerifiedCredentials "$participantName" "$participantDid" "$region"

  # Register dataspace participants
  registerParticipant "$participantName" "$participantDid"
done

# Await registrations of participants.
for participant in "${PARTICIPANTS[@]}"; do
  participantArray=(${participant//:/ })

  participantName=${participantArray[0]}
  participantDid="did:web:did-server:$participantName"

  cmd="java -jar registration-service-cli.jar \
                  -d=did:web:did-server:registration-service \
                  --http-scheme \
                  -k=/resources/vault/$participantName/private-key.pem \
                  -c=$participantDid \
                  participants get"

  # Wait for participant registration.
  ./validate_onboarding.sh "$participantDid" "$cmd"
done

# flag for healthcheck by Docker
echo "finished" > finished.flag
echo "Finished successfully! Keep the container running."

# keep the container running
sleep infinity
