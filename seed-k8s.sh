#!/bin/bash

## Typically, you do not need to execute this script, because with Kubernetes, seeding is done with a Kubernetes Job.
## However, if you want to re-seed data (e.g. after a pod crashed), you can use this script.

## Seed application DATA to both connectors
for url in 'http://127.0.0.1/alice/cp' 'http://127.0.0.1/bob/cp'
do
  newman run \
    --folder "Seed" \
    --env-var "HOST=$url" \
    ./deployment/postman/MVD.postman_collection.json > /dev/null
done

## Seed management DATA to identityhubs
API_KEY="c3VwZXItdXNlcg==.c3VwZXItc2VjcmV0LWtleQo="

# add participant alice
echo
echo "Create participant Bob"
echo
PEM_ALICE=$(sed -E ':a;N;$!ba;s/\r{0,1}\n/\\n/g' deployment/assets/alice_public.pem)
ALICE_CP_URL="http://alice-controlplane"
ALICE_IH_URL="http://127.0.0.1/alice/cs"
DATA_ALICE=$(jq -n --arg pem "$PEM_ALICE" --arg url "$ALICE_CP_URL" '{
           "roles":[],
           "serviceEndpoints":[
             {
                "type": "CredentialService",
                "serviceEndpoint": "\($url)/api/resolution/v1/participants/ZGlkOndlYjpsb2NhbGhvc3QlM0E3MDgz",
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
               "publicKeyPem":"\($pem)"
           }
       }')

curl -s --location "$ALICE_IH_URL/api/identity/v1alpha/participants/" \
--header 'Content-Type: application/json' \
--header "x-api-key: $API_KEY" \
--data "$DATA_ALICE"


# add participant bob
echo
echo "Create participant Bob"
echo

PEM_BOB=$(sed -E ':a;N;$!ba;s/\r{0,1}\n/\\n/g' deployment/assets/bob_public.pem)
BOB_CP_URL="http://bob-controlplane"
BOB_IH_URL="http://127.0.0.1/bob/cs"
DATA_BOB=$(jq -n --arg pem "$PEM_BOB" --arg url "$BOB_CP_URL" '{
           "roles":[],
           "serviceEndpoints":[
             {
                "type": "CredentialService",
                "serviceEndpoint": "\($url)/api/resolution/v1/participants/ZGlkOndlYjpsb2NhbGhvc3QlM0E3MDgz",
                "id": "bob-credentialservice-1"
             },
             {
                "type": "ProtocolEndpoint",
                "serviceEndpoint": "\($url)/api/dsp",
                "id": "bob-dsp"
             }
           ],
           "active": true,
           "participantId": "did:web:bob-identityhub%3A7083:bob",
           "did": "did:web:bob-identityhub%3A7083:bob",
           "key":{
               "keyId": "did:web:bob-identityhub%3A7083:bob#key-1",
               "privateKeyAlias": "key-1",
               "publicKeyPem":"\($pem)"
           }
       }')

curl -s --location "$BOB_IH_URL/api/identity/v1alpha/participants/" \
--header 'Content-Type: application/json' \
--header "x-api-key: $API_KEY" \
--data "$DATA_BOB"