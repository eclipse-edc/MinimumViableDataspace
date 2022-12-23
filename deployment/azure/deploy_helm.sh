#!/bin/bash

# the "x" will log every command to the shell - good for debugging purposes
set -euxo pipefail

echo "Deploy registration service"
envsubst <helm/registration-service/values.yaml | helm upgrade --install registration-service helm/registration-service --timeout 20s --cleanup-on-fail --values -

for PARTICIPANT in "company1" "company2" "company3"; do
  envsubst <helm/connector/values.yaml | helm upgrade --install $PARTICIPANT helm/connector --timeout 20s --cleanup-on-fail --values -
done
