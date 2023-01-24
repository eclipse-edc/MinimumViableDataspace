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

   for subject in '"region": "'$region'"'
   do
     echo "Seeding VC for $participantName: $subject"
     java -jar identity-hub-cli.jar \
                 -s="http://$participantName:7171/api/v1/identity/identity-hub" \
                 vc add \
                 -c="{ $subject }" \
                 -b="$participantDid" \
                 -i="did:web:did-server:gaia-x" \
                 -k="/resources/vault/gaia-x/private-key.pem"
   done
}

function seedAndRegisterParticipant() {
  local participantName="$1"
  local region="$2"
  local participantDid="did:web:did-server:$participantName"

  # seed vc for participant
  seedVerifiedCredentials "$participantName" "$participantDid" "$region"

  # Register dataspace participants
  registerParticipant "$participantName" "$participantDid"
}

function awaitParticipantRegistration() {
  local participantName="$1"
  local region="$2"
  local participantDid="did:web:did-server:$participantName"

  cmd="java -jar registration-service-cli.jar \
                  -d=did:web:did-server:registration-service \
                  --http-scheme \
                  -k=/resources/vault/$participantName/private-key.pem \
                  -c=$participantDid \
                  participants get"

  # Wait for participant registration.
  ./validate_onboarding.sh "$participantDid" "$cmd"
}

# Read participants from participants.json file.
# $participants will contain participants and regions in a shell readable format e.g.:
# 'company1' 'eu' \n 'company2' 'eu' \n 'company3' 'us'
participants=$(jq -r '.include | map([.participant, .region])[] | @sh' /common-resources/participants.json)

# Seed VCs and register participants.
while read -r i; do
  # shellcheck disable=SC2086 # disable IDE warning: allow word splitting on jq @sh output
  eval seedAndRegisterParticipant $i
done <<< "$participants"

# Await registrations of participants.
while read -r i; do
  # shellcheck disable=SC2086 # disable IDE warning: allow word splitting on jq @sh output
  eval awaitParticipantRegistration $i
done <<< "$participants"

# flag for healthcheck by Docker
echo "finished" > finished.flag
echo "Finished successfully! Keep the container running."

# keep the container running
sleep infinity
