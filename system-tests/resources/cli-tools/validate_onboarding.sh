#!/bin/bash

# stop on error
set -euo pipefail

participantDid="$1"
cmd="$2"

echo "Fetching $participantDid onboarding status."

retryCount=0
maxRetryCount=30
onboardingCompleted=false

while [ $retryCount -lt $maxRetryCount ]; do

    status=$($cmd|jq ".status")

    echo "Status: $status"

    if [ "$status" == "\"ONBOARDED\"" ]; then
        echo "$participantDid is onboarded successfully"
        onboardingCompleted=true
        break
    else
        echo "Onboarding is not completed yet for $participantDid. Waiting for 1 second."

        sleep 1
    fi

    retryCount=$((retryCount+1))

done

if [ "$onboardingCompleted" == false ]; then
    echo "Max retries of $maxRetryCount reached. Onboarding is not completed yet for 'did:web:$participantDid'. Exiting."
    exit 1
fi
