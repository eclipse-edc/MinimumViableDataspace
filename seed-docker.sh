#!/bin/bash

#  Copyright (c) 2024 Metaform Systems, Inc.
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0
#
#  SPDX-License-Identifier: Apache-2.0
#
#  Contributors:
#       Metaform Systems, Inc. - initial API and implementation

# This script seeds the Docker Compose environment.
# It assumes the services are running and ports are mapped as defined in docker-compose.yaml.

set -e

echo "Waiting for services to be ready..."
sleep 10 # Give some time for services to start up fully

# --------------------------------------------------------------------------------------------
# 1. SEED ASSET/POLICY/CONTRACT-DEF DATA (Connectors)
# --------------------------------------------------------------------------------------------
# Provider QnA CP Mgmt: 8181 (Mapped) -> http://localhost:8181/api/management
# Provider Mfg CP Mgmt: 8281 (Mapped) -> http://localhost:8281/api/management
# Note: seed-k8s.sh uses port 80 via ingress. IntelliJ uses direct ports.
# The Postman collection often uses {{HOST}} variable.

echo
echo "Seed data to 'provider-qna' and 'provider-manufacturing'"
# Host URLs for Management API
# Provider QnA CP is mapped to 8181 on host? No, let's check docker-compose.yaml
# provider-qna-controlplane: 8181:8081. Correct.
# provider-manufacturing-controlplane: 8281:8081. Correct.

for url in 'http://localhost:8181/api/management' 'http://localhost:8281/api/management'
do
  newman run \
    --folder "Seed" \
    --env-var "HOST=$url" \
    ./deployment/postman/MVD.postman_collection.json > /dev/null
done

# --------------------------------------------------------------------------------------------
# 2. SEED CATALOG SERVER
# --------------------------------------------------------------------------------------------
# Provider Catalog Server Mgmt: 8091:8081
# DSP URLs must be reachable *from the catalog server container*.
# QnA DSP: http://provider-qna-controlplane:8082/api/dsp
# Mfg DSP: http://provider-manufacturing-controlplane:8082/api/dsp

echo
echo "Create linked assets on the Catalog Server"
newman run \
  --folder "Seed Catalog Server" \
  --env-var "HOST=http://localhost:8091/api/management" \
  --env-var "PROVIDER_QNA_DSP_URL=http://provider-qna-controlplane:8082/api/dsp" \
  --env-var "PROVIDER_MF_DSP_URL=http://provider-manufacturing-controlplane:8082/api/dsp" \
  ./deployment/postman/MVD.postman_collection.json > /dev/null

# --------------------------------------------------------------------------------------------
# 3. SEED IDENTITY HUBS
# --------------------------------------------------------------------------------------------
API_KEY="c3VwZXItdXNlcg==.c3VwZXItc2VjcmV0LWtleQo="

# Consumer IdentityHub Identity API: 7082:7082
# Consumer IdentityHub Credentials API (internal): http://consumer-identityhub:7081...
# Wait, the DID resolution needs to happen.
# In K8s: did:web:consumer-identityhub:7083:consumer
# In docker-compose, the service name 'consumer-identityhub' is resolvable.
# The port 7083 is the DID port.
# DID URL: http://consumer-identityhub:7083/consumer/did.json (approx, depends on resolution)
# The DID string in docker-compose is: did:web:consumer-identityhub%3A7083:consumer

# --- CONSUMER ---
echo
echo "Create consumer participant context in IdentityHub"

# The serviceEndpoint must be reachable by other participants (Providers).
# So it must use the docker network hostname.
CONSUMER_CP_DSP_URL="http://consumer-controlplane:8082/api/dsp"
CONSUMER_IH_URL="http://consumer-identityhub:7081" # Credentials API for internal use?
# Actually, the credential service endpoint is public. Port 7081.
# The DID Document will contain this URL.
# So it must be http://consumer-identityhub:7081/api/credentials/v1/participants/...

DATA_CONSUMER=$(jq -n --arg url "$CONSUMER_CP_DSP_URL" --arg ihurl "$CONSUMER_IH_URL" '{
           "roles":[],
           "serviceEndpoints":[
             {
                "type": "CredentialService",
                "serviceEndpoint": "\($ihurl)/api/credentials/v1/participants/ZGlkOndlYjpjb25zdW1lci1pZGVudGl0eWh1YiUzQTcwODM6Y29uc3VtZXI=",
                "id": "consumer-credentialservice-1"
             },
             {
                "type": "ProtocolEndpoint",
                "serviceEndpoint": "\($url)",
                "id": "consumer-dsp"
             }
           ],
           "active": true,
           "participantId": "did:web:consumer-identityhub%3A7083:consumer",
           "did": "did:web:consumer-identityhub%3A7083:consumer",
           "key":{
               "keyId": "did:web:consumer-identityhub%3A7083:consumer#key-1",
               "privateKeyAlias": "did:web:consumer-identityhub%3A7083:consumer#key-1",
               "keyGeneratorParams":{
                  "algorithm": "EC"
               }
           }
       }')

# Target: Consumer IH Identity API (Host port 7082)
curl -s --location "http://localhost:7082/api/identity/v1alpha/participants/" \
--header 'Content-Type: application/json' \
--header "x-api-key: $API_KEY" \
--data "$DATA_CONSUMER"


# --- PROVIDER ---
echo
echo
echo "Create provider participant context in IdentityHub"

