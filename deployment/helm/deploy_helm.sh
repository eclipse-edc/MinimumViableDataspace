#!/bin/bash

# the "x" will log every command to the shell - good for debugging purposes
set -euxo pipefail

echo "Deploy registration service"
envsubst <registration-service/values.template.yaml | helm upgrade --install registration-service registration-service --timeout 20s --cleanup-on-fail --values -

for PARTICIPANT in "company1" "company2" "company3"; do
  envsubst <connector/values.template.yaml | helm upgrade --install $PARTICIPANT connector --timeout 20s --cleanup-on-fail --values -
done
