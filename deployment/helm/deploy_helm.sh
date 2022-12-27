#!/bin/bash

# the "x" will log every command to the shell - good for debugging purposes
set -euxo pipefail

echo "Deploy registration service"
envsubst <registration-service/values.template.yaml | helm upgrade --install registration-service registration-service --timeout 20s --cleanup-on-fail --values -

for participant in "company1" "company2" "company3"; do
  envsubst <connector/values.template.yaml >connector/values.yaml
  envsubst <catalog/values.template.yaml >catalog/values.yaml
  envsubst <dashboard/values.template.yaml >dashboard/values.yaml
  helm dependency update connector
  helm upgrade --install $participant connector --timeout 60s --cleanup-on-fail --wait
done
