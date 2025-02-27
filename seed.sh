#!/bin/bash

#
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
#
#

## This script must be executed when running the dataspace from IntelliJ. Neglecting to do that will render the connectors
## inoperable!

## Seed asset/policy/contract-def data to both "provider-qna" and "provider-manufacturing"
for url in 'http://127.0.0.1:8191' 'http://127.0.0.1:8291'
do
  newman run \
    --folder "Seed" \
    --env-var "HOST=$url" \
    ./deployment/postman/MVD.postman_collection.json > /dev/null
done

## Seed linked assets to Catalog Server
newman run \
  --folder "Seed Catalog Server" \
  --env-var "HOST=http://127.0.0.1:8091" \
  --env-var "PROVIDER_QNA_DSP_URL=http://localhost:8192" \
  --env-var "PROVIDER_MF_DSP_URL=http://localhost:8292" \
  ./deployment/postman/MVD.postman_collection.json > /dev/null


## Seed identity data to identityhubs
API_KEY="c3VwZXItdXNlcg==.c3VwZXItc2VjcmV0LWtleQo="

# add participant "consumer"
echo
echo
echo "Create consumer participant context in IdentityHub"
PEM_CONSUMER=$(sed -E ':a;N;$!ba;s/\r{0,1}\n/\\n/g' deployment/assets/consumer_public.pem)
DATA_CONSUMER=$(jq -n --arg pem "$PEM_CONSUMER" '{
           "roles":[],
           "serviceEndpoints":[
             {
                "type": "CredentialService",
                "serviceEndpoint": "http://localhost:7081/api/credentials/v1/participants/ZGlkOndlYjpsb2NhbGhvc3QlM0E3MDgz",
                "id": "consumer-credentialservice-1"
             },
             {
                "type": "ProtocolEndpoint",
                "serviceEndpoint": "http://localhost:8082/api/dsp",
                "id": "consumer-dsp"
             }
           ],
           "active": true,
           "participantId": "did:web:localhost%3A7083",
           "did": "did:web:localhost%3A7083",
           "key":{
               "keyId": "did:web:localhost%3A7083#key-1",
               "privateKeyAlias": "key-1",
               "publicKeyPem":"\($pem)"
           }
       }')

# the consumer runtime will need to have the client_secret in its vault as well, so we store it in a variable
# and use the Secrets API (part of Management API) to insert it.
clientSecret=$(curl -s --location 'http://localhost:7082/api/identity/v1alpha/participants/' \
--header 'Content-Type: application/json' \
--header "x-api-key: $API_KEY" \
--data "$DATA_CONSUMER" | jq -r '.clientSecret')

# add client secret to the consumer runtime
SECRETS_DATA=$(jq -n --arg secret "$clientSecret" \
'{
  "@context" : {
    "edc" : "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type" : "https://w3id.org/edc/v0.0.1/ns/Secret",
  "@id" : "did:web:localhost%3A7083-sts-client-secret",
  "https://w3id.org/edc/v0.0.1/ns/value": "\($secret)"
}')

curl -sL -X POST http://localhost:8081/api/management/v3/secrets -H "x-api-key: password" -H "Content-Type: application/json" -d "$SECRETS_DATA"

# add participant "provider"
echo
echo
echo "Create provider participant context in IdentityHub"
PEM_PROVIDER=$(sed -E ':a;N;$!ba;s/\r{0,1}\n/\\n/g' deployment/assets/provider_public.pem)
DATA_PROVIDER=$(jq -n --arg pem "$PEM_PROVIDER" '{
            "roles":[],
            "serviceEndpoints":[
              {
                 "type": "CredentialService",
                 "serviceEndpoint": "http://localhost:7091/api/credentials/v1/participants/ZGlkOndlYjpsb2NhbGhvc3QlM0E3MDkz",
                 "id": "provider-credentialservice-1"
              },
              {
                "type": "ProtocolEndpoint",
                "serviceEndpoint": "http://localhost:8092/api/dsp",
                "id": "provider-catalogserver-dsp"
              }
            ],
            "active": true,
            "participantId": "did:web:localhost%3A7093",
            "did": "did:web:localhost%3A7093",
            "key":{
                "keyId": "did:web:localhost%3A7093#key-1",
                "privateKeyAlias": "key-1",
                "publicKeyPem":"\($pem)"
            }
      }')

# the provider runtime will need to have the client_secret in its vault as well, so we store it in a variable
# and use the Secrets API (part of Management API) to insert it.
clientSecret=$(curl -s --location 'http://localhost:7092/api/identity/v1alpha/participants/' \
--header 'Content-Type: application/json' \
--header "x-api-key: $API_KEY" \
--data "$DATA_PROVIDER" | jq -r '.clientSecret')

# add client secret to the provider runtimes
SECRETS_DATA=$(jq -n --arg secret "$clientSecret" \
'{
  "@context" : {
    "edc" : "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type" : "https://w3id.org/edc/v0.0.1/ns/Secret",
  "@id" : "did:web:localhost%3A7093-sts-client-secret",
  "https://w3id.org/edc/v0.0.1/ns/value": "\($secret)"
}')

curl -sL -X POST http://localhost:8091/api/management/v3/secrets -H "x-api-key: password" -H "Content-Type: application/json" -d "$SECRETS_DATA"
curl -sL -X POST http://localhost:8191/api/management/v3/secrets -H "x-api-key: password" -H "Content-Type: application/json" -d "$SECRETS_DATA"
curl -sL -X POST http://localhost:8291/api/management/v3/secrets -H "x-api-key: password" -H "Content-Type: application/json" -d "$SECRETS_DATA"

###############################################
# SEED ISSUER SERVICE
###############################################

echo
echo
echo "Create dataspace issuer"
PEM_ISSUER=$(sed -E ':a;N;$!ba;s/\r{0,1}\n/\\n/g' deployment/assets/issuer_public.pem)
DATA_ISSUER=$(jq -n --arg pem "$PEM_ISSUER" '{
            "roles":["admin"],
            "serviceEndpoints":[
              {
                 "type": "IssuerService",
                 "serviceEndpoint": "http://localhost:10012/api/issuance/v1alpha/participants/ZGlkOndlYjpsb2NhbGhvc3QlM0ExMDEwMA==",
                 "id": "issuer-service-1"
              }
            ],
            "active": true,
            "participantId": "did:web:localhost%3A10100",
            "did": "did:web:localhost%3A10100",
            "key":{
                "keyId": "did:web:localhost%3A10100#key-1",
                "privateKeyAlias": "key-1",
                "keyGeneratorParams":{
                  "algorithm": "EdDSA"
                }
            }
      }')

curl -s --location 'http://localhost:10015/api/identity/v1alpha/participants/' \
--header 'Content-Type: application/json' \
--data "$DATA_ISSUER"

## Seed participant data to the issuer service
newman run \
  --folder "Seed Issuer" \
  --env-var "ISSUER_ADMIN_URL=http://localhost:10013" \
  --env-var "CONSUMER_ID=did:web:localhost%3A7083" \
  --env-var "CONSUMER_NAME=MVD Consumer Participant" \
  --env-var "PROVIDER_ID=did:web:localhost%3A7093" \
  --env-var "PROVIDER_NAME=MVD Provider Participant" \
  ./deployment/postman/MVD.postman_collection.json