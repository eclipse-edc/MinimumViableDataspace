#!/bin/bash

## Typically, you do not need to execute this script, because with Kubernetes, seeding is done with a Kubernetes Job.
## However, if you want to re-seed data (e.g. after a pod crashed), you can use this script.

## Seed application DATA to both connectors
for url in 'http://127.0.0.1/ted/cp' 'http://127.0.0.1/carol/cp'
do
  newman run \
    --folder "Seed" \
    --env-var "HOST=$url" \
    ./deployment/postman/MVD.postman_collection.json > /dev/null
done

## Seed linked assets to Catalog Server "Bob"
newman run \
  --folder "Seed Catalog Server" \
  --env-var "HOST=http://127.0.0.1/provider-catalog-server/cp" \
  ./deployment/postman/MVD.postman_collection.json > /dev/null

## Seed management DATA to identityhubs
API_KEY="c3VwZXItdXNlcg==.c3VwZXItc2VjcmV0LWtleQo="

# add consumer participant
echo
echo "Create consumer participant"
echo
PEM_CONSUMER=$(sed -E ':a;N;$!ba;s/\r{0,1}\n/\\n/g' deployment/assets/alice_public.pem)
CONSUMER_CONTROLPLANE_SERVICE_URL="http://alice-controlplane:8082"
CONSUMER_IDENTITYHUB_URL="http://127.0.0.1/alice/cs"
DATA_CONSUMER=$(jq -n --arg pem "$PEM_CONSUMER" --arg url "$CONSUMER_CONTROLPLANE_SERVICE_URL" --arg ihurl "$CONSUMER_IDENTITYHUB_URL" '{
           "roles":[],
           "serviceEndpoints":[
             {
                "type": "CredentialService",
                "serviceEndpoint": "\($ihurl)/api/resolution/v1/participants/ZGlkOndlYjpsb2NhbGhvc3QlM0E3MDgz",
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

curl -s --location "$CONSUMER_IDENTITYHUB_URL/api/identity/v1alpha/participants/" \
--header 'Content-Type: application/json' \
--header "x-api-key: $API_KEY" \
--data "$DATA_CONSUMER"


# add provider participant

echo
echo "Create provider participant"
echo

PEM_PROVIDER=$(sed -E ':a;N;$!ba;s/\r{0,1}\n/\\n/g' deployment/assets/bob_public.pem)
PROVIDER_CONTROLPLANE_SERVICE_URL="http://provider-catalog-server-controlplane:8082"
PROVIDER_IDENTITYHUB_URL="http://127.0.0.1/bob/cs"

DATA_PROVIDER=$(jq -n --arg pem "$PEM_PROVIDER" --arg url "$PROVIDER_CONTROLPLANE_SERVICE_URL" --arg ihurl "$PROVIDER_IDENTITYHUB_URL" '{
           "roles":[],
           "serviceEndpoints":[
             {
                "type": "CredentialService",
                "serviceEndpoint": "\($ihurl)/api/resolution/v1/participants/ZGlkOndlYjpsb2NhbGhvc3QlM0E3MDgz",
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

curl -s --location "$PROVIDER_IDENTITYHUB_URL/api/identity/v1alpha/participants/" \
--header 'Content-Type: application/json' \
--header "x-api-key: $API_KEY" \
--data "$DATA_PROVIDER"