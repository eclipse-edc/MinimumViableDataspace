#!/bin/bash

# stop on error
set -euo pipefail

function waitForOnboarding() {
  local participantName="$1"
  local participantDid="$2"
  echo "Fetching participant onboarding status..."
  retryCount=0
  maxRetryCount=30
  onboardingCompleted=false
  while [ $retryCount -lt $maxRetryCount ]; do
      cmd='java -jar registration-service-cli.jar \
                      -d="did:web:did-server:registration-service" \
                      --http-scheme \
                      -k=/resources/vault/$participantName/private-key.pem \
                      -c="$participantDid" \
                      participants get'
      status=$($cmd|jq ".status")
      echo "Status is $status"
      if [ "$status" == "\"ONBOARDED\"" ]; then
          echo "Participant is onboarded successfully"
          onboardingCompleted=true
          break
      else
          echo "Onboarding is not completed yet. Waiting for 1 seconds..."
          sleep 1
      fi
      retryCount=$((retryCount+1))
      echo $retryCount
  done
  if [ "$onboardingCompleted" == false ]; then
      echo "$participantDid onboarding is not completed yet. Exiting..."
      exit 1
  fi
}

function registerParticipant() {
  local participantName="$1"
  local participantDid="$2"

  echo "Registering $participantName"
  java -jar registration-service-cli.jar \
              -d="did:web:did-server:registration-service" \
              --http-scheme \
              -k=/resources/vault/$participantName/private-key.pem \
              -c="$participantDid" \
               participants add \
              --ids-url "http://$participantName:8282"

# wait for the participant to be onboarded
  waitForOnboarding "$participantName" "$participantDid"
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


for participant in "${PARTICIPANTS[@]}"; do
    participantArray=(${participant//:/ })

    participantName=${participantArray[0]}
    region=${participantArray[1]}
    participantDid="did:web:did-server:$participantName"

  # seed  vc for participant
  seedVerifiedCredentials "$participantName" "$participantDid" "$region"

  # Register dataspace participants
  registerParticipant "$participantName" "$participantDid"
done