# Provider Catalog Server is the DSP endpoint for the "Provider Corp" identity?
# In K8s: http://provider-catalog-server-controlplane:8082/api/dsp
# Wait, looking at provider.tf: "provider-catalog-server" module.
# In docker-compose: provider-catalog-server:8082/api/dsp
PROVIDER_CP_DSP_URL="http://provider-catalog-server:8082/api/dsp"
PROVIDER_IH_URL="http://provider-identityhub:7081"

DATA_PROVIDER=$(jq -n --arg url "$PROVIDER_CP_DSP_URL" --arg ihurl "$PROVIDER_IH_URL" '{
           "roles":[],
           "serviceEndpoints":[
             {
                "type": "CredentialService",
                "serviceEndpoint": "\($ihurl)/api/credentials/v1/participants/ZGlkOndlYjpwcm92aWRlci1pZGVudGl0eWh1YiUzQTcwODM6cHJvdmlkZXI=",
                "id": "provider-credentialservice-1"
             },
             {
                "type": "ProtocolEndpoint",
                "serviceEndpoint": "\($url)",
                "id": "provider-dsp"
             }
           ],
           "active": true,
           "participantId": "did:web:provider-identityhub%3A7083:provider",
           "did": "did:web:provider-identityhub%3A7083:provider",
           "key":{
               "keyId": "did:web:provider-identityhub%3A7083:provider#key-1",
               "privateKeyAlias": "did:web:provider-identityhub%3A7083:provider#key-1",
               "keyGeneratorParams":{
                  "algorithm": "EC"
               }
           }
       }')

# Target: Provider IH Identity API (Host port 7092)
curl -s --location "http://localhost:7092/api/identity/v1alpha/participants/" \
--header 'Content-Type: application/json' \
--header "x-api-key: $API_KEY" \
--data "$DATA_PROVIDER"


# --------------------------------------------------------------------------------------------
# 4. SEED ISSUER SERVICE
# --------------------------------------------------------------------------------------------
echo
echo
echo "Create dataspace issuer"

# Issuer ID: did:web:dataspace-issuer-service:10016:issuer
# The DID is resolvable via the service itself?
# In docker-compose: dataspace-issuer-service:10016

# Note: We also have NGINX hosting a DID: did:web:dataspace-issuer
# The K8s script seeds:
#   participantId: "did:web:dataspace-issuer-service%3A10016:issuer"
#   serviceEndpoint: "http://dataspace-issuer-service:10012/..."

# But wait, in K8s, the issuer uses `PEM_ISSUER` variable?
# Ah, `seed-k8s.sh` uses `DATA_ISSUER` with `PEM_ISSUER` variable, BUT `seed-k8s.sh` (read previously)
# does NOT use `PEM_ISSUER` variable in the script I read.
# Let's check `seed-k8s.sh` content again.
# It uses `jq -n --arg pem "$PEM_ISSUER"` but `$PEM_ISSUER` is NOT defined in `seed-k8s.sh`.
# It IS defined in `seed.sh`.
# This might be a bug in the existing `seed-k8s.sh` or it relies on the environment.
# However, the key generator params are present: "keyGeneratorParams": { "algorithm": "EdDSA" }.
# So it generates the key.

# Wait, looking at `seed-k8s.sh` provided in context:
# DATA_ISSUER=$(jq -n --arg pem "$PEM_ISSUER" ...
# It seems $PEM_ISSUER is undefined there too.
# But the JSON uses "keyGeneratorParams", so "publicKeyPem" is NOT used?
# Actually, if `keyGeneratorParams` is present, `publicKeyPem` is likely ignored or optional.
# I will assume generation.

DATA_ISSUER=$(jq -n '{
            "roles":["admin"],
            "serviceEndpoints":[
              {
                 "type": "IssuerService",
                 "serviceEndpoint": "http://dataspace-issuer-service:10012/api/issuance/v1alpha/participants/ZGlkOndlYjpkYXRhc3BhY2UtaXNzdWVyLXNlcnZpY2UlM0ExMDAxNjppc3N1ZXI=",
                 "id": "issuer-service-1"
              }
            ],
            "active": true,
            "participantId": "did:web:dataspace-issuer-service%3A10016:issuer",
            "did": "did:web:dataspace-issuer-service%3A10016:issuer",
            "key":{
                "keyId": "did:web:dataspace-issuer-service%3A10016:issuer#key-1",
                "privateKeyAlias": "key-1",
                "keyGeneratorParams":{
                  "algorithm": "EdDSA"
                }
            }
      }')

# Target: Issuer Identity API (Host port 10015)
curl -s --location 'http://localhost:10015/api/identity/v1alpha/participants/' \
--header 'Content-Type: application/json' \
--data "$DATA_ISSUER"

## Seed participant data to the issuer service
# Issuer Admin URL: http://localhost:10013 (mapped)
echo
echo "Seed participant data to the issuer service"
newman run \
  --folder "Seed Issuer SQL" \
  --env-var "ISSUER_ADMIN_URL=http://localhost:10013/api/admin" \
  --env-var "CONSUMER_ID=did:web:consumer-identityhub%3A7083:consumer" \
  --env-var "CONSUMER_NAME=MVD Consumer Participant" \
  --env-var "PROVIDER_ID=did:web:provider-identityhub%3A7083:provider" \
  --env-var "PROVIDER_NAME=MVD Provider Participant" \
  ./deployment/postman/MVD.postman_collection.json > /dev/null

echo "Seeding complete."
