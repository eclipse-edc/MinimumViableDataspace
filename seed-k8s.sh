#!/bin/bash

## Typically, you do not need to execute this script, because with Kubernetes, seeding is done with a Kubernetes Job.
## However, if you want to re-seed data (e.g. after a pod crashed), you can use this script.

## Seed application DATA to both connectors
echo
echo
echo "Seed data to Ted and Carol"
for url in 'http://127.0.0.1/ted/cp' 'http://127.0.0.1/carol/cp'
do
  newman run \
    --folder "Seed" \
    --env-var "HOST=$url" \
    ./deployment/postman/MVD.postman_collection.json > /dev/null
done

## Seed linked assets to Catalog Server "Bob"
echo
echo
echo "Create linked assets on the Catalog Server"
newman run \
  --folder "Seed Catalog Server" \
  --env-var "HOST=http://127.0.0.1/provider-catalog-server/cp" \
  --env-var "TED_DSP_URL=http://ted-controlplane:8082" \
  --env-var "CAROL_DSP_URL=http://carol-controlplane:8082" \
  ./deployment/postman/MVD.postman_collection.json > /dev/null

## Seed management DATA to identityhubsl
API_KEY="c3VwZXItdXNlcg==.c3VwZXItc2VjcmV0LWtleQo="

# add consumer participant
echo
echo
echo "Create consumer participant"
CONSUMER_CONTROLPLANE_SERVICE_URL="http://alice-controlplane:8082"
CONSUMER_IDENTITYHUB_URL="http://alice-identityhub:7082"
DATA_CONSUMER=$(jq -n --arg url "$CONSUMER_CONTROLPLANE_SERVICE_URL" --arg ihurl "$CONSUMER_IDENTITYHUB_URL" '{
           "roles":[],
           "serviceEndpoints":[
             {
                "type": "CredentialService",
                "serviceEndpoint": "\($ihurl)/api/resolution/v1/participants/ZGlkOndlYjphbGljZS1pZGVudGl0eWh1YiUzQTcwODM6YWxpY2U",
                "id": "alice-credentialservice-1"
             },
             {
                "type": "ProtocolEndpoint",
                "serviceEndpoint": "\($url)/api/dsp",
                "id": "alice-dsp"
             }
           ],
           "active": true,
           "participantId": "did:web:alice-identityhub%3A7083:alice",
           "did": "did:web:alice-identityhub%3A7083:alice",
           "key":{
               "keyId": "did:web:alice-identityhub%3A7083:alice#key-1",
               "privateKeyAlias": "key-1",
               "keyGeneratorParams":{
                  "algorithm": "EC"
               }
           }
       }')

curl -s --location "http://127.0.0.1/alice/cs/api/identity/v1alpha/participants/" \
--header 'Content-Type: application/json' \
--header "x-api-key: $API_KEY" \
--data "$DATA_CONSUMER"


# add provider participant

echo
echo
echo "Create provider participant"

PROVIDER_CONTROLPLANE_SERVICE_URL="http://provider-catalog-server-controlplane:8082"
PROVIDER_IDENTITYHUB_URL="http://bob-identityhub:7082"

DATA_PROVIDER=$(jq -n --arg url "$PROVIDER_CONTROLPLANE_SERVICE_URL" --arg ihurl "$PROVIDER_IDENTITYHUB_URL" '{
           "roles":[],
           "serviceEndpoints":[
             {
                "type": "CredentialService",
                "serviceEndpoint": "\($ihurl)/api/resolution/v1/participants/ZGlkOndlYjpib2ItaWRlbnRpdHlodWIlM0E3MDgzOmJvYg",
                "id": "ted-credentialservice-1"
             },
             {
                "type": "ProtocolEndpoint",
                "serviceEndpoint": "\($url)/api/dsp",
                "id": "ted-dsp"
             }
           ],
           "active": true,
           "participantId": "did:web:bob-identityhub%3A7083:bob",
           "did": "did:web:bob-identityhub%3A7083:bob",
           "key":{
               "keyId": "did:web:bob-identityhub%3A7083:bob#key-1",
               "privateKeyAlias": "key-1",
               "keyGeneratorParams":{
                  "algorithm": "EC"
               }
           }
       }')

curl -s --location "http://127.0.0.1/bob/cs/api/identity/v1alpha/participants/" \
--header 'Content-Type: application/json' \
--header "x-api-key: $API_KEY" \
--data "$DATA_PROVIDER"