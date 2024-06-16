#!/bin/bash

## This script must be executed when running the dataspace from IntelliJ. Neglecting to do that will render the connectors
## inoperable!

## Seed application DATA to both connectors
for url in 'http://127.0.0.1:8081' 'http://127.0.0.1:8091'
do
  newman run \
    --folder "Seed" \
    --env-var "HOST=$url" \
    ./deployment/postman/MVD.postman_collection.json > /dev/null
done

## Seed management DATA to identityhubs
API_KEY="c3VwZXItdXNlcg==.c3VwZXItc2VjcmV0LWtleQo="

# add participant alice
PEM_ALICE=$(sed -E ':a;N;$!ba;s/\r{0,1}\n/\\n/g' deployment/assets/alice_public.pem)
DATA_ALICE=$(jq -n --arg pem "$PEM_ALICE" '{
           "roles":[],
           "serviceEndpoints":[
             {
                "type": "CredentialService",
                "serviceEndpoint": "http://localhost:7081/api/resolution/v1/participants/ZGlkOndlYjpsb2NhbGhvc3QlM0E3MDgz",
                "id": "alice-credentialservice-1"
             },
             {
                "type": "ProtocolEndpoint",
                "serviceEndpoint": "http://localhost:8082/api/dsp",
                "id": "alice-dsp"
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

curl -s --location 'http://localhost:7082/api/identity/v1alpha/participants/' \
--header 'Content-Type: application/json' \
--header "x-api-key: $API_KEY" \
--data "$DATA_ALICE"

# add participant bob
PEM_BOB=$(sed -E ':a;N;$!ba;s/\r{0,1}\n/\\n/g' deployment/assets/bob_public.pem)
DATA_BOB=$(jq -n --arg pem "$PEM_BOB" '{
            "roles":[],
            "serviceEndpoints":[
              {
                 "type": "CredentialService",
                 "serviceEndpoint": "http://localhost:7091/api/resolution/v1/participants/ZGlkOndlYjpsb2NhbGhvc3QlM0E3MDkz",
                 "id": "bob-credentialservice-1"
              },
              {
                "type": "ProtocolEndpoint",
                "serviceEndpoint": "http://localhost:8092/api/dsp",
                "id": "bob-dsp"
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

curl -s --location 'http://localhost:7092/api/identity/v1alpha/participants/' \
--header 'Content-Type: application/json' \
--header "x-api-key: $API_KEY" \
--data "$DATA_BOB"